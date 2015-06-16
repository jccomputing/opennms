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

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectableBeanItemContainer;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectionChangedListener;
import org.opennms.features.vaadin.jmxconfiggenerator.data.StringRenderer;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ConfirmationDialog;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans.validation.ValidationManager;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans.validation.ValidationResult;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Attrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompAttrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompMember;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Mbean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controls the "MbeansView".
 * 
 * @author Markus von Rüden
 */
public class MBeansController implements SelectionManager, NameProvider {

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

		public void reset() {
			update(null, null);
		}
	}

	/** Vaadin container for the items to show up in the tree. */
	private final MbeansHierarchicalContainer mbeansContainer = new MbeansHierarchicalContainer(this);

	private final MBeansItemStrategyHandler itemStrategyHandler = new MBeansItemStrategyHandler();

	private final ValidationManager validationManager = new ValidationManager(this, this);

	private final Selection currentSelection = new Selection();

	private final List<SelectionChangedListener> selectionChangedListener = new ArrayList<>();

	private final Map<Class<?>, AttributesContainerCache> attributesContainerCacheMap = new HashMap<>();

	private MBeansContentPanel mbeansContentPanel;

	private MBeansTree mbeansTree;

	public MBeansController() {
		attributesContainerCacheMap.put(Attrib.class, new AttributesContainerCache<>(Attrib.class, new AttributesContainerCache.AttributeCollector<Attrib, Mbean>() {
			@Override
			public List<Attrib> getAttributes(Mbean outer) {
				return outer.getAttrib();
			}
		}));
		attributesContainerCacheMap.put(CompAttrib.class, new AttributesContainerCache<>(
				CompAttrib.class, new AttributesContainerCache.AttributeCollector<CompAttrib, Mbean>() {
			@Override
			public List<CompAttrib> getAttributes(Mbean outer) {
				return outer.getCompAttrib();
			}
		}));
		attributesContainerCacheMap.put(CompMember.class, new AttributesContainerCache<>(
				CompMember.class, new AttributesContainerCache.AttributeCollector<CompMember, CompAttrib>() {
			@Override
			public List<CompMember> getAttributes(CompAttrib outer) {
				return outer.getCompMember();
			}
		}));
	}

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
	protected void selectItemInTree(final Object itemId) {
		if (currentSelection.getItemId() != itemId) {
			if (!mbeansContentPanel.isValid()) {
				new ConfirmationDialog()
						.withOkAction(new ConfirmationDialog.Action() {
							@Override
							public void execute(ConfirmationDialog window) {
								mbeansContentPanel.discard();
								currentSelection.update(itemId, mbeansContainer.getItem(itemId));
								notifyObservers(currentSelection);
							}
						})
						.withCancelAction(new ConfirmationDialog.Action() {
							@Override
							public void execute(ConfirmationDialog window) {
								// abort, do not continue -> revert selection
								// (the change is already visible to the client)
								mbeansTree.select(currentSelection.getItemId());
							}
						})
						.withCaption("Unsaved changes")
						.withDescription("The current view may contain unsaved changes.<br/>The changes are lost if you continue.<br/><br/>Do you really want to continue?")
						.open();
			} else {
				mbeansContentPanel.commit();
				currentSelection.update(itemId, mbeansContainer.getItem(itemId));
				notifyObservers(currentSelection);
			}
//			try {
//				mbeansContentPanel.validate();
				//validated = true;
				//updateValidState(currentSelection.getItemId(), true);
				//mbeansContentPanel.commit();
				//currentSelection.update(itemId, mbeansContainer.getItem(itemId));
				//notifyObservers(currentSelection);
				//mbeansTree.select(itemId);
//			} catch (Validator.InvalidValueException ex) {
//				updateValidState(currentSelection.getItemId(), false);
//				if (!validated)  {
//
//					mbeansTree.select(currentSelection.getItemId()); // revert Selection
//					UIHelper.showValidationError("Cannot change selection. The current view contains errors.");
//				}
//			}
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

	private void handleSelectDeselect(HierarchicalContainer container, Item item, Object itemId, boolean select) {
		final MBeansItemStrategyHandler.ItemStrategy itemStrategy = itemStrategyHandler.getStrategy(itemId.getClass());
		itemStrategy.handleSelectDeselect(item, itemId, select);
		itemStrategy.updateIcon(item);
		handleSelectDeselectForSelectableBeanItemContainer(itemId, select);
		if (!container.hasChildren(itemId)) return;
		// we are not done yet in the tree hierarchy, so continue
		for (Object childItemId : container.getChildren(itemId)) {
			handleSelectDeselect(container, container.getItem(childItemId), childItemId, select);
		}
	}

	private void handleSelectDeselectForSelectableBeanItemContainer(Object itemId, boolean select) {
		// we are now at the end of the hierarchy, handleSelectDeselect for attributes/composite members
		if (itemId instanceof Mbean) {
			SelectableBeanItemContainer<Attrib> attribContainer = getContainer(Attrib.class, itemId);
			attribContainer.selectAllItems(select);
		}
		if (itemId instanceof CompAttrib) {
			SelectableBeanItemContainer<CompMember> compMemberContainer = getContainer(CompMember.class, itemId);
			compMemberContainer.selectAllItems(select);
		}
	}

	protected <T, X> SelectableBeanItemContainer<T> getContainer(Class<T> type, X bean) {
		AttributesContainerCache containerCache = attributesContainerCacheMap.get(type);
		Objects.requireNonNull(containerCache);
		return containerCache.getContainer(bean);
	}

	protected MbeansHierarchicalContainer getMBeansHierarchicalContainer() {
		return mbeansContainer;
	}

	private <T, X> Collection<T> getSelectedAttributes(Class<T> type, X bean) {
		SelectableBeanItemContainer<T> container = getContainer(type, bean);
		return container.getSelectedAttributes();
	}

	@Override
	public Collection<Attrib> getSelectedAttributes(Mbean mbean) {
		return getSelectedAttributes(Attrib.class, mbean);
	}

	@Override
	public Collection<CompMember> getSelectedCompositeMembers(CompAttrib compAttrib) {
		return getSelectedAttributes(CompMember.class, compAttrib);
	}

	@Override
	public Collection<CompAttrib> getSelectedCompositeAttributes(Mbean mbean) {
		return getSelectedAttributes(CompAttrib.class, mbean);
	}

	@Override
	public Collection<Mbean> getSelectedMbeans() {
		return getMBeansHierarchicalContainer().getSelectedMbeans();
	}

	public void updateDataSource(UiModel newModel) {
		reset();
		mbeansContainer.updateDataSource(newModel);
		mbeansContentPanel.reset();
		Object firstItemId = mbeansTree.expandAllItems();
		mbeansTree.select(firstItemId);
		validate();
	}

	private void reset()  {
		currentSelection.reset();
		for (AttributesContainerCache eachCache : attributesContainerCacheMap.values()) {
			eachCache.clear();
		}
	}

	private ValidationResult validate() {
		ValidationResult result = validationManager.validate(mbeansContainer);
		Collection<ValidationResult.ValidationError> validationErrors = result.getValidationErrors(Mbean.class);
		for (ValidationResult.ValidationError eachError : validationErrors) {
			updateValidState(eachError.getErrorObject(), false);
		}
		return result;
	}

	protected boolean isValid() {
		return validate().isValid();
	}

	@Override
	public List<String> getNames() {
		return new DefaultNameProvider(this).getNames();
	}
}
