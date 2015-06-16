package org.opennms.features.vaadin.jmxconfiggenerator.jobs;

import org.opennms.features.jmxconfiggenerator.Starter;
import org.opennms.features.jmxconfiggenerator.jmxconfig.JmxDatacollectionConfiggenerator;
import org.opennms.features.vaadin.jmxconfiggenerator.data.ServiceConfig;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UIHelper;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.UiState;
import org.opennms.xmlns.xsd.config.jmx_datacollection.JmxDatacollectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Runnable to query the mbeanserver.
 */
public class DetectMBeansJob implements JobManager.Task<JmxDatacollectionConfig> {

    private final ServiceConfig config;

    public DetectMBeansJob(ServiceConfig config) {
        this.config = config;
    }


    @Override
	public JmxDatacollectionConfig execute() throws JobManager.TaskRunException {
        try {
            JmxDatacollectionConfiggenerator jmxConfigGenerator = new JmxDatacollectionConfiggenerator();
            JMXServiceURL jmxServiceURL = jmxConfigGenerator.getJmxServiceURL(config.isJmxmp(), config.getHost(), config.getPort());

            try (JMXConnector connector = jmxConfigGenerator.getJmxConnector(config.getUser(), config.getPassword(), jmxServiceURL)) {
                final JmxDatacollectionConfig generatedJmxConfigModel = jmxConfigGenerator.generateJmxConfigModel(connector.getMBeanServerConnection(), "anyservice", !config.isSkipDefaultVM(), config.isRunWritableMBeans(), Starter.loadInternalDictionary());
                try { // TODO MVR...
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new JobManager.TaskRunException("Operation cancelled by user", e);
                }
                return generatedJmxConfigModel;

            } catch (IOException e) {
                //UIHelper.showNotifivation("Connection error", "An error occurred during connection. Please verify connection settings.<br/><br/>" + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                throw new JobManager.TaskRunException("Error while retrieving MBeans from server.", e);
            }
        } catch (MalformedURLException e) {
            throw new JobManager.TaskRunException(
                    String.format("Cannot create valid JMX Connection URL. Host = '%s', Porst = '%s', use jmxmp = %s", config.getHost(), config.getPort(), config.isJmxmp()),
                    e);
        }
	}

//	private void handleError(final Exception ex) {
//        UI.getCurrent().access(new Runnable() {
//            @Override
//            public void run() {
//                LOG.error("Error while retrieving MBeans from server", ex);
//                UIHelper.showNotification("Connection error", "An error occurred during connection. Please verify connection settings.<br/><br/>" + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
//                UIHelper.getCurrent().hideProgressWindow();
//            }
//        });
//	}

    @Override
    public void onSuccess(JmxDatacollectionConfig generatedJmxConfigModel) {
        UIHelper.getCurrent().setRawModel(generatedJmxConfigModel);
        UIHelper.getCurrent().updateView(UiState.MbeansView);
    }

    @Override
    public void onError() {

    }
}
