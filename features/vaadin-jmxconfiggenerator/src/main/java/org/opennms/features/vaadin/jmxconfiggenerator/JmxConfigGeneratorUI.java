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

package org.opennms.features.vaadin.jmxconfiggenerator;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.jmxconfiggenerator.Starter;
import org.opennms.features.jmxconfiggenerator.graphs.GraphConfigGenerator;
import org.opennms.features.jmxconfiggenerator.graphs.JmxConfigReader;
import org.opennms.features.jmxconfiggenerator.graphs.Report;
import org.opennms.features.jmxconfiggenerator.jmxconfig.JmxDatacollectionConfiggenerator;
import org.opennms.features.vaadin.jmxconfiggenerator.data.ServiceConfig;
import org.opennms.features.vaadin.jmxconfiggenerator.data.UiModel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ConfigResultView;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ConfigView;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.HeaderPanel;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.IntroductionView;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.ProgressWindow;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UiState;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans.MBeansView;
import org.opennms.xmlns.xsd.config.jmx_datacollection.JmxDatacollectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Collection;

@Theme("jmxconfiggenerator")
@Title("JmxConfigGenerator UI Tool")
@SuppressWarnings("serial")
public class JmxConfigGeneratorUI extends UI {

    private static final Logger LOG = LoggerFactory.getLogger(JmxConfigGeneratorUI.class);

	/**
	 * The Header panel which holds the steps which are necessary to complete
	 * the configuration for a new service to get collected.
	 * 
	 */
	private HeaderPanel headerPanel;

	/**
	 * The "content" panel which shows the view for the currently selected step
	 * of the configuration process.
	 */
	private Panel contentPanel;

	private ProgressWindow progressWindow;

	private UiModel model = new UiModel();
	private Navigator navigator;

	@Override
	protected void init(VaadinRequest request) {
		initHeaderPanel();
		initContentPanel();
		initMainLayout();
		initNavigator();

		updateView(UiState.IntroductionView);
	}

	// TODO MVR implement
	private void initNavigator() {
		navigator = new Navigator(this, contentPanel);
		// common views
		navigator.addView(UiState.IntroductionView.name(), new IntroductionView(this));
		navigator.addView(UiState.ServiceConfigurationView.name(), new ConfigView(this));
		navigator.addView(UiState.MbeansView.name(), new MBeansView(this));
		navigator.addView(UiState.ResultView.name(), new ConfigResultView(this));

		// "working views" need to be simulated, they do not actually ecist, but we need them for navigation
		navigator.addView(UiState.MbeansDetection.name(), new Navigator.EmptyView());
		navigator.addView(UiState.ResultConfigGeneration.name(), new Navigator.EmptyView());


		// We need to hook into the "view change" process to prevent changing to the "working views"
		// Instead we trigger a long running task and show a "please wait" window.
		navigator.addViewChangeListener(new ViewChangeListener() {
			@Override
			public boolean beforeViewChange(ViewChangeEvent event) {
				removeWindow(getProgressWindow());
				headerPanel.enter(event);

				final UiState uiState = UiState.valueOf(event.getViewName());
				if (UiState.MbeansDetection == uiState) {
					showProgressWindow(uiState.getDescription() + ".\n\n This may take a while ...");
					new DetectMBeansWorkerThread(((JmxConfigGeneratorUI) UI.getCurrent()).getUiModel().getServiceConfig()).start();
					return false;
				}
				if (UiState.ResultConfigGeneration == uiState) {
					showProgressWindow(uiState.getDescription() +  ".\n\n This may take a while ...");
					new CreateOutputWorkerThread().start();
					return false;
				}
				return true;
			}

			@Override
			public void afterViewChange(ViewChangeEvent event) {

			}
		});
	}

	private void initHeaderPanel() {
		headerPanel = new HeaderPanel();
	}

	// the Main panel holds all views such as Config view, mbeans view, etc.
	private void initContentPanel() {
		contentPanel = new Panel();
		contentPanel.setContent(new VerticalLayout());
		contentPanel.getContent().setSizeFull();
		contentPanel.setSizeFull();
	}

	/**
	 * Creates the main window and adds the header, main and button panels to
	 * it.
	 */
	private void initMainLayout() {
		VerticalLayout layout = new VerticalLayout();
		layout.addComponent(headerPanel);
		layout.addComponent(contentPanel);
		// content Panel should use most of the space
		layout.setExpandRatio(contentPanel, 1);
		setContent(layout);
	}

	public void updateView(UiState uiState) {
		navigator.navigateTo(uiState.name());
	}

	private ProgressWindow getProgressWindow() {
		if (progressWindow == null) {
			progressWindow = new ProgressWindow();
		}
		return progressWindow;
	}

	private void showProgressWindow(String label) {
		getProgressWindow().setLabelText(label);
		addWindow(getProgressWindow());
	}

	private void setRawModel(JmxDatacollectionConfig newModel) {
		model.setRawModel(newModel);
	}

	private class DetectMBeansWorkerThread extends Thread {

		private ServiceConfig config;

		private DetectMBeansWorkerThread(ServiceConfig config)  {
			this.config = config;
		}

		@Override
        public void run() {
            try {
                // TODO loading of the dictionary should not be done via the Starter class and not in a static way!
                JmxDatacollectionConfiggenerator jmxConfigGenerator = new JmxDatacollectionConfiggenerator();
                JMXServiceURL jmxServiceURL = jmxConfigGenerator.getJmxServiceURL(config.isJmxmp(), config.getHost(), config.getPort());
                JMXConnector connector = jmxConfigGenerator.getJmxConnector(config.getUser(), config.getPassword(), jmxServiceURL);
                JmxDatacollectionConfig generateJmxConfigModel = jmxConfigGenerator.generateJmxConfigModel(connector.getMBeanServerConnection(), "anyservice", !config.isSkipDefaultVM(), config.isRunWritableMBeans(), Starter.loadInternalDictionary());
                connector.close();

				UI.getCurrent().access(new Runnable() {
					@Override
					public void run() {
						setRawModel(generateJmxConfigModel);
						updateView(UiState.MbeansView);
					}
				});
			} catch (Exception ex) {
			 	// I know this is bad, but we have to catch
			 	// ALL Exceptions, otherwise we are in "please wait" mode forever
				handleError(ex);
			}
        }

        private void handleError(final Exception ex) {
			UI.getCurrent().access(new Runnable() {
				@Override
				public void run() {
					LOG.error("Error while retrieving MBeans from server", ex);
					UIHelper.showNotification("Connection error", "An error occured during connection. Please verify connection settings.<br/><br/>" + ex.getMessage(), Type.ERROR_MESSAGE);
					removeWindow(getProgressWindow());
				}
			});
        }
    }

	private class CreateOutputWorkerThread extends Thread {

		@Override
		public void run() {
			try {
				// create snmp-graph.properties
				GraphConfigGenerator graphConfigGenerator = new GraphConfigGenerator();
				Collection<Report> reports = new JmxConfigReader().generateReportsByJmxDatacollectionConfig(model.getOutputConfig());
				model.setSnmpGraphProperties(graphConfigGenerator.generateSnmpGraph(reports));
			} catch (IOException ex) {
				handleError(ex);
			}

			model.updateOutput();

			UI.getCurrent().access(new Runnable() {
				@Override
				public void run() {
					updateView(UiState.ResultView);
				}
			});
		}


		private void handleError(final Exception ex) {
			UI.getCurrent().access(new Runnable() {
				@Override
				public void run() {
					LOG.error("SNMP Graph-Properties couldn't be created.", ex);
					UIHelper.showNotification("Error", ex.getMessage(), Type.ERROR_MESSAGE);
					removeWindow(getProgressWindow());
				}
			});
		}

	}

	public UiModel getUiModel() {
		return model;
	}
}
