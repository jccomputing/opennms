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
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.vaadin.jmxconfiggenerator.JmxConfigGeneratorUI;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ButtonPanel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UiState;

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
			model.setJmxDataCollectionAccordingToSelection(controller.createJmxDataCollectionAccordingToSelection(model));
			app.updateView(UiState.ResultConfigGeneration);
		}
	}

	private AbstractSplitPanel initMainPanel(Component first, Component second) {
		AbstractSplitPanel layout = new HorizontalSplitPanel();
		layout.setSizeFull();
		layout.setLocked(false);
		layout.setSplitPosition(20, Unit.PERCENTAGE);
		layout.setFirstComponent(first);
		layout.setSecondComponent(second);
		layout.setCaption(first.getCaption());
		return layout;
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		UiModel newModel = UIHelper.getCurrent().getUiModel();
		model = newModel; // TODO MVR was machen wir hiermit?

		controller.updateDataSource(newModel);
	}

	private boolean isValid() {
		return controller.isValid();
	}
}
