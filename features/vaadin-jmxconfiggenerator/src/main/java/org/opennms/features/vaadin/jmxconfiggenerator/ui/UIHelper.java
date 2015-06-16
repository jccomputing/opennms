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

import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import org.opennms.features.vaadin.jmxconfiggenerator.Config;
import org.opennms.features.vaadin.jmxconfiggenerator.JmxConfigGeneratorUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class provides several helper methods for ui stuff, e.g. creating a
 * button. So the amount of code is reduced generally.
 * 
 * @author Markus von Rüden
 */
public abstract class UIHelper {

	private static final Logger LOG = LoggerFactory.getLogger(UIHelper.class);

	public static Button createButton(
			final String buttonCaption,
			final String buttonDescription,
			final Resource icon,
			final ClickListener clickListener) {
		Button button = new Button();
		button.setCaption(buttonCaption);
		button.setIcon(icon);
		if (buttonDescription != null) button.setDescription(buttonDescription);
		if (clickListener != null) button.addClickListener(clickListener);
		return button;
	}

	public static Button createButton(final String buttonCaption, String buttonDescription, final Resource icon) {
		return createButton(buttonCaption, buttonDescription, icon, null);
	}


	/**
	 * Closes the given Closeable silently. That means if an error during
	 * {@link Closeable#close()} occurs, the IOException is catched and logged.
	 * No further information is forwarded.
	 */
	public static void closeSilently(Closeable closeable) {
		if (closeable == null) return; // prevent NPE
		try {
			closeable.close();
		} catch (IOException e) {
			LOG.warn("Error while closing resource '{}'.", closeable, e);
		}
	}

	/**
	 * Loads the <code>resourceName</code> from the classpath using the given
	 * <code>clazz</code>. If the resource couldn't be loaded an empty string is
	 * returned.
	 * 
	 * @param clazz
	 *            The class to use for loading the resource.
	 * @param resourceName
	 *            The name of the resource to be loaded (e.g.
	 *            /folder/filename.txt)
	 * @return The content of the file, each line separated by line.separator or
	 *         empty string if the resource does not exist.
	 */
	public static String loadContentFromFile(final Class<?> clazz, final String resourceName) {
		// prevent NullPointerException
		if (clazz == null || resourceName == null) {
			LOG.warn("loadContentFromFile not invoked, due to null arguments");
			return "";
		}

		// check if resource is there
		final InputStream is = clazz.getResourceAsStream(resourceName);
		if (is == null) {
			LOG.warn("Resource '{}' couldn't be loaded from class '{}'", resourceName, clazz.getName());
			return "";
		}

		// resource is there, so we can try loading it
		BufferedReader bufferedReader = null;
		StringBuilder result = new StringBuilder(100);
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(is));
			String eachLine = null;
			while ((eachLine = bufferedReader.readLine()) != null) {
				result.append(eachLine);
				result.append(System.getProperty("line.separator"));
			}
		} catch (IOException ioEx) {
			LOG.error("Error while reading resource from '{}'.", resourceName, ioEx);
		} finally {
			closeSilently(bufferedReader);
		}
		return result.toString();
	}

	/**
	 * Shows a validation error to the user.
	 * 
	 * @param errorMessage
	 *            the error message.
	 */
	public static void showValidationError(String errorMessage) {
		showNotification("Validation Error", errorMessage != null ? errorMessage : "An unknown error occurred.", Type.ERROR_MESSAGE);
	}

	public static JmxConfigGeneratorUI getCurrent() {
		return (JmxConfigGeneratorUI) UI.getCurrent();
	}

	public static void showNotification(String message) {
		showNotification(message, null, Type.ERROR_MESSAGE);
	}

	public static void showNotification(String title, String message, Type type) {
		Notification notification = new Notification(title, message, type, true);
		notification.setDelayMsec(Config.NOTIFICATION_DELAY);
		notification.show(Page.getCurrent());
	}
}
