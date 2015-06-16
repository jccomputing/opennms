/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Field;
import org.opennms.features.vaadin.jmxconfiggenerator.Config;
import org.opennms.features.vaadin.jmxconfiggenerator.data.JmxCollectionCloner;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectableBeanItemContainer;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectionChangedListener;
import org.opennms.features.vaadin.jmxconfiggenerator.data.StringRenderer;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.AttributeNameValidator;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.NameValidator;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.UniqueAttributeNameValidator;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Attrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompAttrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompMember;
import org.opennms.xmlns.xsd.config.jmx_datacollection.JmxDatacollectionConfig;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Mbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls the "MbeansView".
 * 
 * @author Markus von Rüden
 */
public class MBeansController implements NameProvider {

	/**
	 * Helper class to save information about selection.
	 */
	private static class Selection {
		private Item item;
		private Object itemId;

		public Object getItemId() {
			return itemId;
		}

		public Item getItem() {
			return item;
		}

		public void update(Object itemId, Item item) {
			this.itemId = itemId;
			this.item = item;
		}
	}

	/**
	 * Vaadin container for the items to show up in the tree.
	 */
	private final MbeansHierarchicalContainer mbeansContainer = new MbeansHierarchicalContainer(this);

	private final MBeansItemStrategyHandler itemStrategyHandler = new MBeansItemStrategyHandler();

	private Selection currentSelection = new Selection();

	private List<SelectionChangedListener> selectionChangedListener = new ArrayList<>();

	private AttributesContainerCache<Attrib, Mbean> attribContainerCache = new AttributesContainerCache<Attrib, Mbean>(
			Attrib.class, new AttributesContainerCache.AttributeCollector<Attrib, Mbean>() {
				@Override
				public List<Attrib> getAttributes(Mbean outer) {
					return outer.getAttrib();
				}
			});

	// TODO MVR -> this is not correct, because we do not want all members,
	// we just want specific ones
	private AttributesContainerCache<CompAttrib, Mbean> compAttribContainerCache = new AttributesContainerCache<CompAttrib, Mbean>(
			CompAttrib.class, new AttributesContainerCache.AttributeCollector<CompAttrib, Mbean>() {
				@Override
				public List<CompAttrib> getAttributes(Mbean outer) {
					return outer.getCompAttrib();
				}
			});

	private AttributesContainerCache<CompMember, CompAttrib> compMemberContainerCache = new AttributesContainerCache<CompMember, CompAttrib>(
			CompMember.class, new AttributesContainerCache.AttributeCollector<CompMember, CompAttrib>() {
				@Override
				public List<CompMember> getAttributes(CompAttrib outer) {
					return outer.getCompMember();
				}
			});

	private MBeansContentPanel mbeansContentPanel;

	private MBeansTree mbeansTree;

	public void registerSelectionChangedListener(SelectionChangedListener listener) {
		if (!selectionChangedListener.contains(listener))
			selectionChangedListener.add(listener);
	}

	private void notifyObservers(Selection currentSelection) {
		// TODO MVR ...
		SelectionChangedListener.SelectionChangedEvent changeEvent = new SelectionChangedListener.SelectionChangedEvent(currentSelection.getItem(), currentSelection.getItemId());
		for (SelectionChangedListener eachListener : selectionChangedListener)
			eachListener.selectionChanged(changeEvent);
	}

	public MbeansHierarchicalContainer getMBeansHierarchicalContainer() {
		return mbeansContainer;
	}

	public void setMbeansTree(MBeansTree mbeansTree) {
		this.mbeansTree = mbeansTree;
	}

	public void setMbeansContentPanel(MBeansContentPanel mbeansContentPanel) {
		this.mbeansContentPanel = mbeansContentPanel;
	}

	/**
	 * Updates the view when the selected MBean changes. At first each
	 * SelectionChangedListener are told, that there is a new Mbean to take care of
	 * (in detail: change the view to list mbean details of new mbean). And of
	 * course set a new ViewState (e.g. a non Mbean was selected and now a Mbean
	 * is selected)
	 *
	 * @param itemId the ItemId (Object Id) to select in the tree.
	 */
	protected void selectItemInTree(Object itemId) {
		if (currentSelection.getItemId() != itemId) {
			boolean validated = false;
			try {
				mbeansContentPanel.validate();
				validated = true;
				updateValidState(currentSelection.getItemId(), true);
				mbeansContentPanel.commit();
				currentSelection.update(itemId, mbeansContainer.getItem(itemId));
				notifyObservers(currentSelection);
				mbeansTree.select(itemId);
			} catch (Validator.InvalidValueException ex) {
				updateValidState(currentSelection.getItemId(), false);
				if (!validated)  {
					UIHelper.getCurrent().access(new Runnable() {
						@Override
						public void run() {
							mbeansTree.select(currentSelection.getItemId()); // revert Selection
						}
					});
					UIHelper.showValidationError("Cannot change selection. The current view contains errors.");
				}
			}
		}
	}
	private void updateValidState(Object itemId, boolean valid) {
		Item theItem = mbeansContainer.getItem(itemId);
		if (theItem != null) {
			theItem.getItemProperty("valid").setValue(Boolean.valueOf(valid)); // set the new validity
		}
	}

	public void setItemProperties(Item item, Object itemId) {
		itemStrategyHandler.setItemProperties(item, itemId);
	}

	public StringRenderer getStringRenderer(Class<?> clazz) {
		return itemStrategyHandler.getStringRenderer(clazz);
	}


	void handleDeselect(HierarchicalContainer container, Object itemId) {
		handleSelectDeselect(container, container.getItem(itemId), itemId, false);
	}

	void handleSelect(HierarchicalContainer container, Object itemId) {
		handleSelectDeselect(container, container.getItem(itemId), itemId, true);
	}

	public void handleSelectDeselect(HierarchicalContainer container, Item item, Object itemId, boolean select) {
		final MBeansItemStrategyHandler.ItemStrategy itemStrategy = itemStrategyHandler.getStrategy(itemId.getClass());
		itemStrategy.handleSelectDeselect(item, itemId, select);
		itemStrategy.updateIcon(item);
		if (!container.hasChildren(itemId)) return;
		for (Object childItemId : container.getChildren(itemId)) {
			handleSelectDeselect(container, container.getItem(childItemId), childItemId, select);
		}
	}

//	public void updateMBeanIcon() {
//		itemStrategyHandler.getStrategy(Mbean.class).updateIcon(mbeansContainer.getItem(currentlySelected));
//	}

	public SelectableBeanItemContainer<Attrib> getAttributeContainer(Mbean bean) {
		return attribContainerCache.getContainer(bean);
	}

	private void reset()  {
		currentSelection.update(null, null);
		attribContainerCache.clear();
		compAttribContainerCache.clear();
		compMemberContainerCache.clear();
	}

//	public void clearAttributesCache() {
//		attribContainerCache.containerMap.clear();
//	}

//	protected void updateMBean() {
//		itemStrategyHandler.getStrategy(Mbean.class).updateModel(mbeansContainer.getItem(currentlySelected),
//				currentlySelected);
//	}

	public SelectableBeanItemContainer<CompMember> getCompositeMemberContainer(CompAttrib attrib) {
		return compMemberContainerCache.getContainer(attrib);
	}

	public SelectableBeanItemContainer<CompAttrib> getCompositeAttributeContainer(Mbean mbean) {
		return compAttribContainerCache.getContainer(mbean);
	}

	@Override
	public Map<Object, String> getNames() {
		Map<Object, String> names = new HashMap<Object, String>();
		for (Mbean bean : getSelectedMbeans()) {
			SelectableBeanItemContainer<Attrib> attributeContainer = getAttributeContainer(bean);
			for (Attrib att : bean.getAttrib()) {
				if (attributeContainer.getItem(att).isSelected()) {
					names.put(att, att.getAlias());
				}
			}
			for (CompAttrib compAttrib : bean.getCompAttrib()) {
				SelectableBeanItemContainer<CompMember> compMemberContainer = getCompositeMemberContainer(compAttrib);
				for (CompMember compMember : compAttrib.getCompMember()) {
					if (compMemberContainer.getItem(compMember).isSelected()) {
						names.put(compMember, compMember.getAlias());
					}
				}
			}
		}
		return names;
	}

//	protected Mbean getSelectedMBean() {
//		return currentlySelected;
//	}

	/**
	 * The whole point was to select/deselect
	 * Mbeans/Attribs/CompMembers/CompAttribs. In this method we simply create a
	 * JmxDatacollectionConfig considering the choices we made in the gui. To do
	 * this, we simply clone the original <code>JmxDatacollectionConfig</code>
	 * loaded at the beginning. After that we remove all
	 * MBeans/Attribs/CompMembers/CompAttribs and add them manually with the
	 * changes made in the gui.
	 * 
	 * @param uiModel
	 *
	 * @return
	 */
	// TODO mvonrued -> I guess we do not need this clone-stuff at all ^^ and it
	// is too complicated for such a simple
	// task
	public JmxDatacollectionConfig createJmxDataCollectionAccordingToSelection(UiModel uiModel) {
		/*
		 * At First we clone the original collection. This is done, because if
		 * we make any modifications (e.g. deleting not selected elements) the
		 * data isn't available in the GUI, too. To avoid reloading the data
		 * from server, we just clone it.
		 */
		JmxDatacollectionConfig clone = JmxCollectionCloner.clone(uiModel.getRawModel());

		/*
		 * At second we remove all MBeans from original data and get only
		 * selected once.
		 */
		List<Mbean> exportBeans = clone.getJmxCollection().get(0).getMbeans().getMbean();
		exportBeans.clear();
		Iterable<Mbean> selectedMbeans = getSelectedMbeans(getMBeansHierarchicalContainer());
		for (Mbean mbean : selectedMbeans) {
			/*
			 * We remove all Attributes from Mbean, because we only want
			 * selected ones.
			 */
			Mbean exportBean = JmxCollectionCloner.clone(mbean);
			exportBean.getAttrib().clear(); // we only want selected ones :)
			for (Attrib att : getSelectedAttributes(mbean, getAttributeContainer(mbean))) {
				exportBean.getAttrib().add(JmxCollectionCloner.clone(att));
			}
			if (!exportBean.getAttrib().isEmpty()) {
				exportBeans.add(exportBean); // no attributes selected, don't
												// add bean
			}
			/*
			 * We remove all CompAttribs and CompMembers from MBean,
			 * because we only want selected ones.
			 */
			exportBean.getCompAttrib().clear();
			for (CompAttrib compAtt : getSelectedCompositeAttributes(mbean, getCompositeAttributeContainer(mbean))) {
				CompAttrib cloneCompAtt = JmxCollectionCloner.clone(compAtt);
				cloneCompAtt.getCompMember().clear();
				for (CompMember compMember : getSelectedCompositeMembers(compAtt, getCompositeMemberContainer(compAtt))) {
					cloneCompAtt.getCompMember().add(JmxCollectionCloner.clone(compMember));
				}
				if (!cloneCompAtt.getCompMember().isEmpty()) {
					exportBean.getCompAttrib().add(cloneCompAtt);
				}
			}
		}
		// Last but not least, we need to update the service name
		clone.getJmxCollection().get(0).setName(uiModel.getServiceName());
		return clone;
	}

	/**
	 * Returns all mbeans which are selected.
	 * 
	 * @return all mbeans which are selected.
	 */
	protected Iterable<Mbean> getSelectedMbeans() {
		return getSelectedMbeans(mbeansContainer);
	}

	/**
	 * Returns all selected Attributes for the given mbean. The mbean should be
	 * also selected. There is no check if that is the case.
	 * 
	 * @param mbean
	 *            The mbean to get all selected attributes from. The mbean
	 *            should be also selected. There is no check if that is the
	 *            case.
	 * @return all selected attributes for the given mbean.
	 */
	protected Iterable<Attrib> getSelectedAttributes(Mbean mbean) {
		return getSelectedAttributes(mbean, getAttributeContainer(mbean));
	}

	/**
	 * Returns all selected composite attributes for the given mbean. The mbean
	 * should be also selected. There is no check if that is the case.
	 * 
	 * @param mbean
	 *            The mbean to get all selected composite attributes from. The
	 *            mbean should be also selected. There is no check if that is
	 *            the case.
	 * @return all selected attributes for the given mbean.
	 */
	protected Iterable<CompAttrib> getSelectedCompositeAttributes(Mbean mbean) {
		return getSelectedCompositeAttributes(mbean, getCompositeAttributeContainer(mbean));
	}

	/**
	 * Returns all selected composite members for the given composite attribute.
	 * The composite attribute should be also selected. There is no check if
	 * that is the case.
	 * 
	 * @param compAttrib
	 *            The composite attribute to get all selected composite members
	 *            from. The composite attribute should be also selected. There
	 *            is no check if that is the case.
	 * @return all selected composite members for the given composite attribute.
	 */
	protected Iterable<CompMember> getSelectedCompositeMembers(CompAttrib compAttrib) {
		return getSelectedCompositeMembers(compAttrib, getCompositeMemberContainer(compAttrib));
	}

	public void updateDataSource(UiModel newModel) {
		reset();
		mbeansContainer.updateDataSource(newModel);
		mbeansContentPanel.reset();
		Object firstItemId = mbeansTree.expandAllItems();
		mbeansTree.select(firstItemId);
		isValid();
	}

	// TODO the whole validation is made twice :-/
	// TODO we can fix that when there is a central "ValidationStrategy"-Handler instance or so
	public boolean isValid() {
		List<Validator.InvalidValueException> exceptionList = new ArrayList<Validator.InvalidValueException>();
		NameValidator nameValidator = new NameValidator();

		Validator attributeNameValidator = new AttributeNameValidator();
		Validator attributeLengthValidator = new StringLengthValidator(String.format("Maximal length is %d", Config.ATTRIBUTES_ALIAS_MAX_LENGTH), 0, Config.ATTRIBUTES_ALIAS_MAX_LENGTH, false);  // TODO do it more dynamically
		UniqueAttributeNameValidator attributeUniqueNameValidator = new UniqueAttributeNameValidator(this, new HashMap<Object, Field<String>>());


		// 1. validate each MBean (Mbean name without required check!)
		for (Mbean eachMBean : getSelectedMbeans()) {
			validate(nameValidator, eachMBean, eachMBean.getName(), exceptionList); // TODO do it more dynamically

			// 2. validate each CompositeAttribute
			for (CompAttrib eachCompositeAttribute : getSelectedCompositeAttributes(eachMBean)) {
				validate(nameValidator, eachCompositeAttribute, eachCompositeAttribute.getName(), exceptionList); // TODO do it more dynamically

				for (org.opennms.xmlns.xsd.config.jmx_datacollection.CompMember eachCompMember : getSelectedCompositeMembers(eachCompositeAttribute)) {
					validate(attributeNameValidator, eachCompMember, eachCompMember.getAlias(), exceptionList); // TODO do it more dynamically
					validate(attributeLengthValidator, eachCompMember, eachCompMember.getAlias(), exceptionList); // TODO do it more dynamically
					validate(attributeUniqueNameValidator, eachCompMember, eachCompMember.getAlias(), exceptionList); // TODO do it more dynamically
				}
			}

			// 3. validate each Attribute
			for (Attrib eachAttribute : getSelectedAttributes(eachMBean)) {
				validate(attributeNameValidator, eachAttribute, eachAttribute.getAlias(), exceptionList); // TODO do it more dynamically
				validate(attributeLengthValidator, eachAttribute, eachAttribute.getAlias(), exceptionList); // TODO do it more dynamically
				validate(attributeUniqueNameValidator, eachAttribute, eachAttribute.getAlias(), exceptionList); // TODO do it more dynamically
			}
		}
		return exceptionList.isEmpty();
	}

	private void validate(Validator validator, Object itemId, Object value, List<Validator.InvalidValueException> exceptionList) {
		try {
			validator.validate(value); // TODO do it more dynamically
		} catch (Validator.InvalidValueException ex) {
			updateValidState(itemId, false);
			exceptionList.add(ex);
		}
	}

	/**
	 * @param container
	 * @return all Mbeans which are selected
	 */
	private static Iterable<Mbean> getSelectedMbeans(final MbeansHierarchicalContainer container) {
		return Iterables.filter(container.getMBeans(), new Predicate<Mbean>() {
			@Override
			public boolean apply(final Mbean bean) {
				Item item = container.getItem(bean);
				Property itemProperty = item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED);
				if (itemProperty != null && itemProperty.getValue() != null) {
					return (Boolean) itemProperty.getValue();
				}
				return false;
			}
		});
	}

	/**
	 * 
	 * @param mbean
	 * @param compAttribContainer
	 * @return all CompAttrib elements which are selected
	 */
	private static Iterable<CompAttrib> getSelectedCompositeAttributes(final Mbean mbean,
			final SelectableBeanItemContainer<CompAttrib> compAttribContainer) {
		if (AttributesContainerCache.NULL == compAttribContainer) {
			return mbean.getCompAttrib();
		}
		return Iterables.filter(mbean.getCompAttrib(), new Predicate<CompAttrib>() {
			@Override
			public boolean apply(CompAttrib compAtt) {
				return compAttribContainer.getItem(compAtt).isSelected();
			}
		});
	}

	/**
	 * 
	 * @param compAtt
	 * @param compMemberContainer
	 * @return all <code>CompMember</code>s which are selected.
	 */
	private static Iterable<CompMember> getSelectedCompositeMembers(final CompAttrib compAtt,
			final SelectableBeanItemContainer<CompMember> compMemberContainer) {
		if (AttributesContainerCache.NULL == compMemberContainer) {
			return compAtt.getCompMember();
		}
		return Iterables.filter(compAtt.getCompMember(), new Predicate<CompMember>() {
			@Override
			public boolean apply(CompMember compMember) {
				return compMemberContainer.getItem(compMember).isSelected();
			}
		});
	}

	/**
	 * 
	 * @param mbean
	 * @param attributesContainer
	 * @return all Attributes which are selected.
	 */
	private static Iterable<Attrib> getSelectedAttributes(final Mbean mbean,
			final SelectableBeanItemContainer<Attrib> attributesContainer) {
		if (AttributesContainerCache.NULL == attributesContainer) {
			return mbean.getAttrib(); // no change made, return all
		}
		return Iterables.filter(mbean.getAttrib(), new Predicate<Attrib>() {
			@Override
			public boolean apply(Attrib attrib) {
				return attributesContainer.getItem(attrib).isSelected();
			}
		});
	}
}
