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

package org.opennms.features.vaadin.jmxconfiggenerator.ui;

import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import org.kohsuke.args4j.Option;
import org.opennms.features.jmxconfiggenerator.Starter;
import org.opennms.features.vaadin.jmxconfiggenerator.JmxConfigGeneratorUI;
import org.opennms.features.vaadin.jmxconfiggenerator.data.MetaConfigModel;
import org.opennms.features.vaadin.jmxconfiggenerator.data.ServiceConfig;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;

import java.util.HashMap;
import java.util.Map;

/**
 * This form handles editing of a {@link ServiceConfig} model.
 * 
 * @author Markus von Rüden <mvr@opennms.com>
 * @see ServiceConfig
 */
@SuppressWarnings("deprecation")
public class ConfigView extends FormLayout implements View, ClickListener {
    private static final long serialVersionUID = -9179098093927051983L;
    private JmxConfigGeneratorUI app;
	private ButtonPanel buttonPanel = new ButtonPanel(this);
	private FieldGroup configFieldGroup;

	public ConfigView(JmxConfigGeneratorUI app) {
		this.app = app;
		this.configFieldGroup = new FieldGroup();
		this.configFieldGroup.setItemDataSource(new BeanItem<>(new ServiceConfig()));

		initFields();

		setImmediate(true);
		setMargin(true);
		setSpacing(true);
		setSizeFull();

		addComponent(buttonPanel);
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		UiModel model = ((JmxConfigGeneratorUI) UI.getCurrent()).getUiModel();
      	configFieldGroup.setItemDataSource(new BeanItem<>(model.getServiceConfig()));
        updateAuthenticationFields(false); // default -> hide those fields
    }

	@Override
	public void buttonClick(ClickEvent event) {
		if (buttonPanel.getNext().equals(event.getButton())) {
			if (!configFieldGroup.isValid()) {
				UIHelper.showValidationError(
						"There are still errors on this page. You cannot continue. Please check if all required fields have been filled.");
				return;
			}
			try {
				configFieldGroup.commit();
				app.updateView(UiState.MbeansDetection);
			} catch (FieldGroup.CommitException e) {
				UIHelper.showNotification("An unexpected error occured.");
			}
		}
		if (buttonPanel.getPrevious().equals(event.getButton())) {
			app.updateView(UiState.IntroductionView);
		}
	}

	/**
	 * Toggles the visibility of user and password fields. The fields are shown
	 * if "authenticate" checkbox is presssed. Otherwise they are not shown.
	 */
	private void updateAuthenticationFields(boolean visible) throws Property.ReadOnlyException, ConversionException {
		((Field<Boolean>) configFieldGroup.getField(MetaConfigModel.AUTHENTICATE)).setValue(visible);
		configFieldGroup.getField(MetaConfigModel.USER).setVisible(visible);
		configFieldGroup.getField(MetaConfigModel.PASSWORD).setVisible(visible);
		if (!visible) {
			configFieldGroup.getField(MetaConfigModel.USER).setValue(null);
			configFieldGroup.getField(MetaConfigModel.PASSWORD).setValue(null);
		}
	}

	/**
	 * DefaultFieldFactory works for us, we only add some additional stuff to
	 * each field -> if needed.
	 * 
	 */
	private void initFields() {
		CheckBox authenticateField = new CheckBox();
		authenticateField.setCaption(MetaConfigModel.AUTHENTICATE);
		authenticateField.addListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				updateAuthenticationFields((Boolean) event.getProperty().getValue());
			}
		});

		TextField userField = new TextField();
		userField.setCaption("User");
		userField.setNullRepresentation("");

		PasswordField passwordField = new PasswordField();
		passwordField.setCaption("Password");
		passwordField.setNullRepresentation("");

		final TextField serviceNameField = new TextField();
		serviceNameField.setCaption("Service name");
		serviceNameField.setNullRepresentation("");
		serviceNameField.setRequired(true);
		serviceNameField.setRequiredError("required");
		serviceNameField.addValidator(new RegexpValidator("^[A-Za-z0-9_-]+$",
				"You must specify a valid name. Allowed characters: (A-Za-z0-9_-)"));

		final TextField hostNameField = new TextField();
		hostNameField.setCaption("Host");
		hostNameField.setRequired(true);
		hostNameField.setRequiredError("required");

		final TextField portField = new TextField();
		portField.setCaption("Port");
		portField.setRequired(true);
		portField.setRequiredError("required");

		addComponent(serviceNameField);
		addComponent(hostNameField);
		addComponent(portField);
		addComponent(authenticateField);
		addComponent(userField);
		addComponent(passwordField);

		addComponent(configFieldGroup.buildAndBind(MetaConfigModel.JMXMP));
		addComponent(configFieldGroup.buildAndBind(MetaConfigModel.SKIP_DEFAULT_VM));
		addComponent(configFieldGroup.buildAndBind(MetaConfigModel.RUN_WRITABLE_MBEANS));

		configFieldGroup.bind(serviceNameField, MetaConfigModel.SERVICE_NAME);
		configFieldGroup.bind(hostNameField, MetaConfigModel.HOST);
		configFieldGroup.bind(portField, MetaConfigModel.PORT);
		configFieldGroup.bind(passwordField, MetaConfigModel.PASSWORD);
		configFieldGroup.bind(authenticateField, MetaConfigModel.AUTHENTICATE);
		configFieldGroup.bind(userField, MetaConfigModel.USER);

		updateDescriptions();
		authenticateField.setDescription("Connection requires authentication");
	}

	/**
	 * Updates the descriptions (tool tips) of each field in the form using
	 * {@link #getOptionDescriptions()
     * }
	 */
	private void updateDescriptions() {
		final Map<String, String> optionDescriptions = getOptionDescriptions();
		for (Object property : configFieldGroup.getBoundPropertyIds()) {
			String propName = property.toString();
			if (configFieldGroup.getField(propName) != null && optionDescriptions.get(propName) != null) {
				Field field = configFieldGroup.getField(propName);
				if (field instanceof AbstractComponent) {
					((AbstractComponent) field).setDescription(optionDescriptions.get(propName));
				}
			}
		}
	}

	/**
	 * In class {@link org.opennms.features.jmxconfiggenerator.Starter} are several
	 * command line options defined. Each option has a name (mandatory) and a
	 * description (optional). This method gets all descriptions and assign each
	 * description to the name. If the option starts with at least one '-' all
	 * '-' are removed. Therefore the builded map looks like:
	 * 
	 * <pre>
	 *      {key} -> {value}
	 *      "force" -> "this option forces the deletion of the file"
	 * </pre>
	 * 
	 * @return a Map containing a description for each option defined in
	 *         {@link org.opennms.features.jmxconfiggenerator.Starter}
	 * @see org.opennms.features.jmxconfiggenerator.Starter
	 */
	private Map<String, String> getOptionDescriptions() {
		Map<String, String> optionDescriptions = new HashMap<>();
		for (java.lang.reflect.Field f : Starter.class.getDeclaredFields()) {
			Option ann = f.getAnnotation(Option.class);
			if (ann == null || ann.usage() == null) {
				continue;
			}
			optionDescriptions.put(f.getName(), ann.usage());
			optionDescriptions.put(ann.name().replaceAll("-", ""), ann.usage());
		}
		return optionDescriptions;
	}
}
