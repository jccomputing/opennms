/*******************************************************************************
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans;

import com.vaadin.data.Validator;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import org.opennms.features.vaadin.jmxconfiggenerator.Config;
import org.opennms.features.vaadin.jmxconfiggenerator.data.SelectionChangedListener;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.NameValidator;


/**
 * Handles the editing of the MBeans name.
 *
 * @author Markus von Rüden
 */
public class NameEditForm extends Panel implements SelectionChangedListener {

    private final MBeansController controller;
    private final Validator nameValidator = new NameValidator();
    private FormParameter parameter;
    private FieldGroup fieldGroup;
    private FormLayout contentLayout = new FormLayout();
    private TextField editableField;
    private TextField nonEditableField;

    public NameEditForm(MBeansController controller) {
        this.controller = controller;

        initFields();

        setContent(contentLayout);
        setWidth(100, Unit.PERCENTAGE);
        setHeight(Config.NAME_EDIT_FORM_HEIGHT, Unit.PIXELS);
        setReadOnly(true);
        setImmediate(true);
    }

    protected void setParameter(FormParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent changeEvent) {
        if (parameter != null) {
            final BeanItem beanItem = new BeanItem(changeEvent.getSelectedBean(), parameter.getEditablePropertyName(), parameter.getNonEditablePropertyName());

            setCaption(parameter.getCaption());

            fieldGroup = new FieldGroup();
            fieldGroup.bind(editableField, parameter.getEditablePropertyName());
            fieldGroup.bind(nonEditableField, parameter.getNonEditablePropertyName());
            fieldGroup.setItemDataSource(beanItem);
            fieldGroup.getField(parameter.getNonEditablePropertyName()).setCaption(parameter.getNonEditablePropertyCaption());
            fieldGroup.getField(parameter.getNonEditablePropertyName()).setReadOnly(true);
            fieldGroup.getField(parameter.getEditablePropertyName()).setCaption(parameter.getEditablePropertyCaption());
            fieldGroup.getField(parameter.getEditablePropertyName()).setReadOnly(false);

            validate();
        }
    }

    private void initFields() {
        nonEditableField = new TextField();
		nonEditableField.setWidth(400, Unit.PIXELS);
        nonEditableField.setReadOnly(true);
        nonEditableField.setEnabled(false);

        editableField = new TextField();
		editableField.setWidth(400, Unit.PIXELS);
        editableField.setRequired(true);
        editableField.setRequiredError("You must provide a value.");
        editableField.setValidationVisible(false);
        editableField.addValidator(nameValidator);
        editableField.setTextChangeTimeout(200);
        editableField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                editableField.setComponentError(null);
                editableField.setValue(event.getText());
                editableField.validate();
            }
        });

        contentLayout.addComponent(editableField);
        contentLayout.addComponent(nonEditableField);
    }

    public void validate() throws Validator.InvalidValueException {
        setComponentError(null);

        for (Field eachField : fieldGroup.getFields()) {
            try {
                eachField.validate();
                // reset previous shown errors
                ((AbstractComponent) eachField).setComponentError(null);
            } catch (Validator.InvalidValueException ex) {
                // set new error
                ((AbstractComponent) eachField).setComponentError(new UserError(ex.getMessage()));
                throw ex;
            }
        }
    }

    public void commit() {
        try {
            if (fieldGroup.isValid()) {
                fieldGroup.commit();
            }
        } catch (FieldGroup.CommitException e) {
            UIHelper.showValidationError("There are errors in this view.");
        }
    }
}
