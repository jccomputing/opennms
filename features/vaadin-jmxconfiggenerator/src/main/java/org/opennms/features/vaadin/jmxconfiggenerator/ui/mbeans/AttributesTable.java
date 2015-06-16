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
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.FieldEvents;
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
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans.validation.AttributeNameValidator;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans.validation.UniqueAttributeNameValidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Markus von Rüden
 */
public class AttributesTable<T> extends Table {

	private final Map<Object[], Field<String>> fieldsToValidate = new HashMap<>();
	private final Map<Object[], Field<?>> fields = new HashMap<>();
	private final SelectionManager selectionManager;
	private final TableFieldFactory tableFieldFactory;

	public AttributesTable(SelectionManager selectionManager) {
		this.selectionManager = selectionManager;
		this.tableFieldFactory = new AttributesTableFieldFactory();
		setSizeFull();
		setSelectable(false);
		setEditable(true);
		setValidationVisible(true);
		setReadOnly(true);
		setImmediate(true);
		setTableFieldFactory(new ReferenceTableFieldFactory());
		setValidationVisible(false);
	}

	public void modelChanged(T bean, Container container) {
		if (getData() == bean) return;
		setData(bean);

		fieldsToValidate.clear();
		fields.clear();

		// we initialize the tableFieldFactory because by default pagination is enabled, which
		// causes the table not to know all fields. But the validation is bound to a field, therefore
		// we need to create all fields in advance.
		for (Object eachItemId : container.getItemIds()) {
			for (Object eachPropertyId : container.getContainerPropertyIds()) {
				Field<?> eachField = tableFieldFactory.createField(container, eachItemId, eachPropertyId, this);
				if (eachField != null) {
					fields.put(computeKey(eachItemId, eachPropertyId), eachField);
					if (MetaAttribItem.ALIAS.equals(eachPropertyId)) {
						fieldsToValidate.put(computeKey(eachItemId, eachPropertyId), (Field<String>) eachField);

						// we have to set this manually, otherwise validation does not work
						Item item = container.getItem(eachItemId);
						Property property = item.getItemProperty(eachPropertyId);
						((Field<String>)eachField).setValue((String) property.getValue());
					}
				}
			}
		}

		setContainerDataSource(container);

		setVisibleColumns(
				MetaAttribItem.SELECTED,
				MetaAttribItem.NAME,
				MetaAttribItem.ALIAS,
				MetaAttribItem.TYPE);

		// we initially validate to ensure the ui is ok ant not in a broken state.
		// Only to allow this validation we have to do the initialization of the fields (See above) manually.
		// This is not ideal, but it seems to be the only way.
		validateFields(true);
	}

	private static Field<?> findFieldByKey(Map<Object[], Field<?>> inputMap, Object... key) {
		for (Object[] eachKey : inputMap.keySet()) {
			if (Arrays.equals(eachKey, key)) {
				return inputMap.get(eachKey);
			}
		}
		return null;
	}

	private static Object[] computeKey(Object... input) {
		return input;
	}

	//  This TableFieldFactory simply uses the already created fields and returns them (or null).
	private class ReferenceTableFieldFactory implements TableFieldFactory {

		@Override
		public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
			Object[] key = computeKey(itemId, propertyId);
			Field<?> field = findFieldByKey(fields, key);
			return field;
		}
	}

	// The default field factory for the attributes table.
	private class AttributesTableFieldFactory implements TableFieldFactory {

		private final Validator nameValidator = new AttributeNameValidator();
		private final Validator lengthValidator = new StringLengthValidator(String.format("Maximum length is %d", Config.ATTRIBUTES_ALIAS_MAX_LENGTH), 0, Config.ATTRIBUTES_ALIAS_MAX_LENGTH, false);
		private final Validator uniqueAttributeNameValidator =  new UniqueAttributeNameValidator(new DefaultNameProvider(selectionManager, new NameProvider.FieldValueProvider() {
			@Override
			public String getFieldValue(Object input) {
				for (Map.Entry<Object[], Field<String>> eachEntry : fieldsToValidate.entrySet()) {
					if (Objects.equals(input, eachEntry.getKey()[0])) {
						return eachEntry.getValue().getValue();
					}
				}
				return null;
			}
		}));

		@Override
		public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
			Field<?> field = null;
			if (propertyId.toString().equals(MetaAttribItem.ALIAS)) {
				Field<String> tf = new TableTextFieldWrapper(createAlias(itemId));
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
			return field;
		}

		private ComboBox createType(Object itemId) {
			ComboBox select = new ComboBox();
			for (AttribType type : AttribType.values()) {
				select.addItem(type.name());
			}
			select.setValue(AttribType.gauge);
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
			tf.setData(itemId);
			tf.setTextChangeTimeout(200);
			tf.addTextChangeListener(new FieldEvents.TextChangeListener() {
				@Override
				public void textChange(FieldEvents.TextChangeEvent event) {
					tf.setValue(event.getText());
					UIHelper.validateField(tf, true);
				}
			});
			return tf;
		}
	}

	@Override
	public void commit() throws SourceException, InvalidValueException {
		super.commit();
		if (isReadOnly()) return; //we do not commit on read only
		for (Field<?> f : fields.values()) {
			f.commit();
		}
	}

	@Override
	public void discard() throws SourceException {
		super.discard();
		for (Field<?> f : fields.values()) {
			f.discard();
		}
	}

	@Override
	public void validate() throws InvalidValueException {
		super.validate();
		validateFields(false);
	}

	private void validateFields(boolean swallowValidationExceptions) throws InvalidValueException {
		UIHelper.validateFields(fieldsToValidate.values(), swallowValidationExceptions);
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
