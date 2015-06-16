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

import com.vaadin.data.Container;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import org.opennms.features.vaadin.jmxconfiggenerator.Config;
import org.opennms.features.vaadin.jmxconfiggenerator.data.MetaAttribItem;
import org.opennms.features.vaadin.jmxconfiggenerator.data.MetaAttribItem.AttribType;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.AttributeNameValidator;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.validators.UniqueAttributeNameValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Markus von Rüden
 */
public class AttributesTable<T> extends Table {

	private final Map<Object, Field<String>> fieldsToValidate = new HashMap<>();
	private List<Field<?>> fields = new ArrayList<>();
	private final UniqueAttributeNameValidator uniqueAttributeNameValidator;

	public AttributesTable(NameProvider provider) {
		uniqueAttributeNameValidator =  new UniqueAttributeNameValidator(provider, fieldsToValidate);
		setSizeFull();
		setSelectable(false);
		setEditable(true);
		setValidationVisible(true);
		setReadOnly(true);
		setImmediate(true);
		setTableFieldFactory(new AttributesTableFieldFactory());
		setValidationVisible(false);
	}

	public void modelChanged(T bean, Container container) {
		if (getData() == bean) return;
		setData(bean);
		fieldsToValidate.clear();
		fields.clear();
		setContainerDataSource(container);
		if (getContainerDataSource() == AttributesContainerCache.NULL) return;
		setVisibleColumns(
				MetaAttribItem.SELECTED,
				MetaAttribItem.NAME,
				MetaAttribItem.ALIAS,
				MetaAttribItem.TYPE);

		validate();
	}

	private class AttributesTableFieldFactory implements TableFieldFactory {

		private final Validator nameValidator = new AttributeNameValidator();
		private final Validator lengthValidator = new StringLengthValidator(String.format("Maximum length is %d", Config.ATTRIBUTES_ALIAS_MAX_LENGTH), 0, Config.ATTRIBUTES_ALIAS_MAX_LENGTH, false);

		@Override
		public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
			Field<?> field = null;
			if (propertyId.toString().equals(MetaAttribItem.ALIAS)) {
				Field<String> tf = new TableTextFieldWrapper(createAlias(itemId));
				fieldsToValidate.put(itemId, tf); //is needed to decide if this table is valid or not
				field = tf;
			}
			if (propertyId.toString().equals(MetaAttribItem.SELECTED)) {
				CheckBox c = new CheckBox();
				c.setBuffered(true);
				field = c;
			}
			if (propertyId.toString().equals(MetaAttribItem.TYPE)) {
				field = createType(itemId);
			}
			if (field == null) return null;
			fields.add(field);
			return field;
		}

		private ComboBox createType(Object itemId) {
			ComboBox select = new ComboBox();
			for (AttribType type : AttribType.values())
				select.addItem(type.name());
			select.setValue(AttribType.valueOf(itemId).name());
			select.setNullSelectionAllowed(false);
			select.setData(itemId);
			select.setBuffered(true);
			return select;
		}

		private TextField createAlias(Object itemId) {
			final TextField tf = new TextField();
			tf.setValidationVisible(false);
			tf.setBuffered(true);
			tf.setImmediate(true);
			tf.setRequired(true);
			tf.setWidth(300, Unit.PIXELS);
			tf.setMaxLength(Config.ATTRIBUTES_ALIAS_MAX_LENGTH);
			tf.setRequiredError("You must provide a name.");
			tf.addValidator(nameValidator);
			tf.addValidator(lengthValidator);
			tf.addValidator(uniqueAttributeNameValidator);
			tf.setTextChangeTimeout(200);
			tf.addTextChangeListener(new FieldEvents.TextChangeListener() {
				@Override
				public void textChange(FieldEvents.TextChangeEvent event) {
					tf.setComponentError(null);
					tf.setValue(event.getText());
					tf.validate();
				}
			});
			tf.setData(itemId);
			return tf;
		}
	}

	@Override
	public void commit() throws SourceException, InvalidValueException {
		super.commit();
		for (Field<?> f : fields) {
			f.commit();
		}
	}

	@Override
	public void discard() throws SourceException {
		super.discard();
		for (Field<?> f : fields) {
			f.discard();
		}
	}

	@Override
	public void validate() throws InvalidValueException {
		InvalidValueException validationException = null;
		try {
			super.validate();
		} catch (Validator.InvalidValueException ex) {
			validationException = ex;
		}

		//validators must be invoked manually
		for (Field<?> tf : fieldsToValidate.values()) {
			try {
				tf.validate();
				((AbstractComponent) tf).setComponentError(null); // reset previous errors
			} catch (InvalidValueException ex) {
				// we want to validate ALL fields to show all errors at once
				validationException = ex;
				((AbstractComponent) tf).setComponentError(new UserError(ex.getMessage()));
			}
		}
		if (validationException != null) throw validationException;
	}

	@Override
	public boolean isValid() {
		try {
			validate();
			return true;
		} catch (InvalidValueException invex) {
			return false;
		}
	}
}
