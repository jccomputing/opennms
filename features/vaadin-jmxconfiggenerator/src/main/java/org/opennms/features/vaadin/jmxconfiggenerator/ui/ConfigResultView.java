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

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.vaadin.jmxconfiggenerator.JmxConfigGeneratorUI;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel.OutputDataKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents the result view. It shows all generated configurations (including
 * some description texts) to the user.
 * 
 * @author Markus von Rüden <mvr@opennms.com>
 */
public class ConfigResultView extends CustomComponent implements Button.ClickListener, View {

	/**
	 * The name of the downlaodable zip archive.
	 */
	private static String DOWNLOAD_FILE_NAME = "jmx-config-files.zip";

	/**
	 * The TabSheet for the config and description content.
	 */
	private TabSheet tabSheet = new TabSheet();

	/**
	 * Stores the content.
	 */
	private Map<UiModel.OutputDataKey, TabContent> tabContentMap = new HashMap<>();

	/**
	 * Panel for previous and download buttons.
	 */
	private final ButtonPanel buttonPanel = new ButtonPanel(this);
	private final JmxConfigGeneratorUI app;

	private final FileDownloader fileDownloader;

	public ConfigResultView(JmxConfigGeneratorUI app) {
		this.app = app;
		setSizeFull();

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.addComponent(tabSheet);
		mainLayout.addComponent(buttonPanel);

		tabSheet.setSizeFull();
		tabContentMap.put(OutputDataKey.JmxDataCollectionConfig, new TabContent(OutputDataKey.JmxDataCollectionConfig));
		tabContentMap.put(OutputDataKey.SnmpGraphProperties, new TabContent(OutputDataKey.SnmpGraphProperties));
		tabContentMap.put(OutputDataKey.CollectdConfigSnippet, new TabContent(OutputDataKey.CollectdConfigSnippet));

		// add all tabs
		for (TabContent eachContent : tabContentMap.values()) {
			tabSheet.addTab(eachContent, eachContent.getCaption());
		}

		tabSheet.setSelectedTab(0); // select first component!

		buttonPanel.getNext().setCaption("download all");
		buttonPanel.getNext().setDescription("Download a zip file containing the JMX datacollection configuration");
		buttonPanel.getNext().setIcon(IconProvider.BUTTON_SAVE);

		// this is a dummy resource as we are going to set the real
		// resource when entering the view.
		fileDownloader = new FileDownloader(new Resource() {
			@Override
			public String getMIMEType() {
				return "application/unknown";
			}
		});
		fileDownloader.extend(buttonPanel.getNext());

		mainLayout.setExpandRatio(tabSheet, 1);
		mainLayout.setSizeFull();
		setCompositionRoot(mainLayout);
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource().equals(buttonPanel.getPrevious())) {
			app.updateView(UiState.MbeansView);
		}
	}

	/**
	 * Creates a map with the content for the zip file to create.
	 */
	private Map<String, String> createZipContentMap() {
		// key: filename, value: file content
		Map<String, String> zipContentMap = new HashMap<>();
		// create map for the downloadable zip file
		for (OutputDataKey eachKey : tabContentMap.keySet()) {
			// config file
			zipContentMap.put(eachKey.getDownloadFilename(), tabContentMap.get(eachKey).getConfigContent());
			// description file
//			zipContentMap.put(flatten(eachKey.getDescriptionFilename()), tabContentMap.get(eachKey).getDescriptionText());
		}
		return zipContentMap;
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		UiModel newValue = UIHelper.getCurrent().getUiModel();
		if (newValue == null) return;
		for (Entry<UiModel.OutputDataKey, String> eachEntry : newValue.getOutputMap().entrySet()) {
			if (tabContentMap.get(eachEntry.getKey()) != null) {
				tabContentMap.get(eachEntry.getKey()).setConfigContent(eachEntry.getValue());
			}
		}
		fileDownloader.setFileDownloadResource(new DownloadResource(createZipContentMap(), DOWNLOAD_FILE_NAME, getUI()));
	}

	/**
	 * Represents a downloadable Resource. If opened in the Application Window a
	 * download via the browser is initiated. Usually a "save or open"-dialogue
	 * shows up.
	 * 
	 * @author Markus von Rüden <mvr@opennms.com>
	 * 
	 */
	private static class DownloadResource extends StreamResource {

		/**
		 * 
		 * @param zipContentMap
		 *            key: Filename, value: File content
		 * @param application
		 * @param filename
		 *            The filename for the downloadable zip file.
		 */
		public DownloadResource(final Map<String, String> zipContentMap, final String filename,
				final UI application) {
			super(new StreamSource() {
				@Override
				public InputStream getStream() {
					return new ByteArrayInputStream(getZipByteArray(zipContentMap));
				}
			}, filename);
			// for "older" browsers to force a download,
			// otherwise it may not be downloaded
			setMIMEType("application/unknown");
		}

		/**
		 * Set DownloadStream-Parameter "Content-Disposition" to atachment,
		 * therefore the Stream is downloaded and is not parsed as for example
		 * "normal" xml.
		 */
		@Override
		public DownloadStream getStream() {
			DownloadStream ds = super.getStream();
			ds.setParameter("Content-Disposition", "attachment; filename=\"" + getFilename() + "\"");
			return ds;
		}

		/**
		 * Creates a byte-Array which represents the content from the
		 * zipContentMap in zipped form. The files are defined by the given Map.
		 * The key of the map defines the name in the zip archive. The value of
		 * the map defines file's content.
		 * 
		 * @param zipContentMap
		 *            The map which contains the filenames and file contents for
		 *            the zip archive to create.
		 * @return a byte-Array which represents the zip file.
		 */
		private static byte[] getZipByteArray(Map<String, String> zipContentMap) {
			try {
				// create output streams ...
				ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
				ZipOutputStream out = new ZipOutputStream(arrayOutputStream);

				// Compress the files
				for (Entry<String, String> eachEntry : zipContentMap.entrySet()) {
					out.putNextEntry(new ZipEntry(eachEntry.getKey()));
					out.write(eachEntry.getValue().getBytes());
					out.closeEntry();
				}
				out.close(); // Complete the ZIP file
				arrayOutputStream.close();
				return arrayOutputStream.toByteArray();
			} catch (IOException e) {
				UIHelper.showNotification("Download Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
				return new byte[0];
			}
		}
	}

	private static class TabContent extends HorizontalSplitPanel {

		/**
		 * TextArea for the configuration content (e.g. the SNMP-Graph-Properties)
		 */
		private final TextArea configTextArea = new TextArea();

		/**
		 * Label for the description content (e.g. an explanation of the SNMP-Graph-Properties and what to do with that file).
		 */
		private final Label descriptionLabel;

		private TabContent(OutputDataKey key) {
			setSizeFull();
			setLocked(false);
			setSplitPosition(50, Unit.PERCENTAGE);
			configTextArea.setSizeFull();
			configTextArea.setHeight(800, Unit.PIXELS);
			descriptionLabel = new Label(
					UIHelper.loadContentFromFile(getClass(), key.getDescriptionFilename()),
					ContentMode.HTML);
			addComponent(configTextArea);
			addComponent(descriptionLabel);
			setCaption(key.getTitle());
		}

		public String getDescriptionText() {
			return descriptionLabel.getValue();
		}

		/**
		 * Sets the content of the {@linkplain #configTextArea}.
		 * 
		 * @param newConfigContent The new configuration content.
		 */
		public void setConfigContent(String newConfigContent) {
			configTextArea.setValue(newConfigContent);
		}

		public String getConfigContent() {
			return configTextArea.getValue() == null ? "" : configTextArea.getValue();
		}
	}
}
