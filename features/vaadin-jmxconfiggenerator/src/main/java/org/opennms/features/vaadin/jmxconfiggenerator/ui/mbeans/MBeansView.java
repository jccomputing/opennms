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

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.vaadin.jmxconfiggenerator.JmxConfigGeneratorUI;
import org.opennms.features.vaadin.jmxconfiggenerator.data.JmxCollectionCloner;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ButtonPanel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UiState;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Attrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompAttrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompMember;
import org.opennms.xmlns.xsd.config.jmx_datacollection.JmxDatacollectionConfig;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Mbean;

import java.util.List;

public class MBeansView extends VerticalLayout implements ClickListener, View {

	/**
	 * Handles the ui behaviour.
	 */
	private final MBeansController controller;

	/**
	 * We need an instance of the current UiModel to create the output jmx
	 * config model when clicking on 'next' button.
	 */
	private UiModel model;
	private final JmxConfigGeneratorUI app;
	private final MBeansTree mbeansTree;
	private final MBeansContentPanel mbeansContentPanel;
	private final ButtonPanel buttonPanel = new ButtonPanel(this);

	public MBeansView(JmxConfigGeneratorUI app) {
		this.app = app;
		controller = new MBeansController();
		mbeansContentPanel = new MBeansContentPanel(controller);
		mbeansTree = new MBeansTree(controller);

		controller.registerSelectionChangedListener(mbeansContentPanel);
		controller.setMbeansContentPanel(mbeansContentPanel);
		controller.setMbeansTree(mbeansTree);

		AbstractSplitPanel mainPanel = initMainPanel(mbeansTree, mbeansContentPanel);

		addComponent(mainPanel);
		addComponent(buttonPanel);
		setExpandRatio(mainPanel, 1);
		setSizeFull();
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton().equals(buttonPanel.getPrevious())) {
			app.updateView(UiState.ServiceConfigurationView);
		}
		if (event.getButton().equals(buttonPanel.getNext())) {
			if (!isValid()) {
				UIHelper.showValidationError("There are errors on this view. Please fix them first");
				return;
			}
			model.setJmxDataCollectionAccordingToSelection(createJmxDataCollectionAccordingToSelection(model, controller));
			app.updateView(UiState.ResultConfigGeneration);
		}
	}

	private AbstractSplitPanel initMainPanel(Tree first, Component second) {
		AbstractSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSizeFull();
		splitPanel.setLocked(false);
		splitPanel.setSplitPosition(25, Unit.PERCENTAGE);

		splitPanel.setFirstComponent(first);
		splitPanel.setSecondComponent(second);
		splitPanel.setCaption(first.getCaption());
		return splitPanel;
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		UiModel newModel = app.getUiModel();
		model = newModel; // TODO MVR was machen wir hiermit?

		controller.updateDataSource(model);
	}

	private boolean isValid() {
		return controller.isValid();
	}

	/**
	 * The whole point was to select/deselect
	 * Mbeans/Attribs/CompMembers/CompAttribs. In this method we simply create a
	 * JmxDatacollectionConfig considering the choices we made in the gui. To do
	 * this, we clone the original <code>JmxDatacollectionConfig</code>
	 * loaded at the beginning. After that we remove all
	 * MBeans/Attribs/CompMembers/CompAttribs and add all selected
	 * MBeans/Attribs/CompMembers/CompAttribs afterwards.
	 *
	 * @return
	 */
	private static JmxDatacollectionConfig createJmxDataCollectionAccordingToSelection(final UiModel uiModel, final SelectionManager selectionManager) {
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
		Iterable<Mbean> selectedMbeans = selectionManager.getSelectedMbeans();
		for (Mbean mbean : selectedMbeans) {
			/*
			 * We remove all Attributes from Mbean, because we only want
			 * selected ones.
			 */
			Mbean exportBean = JmxCollectionCloner.clone(mbean);
			exportBean.getAttrib().clear(); // we only want selected ones :)
			for (Attrib att : selectionManager.getSelectedAttributes(mbean)) {
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
			for (CompAttrib compAtt : selectionManager.getSelectedCompositeAttributes(mbean)) {
				CompAttrib cloneCompAtt = JmxCollectionCloner.clone(compAtt);
				cloneCompAtt.getCompMember().clear();
				for (CompMember compMember : selectionManager.getSelectedCompositeMembers(compAtt)) {
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
}
