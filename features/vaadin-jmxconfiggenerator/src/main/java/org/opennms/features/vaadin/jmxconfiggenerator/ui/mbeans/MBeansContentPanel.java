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

import com.vaadin.data.Validator;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.vaadin.jmxconfiggenerator.data.MetaMBeanItem;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectionChangedListener;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Attrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompAttrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompMember;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Mbean;

/**
 *
 * @author Markus von Rüden
 */
public class MBeansContentPanel extends VerticalLayout implements SelectionChangedListener {

	private static final FormParameter MBEAN_FORM_PARAMETER = new FormParameter() {
		@Override
		public String getCaption() {
			return "MBean details";
		}

		@Override
		public String getEditablePropertyName() {
			return MetaMBeanItem.NAME;
		}

		@Override
		public String getNonEditablePropertyName() {
			return MetaMBeanItem.OBJECTNAME;
		}

		@Override
		public String getEditablePropertyCaption() {
			return "Name";
		}

		@Override
		public String getNonEditablePropertyCaption() {
			return "Objectname";
		}
	};

	private static final FormParameter COMPOSITE_FORM_PARAMETER = new FormParameter() {
		@Override
		public String getCaption() {
			return "Composite details";
		}

		@Override
		public String getEditablePropertyName() {
			return "alias";
		}

		@Override
		public String getNonEditablePropertyName() {
			return "name";
		}

		@Override
		public String getEditablePropertyCaption() {
			return "Alias";
		}

		@Override
		public String getNonEditablePropertyCaption() {
			return "Name";
		}
	};

	private static final String CAPTION_FORMAT = "<b>%s</b>";

	private final AttributesTable attributesTable;

	private final NameEditForm nameEditForm;

	private final MBeansController controller;

	private final VerticalLayout contentLayout;

	private final VerticalLayout emptyLayout;

	private final Label captionLabel;

	public MBeansContentPanel(final MBeansController controller) {
		this.controller = controller;
		this.attributesTable = new AttributesTable(controller);

		emptyLayout = new VerticalLayout();
		emptyLayout.setSpacing(true);
		emptyLayout.setMargin(true);
		emptyLayout.addComponent(new Label("No MBean or Composite selected. Please select one to modify data."));

		nameEditForm = new NameEditForm();

		captionLabel = new Label();
		captionLabel.setContentMode(ContentMode.HTML);
		VerticalLayout attributeLayout = new VerticalLayout();
		attributeLayout.addComponent(captionLabel);
		attributeLayout.addComponent(attributesTable);
		attributeLayout.setSizeFull();
		attributeLayout.setExpandRatio(attributesTable, 1);

		contentLayout = new VerticalLayout();
		contentLayout.setSpacing(true);
		contentLayout.setMargin(true);
		contentLayout.addComponent(nameEditForm);
		contentLayout.addComponent(attributeLayout);
		contentLayout.setSizeFull();
		contentLayout.setExpandRatio(attributeLayout, 1);

		// by default we do not show details
		addComponent(emptyLayout);
		setSizeFull();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object selectedBean = event.getSelectedBean();

		if (selectedBean instanceof Mbean) {
			nameEditForm.setParameter(MBEAN_FORM_PARAMETER);
			attributesTable.modelChanged(selectedBean, controller.getContainer(Attrib.class, selectedBean));
			captionLabel.setValue(String.format(CAPTION_FORMAT, "MBean Attributes"));
			setContent(contentLayout);
			nameEditForm.selectionChanged(event);
			return;
		}

		if (selectedBean instanceof CompAttrib) {
			nameEditForm.setParameter(COMPOSITE_FORM_PARAMETER);
			attributesTable.modelChanged(selectedBean, controller.getContainer(CompMember.class, selectedBean));
			captionLabel.setValue(String.format(CAPTION_FORMAT, "Composite Members"));
			setContent(contentLayout);
			nameEditForm.selectionChanged(event);
			return;
		}
		setContent(emptyLayout);
	}

	private void setContent(Component component) {
		removeAllComponents();
		addComponent(component);
	}

	private Component getContent() {
		if (getComponentCount() == 0) {
			return null;
		}
		return getComponent(0);
	}

	private void validate() throws Validator.InvalidValueException {
		if (getContent() != emptyLayout) {
			nameEditForm.validate();
			attributesTable.validate();
		}
	}

	public boolean isValid() {
		try {
			validate();
			return true;
		} catch (Validator.InvalidValueException ex) {
			return false;
		}
	}

	public void commit() {
		if (getContent() != emptyLayout) {
			nameEditForm.commit();
			attributesTable.commit();
		}
	}

	public void reset() {
		setContent(emptyLayout);
	}

	public void discard() {
		if (getContent() != emptyLayout) {
			nameEditForm.discard();
			attributesTable.discard();
		}
	}
}
