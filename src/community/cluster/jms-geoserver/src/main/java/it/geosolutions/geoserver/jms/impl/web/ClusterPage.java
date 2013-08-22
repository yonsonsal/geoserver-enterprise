/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.client.JMSContainer;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ToggleConfiguration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.events.ToggleType;
import it.geosolutions.geoserver.jms.impl.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.impl.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.impl.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import it.geosolutions.geoserver.jms.impl.configuration.ReadOnlyConfiguration;
import it.geosolutions.geoserver.jms.impl.configuration.TopicConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.ReadOnlyGeoServerLoader;
import org.geoserver.web.GeoServerSecuredPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

    protected JMSConfiguration getConfig() {
        return getGeoServerApplication().getBeanOfType(JMSConfiguration.class);
    }

    protected JMSContainer getJMSContainer() {
        return getGeoServerApplication().getBeanOfType(JMSContainer.class);
    }

    protected ReadOnlyGeoServerLoader getReadOnlyGeoServerLoader() {
        return getGeoServerApplication().getBeanOfType(ReadOnlyGeoServerLoader.class);
    }

    protected JMSContainerHandlerExceptionListenerImpl getJMSContainerExceptionHandler() {
        return getGeoServerApplication().getBeanOfType(
                JMSContainerHandlerExceptionListenerImpl.class);
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterPage.class);

    private Model<String> getConnectionModel() {
        final JMSContainer c = getJMSContainer();
        final FeedbackPanel fp = getFeedbackPanel();
        if (c.isRunning() && c.isRegisteredWithDestination()) {
            fp.info("GeoServer is connected and registered to the topic destination");
            return new Model<String>("Registered");
        } else {
            fp.error("Impossible to register GeoServer to destination, please check the broker.");
            return new Model<String>("Not registered");
        }
    }

    private Model<String> getReadOnlyModel() {
        final ReadOnlyGeoServerLoader l = getReadOnlyGeoServerLoader();
        // final FeedbackPanel fp = getFeedbackPanel();

        if (l.isEnabled()) {
            // fp.info("GeoServer is connected and registered to the topic destination");
            return new Model<String>("Enabled");
        } else {
            // fp.error("Impossible to register GeoServer to destination, please check the broker.");
            return new Model<String>("Disabled");
        }
    }

    public ClusterPage() {

        final FeedbackPanel fp = getFeedbackPanel();

        // setup the JMSContainer exception handler
        getJMSContainerExceptionHandler().setFeedbackPanel(fp);
        getJMSContainerExceptionHandler().setSession(fp.getSession());

        fp.setOutputMarkupId(true);

        // form and submit
        final Form<Properties> form = new Form<Properties>("form",
                new CompoundPropertyModel<Properties>(getConfig().getConfigurations()));

        // add broker URL setting
        final TextField<String> brokerURL = new TextField<String>(
                BrokerConfiguration.BROKER_URL_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        brokerURL.setType(String.class);
        form.add(brokerURL);

        // add instance name setting
        final TextField<String> instanceName = new TextField<String>(
                JMSConfiguration.INSTANCE_NAME_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        instanceName.setType(String.class);
        form.add(instanceName);

        // add topic name setting
        final TextField<String> topicName = new TextField<String>(TopicConfiguration.TOPIC_NAME_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        topicName.setType(String.class);
        form.add(topicName);

        // add connection status info
        final TextField<String> connectionInfo = new TextField<String>(
                ConnectionConfiguration.CONNECTION_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        connectionInfo.setType(String.class);

        connectionInfo.setOutputMarkupId(true);
        connectionInfo.setOutputMarkupPlaceholderTag(true);
        connectionInfo.setEnabled(false);
        form.add(connectionInfo);

        final AjaxButton connection = new AjaxButton("connectionB", new StringResourceModel(
                ConnectionConfiguration.CONNECTION_KEY, this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                final JMSContainer c = getJMSContainer();

                final int max = 3;
                final long maxWait=2000;
                if (c.isRunning()) {
                    fp.info("Disconnecting...");
                    c.stop();
                    c.shutdown();
                    for (int rep = 1; rep <= max; ++rep) {
                        fp.info("Waiting for connection shutdown...(" + rep + "/" + max + ")");
                        try {
                            Thread.sleep(maxWait);
                        } catch (InterruptedException e) {
                            fp.warn(e.getLocalizedMessage());
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                        fp.info("Unregistering...");
                        if (!c.isRegisteredWithDestination()) {
                            fp.info("Succesfully un-registered from the destination topic");
                            fp.warn("You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
                            // connectionStatusModel.setObject("Not registered");
                            connectionInfo.getModel().setObject(ConnectionConfigurationStatus.disabled.toString());
                            break;
                        }
                    }
                } else {
                    fp.info("Connecting...");
                    c.start();
                    if (c.isRunning()) {
                        for (int repReg = 1; repReg <= max; ++repReg) {
                            fp.info("Checking for registration...(" + repReg + "/" + max + ")");
                            if (c.isRegisteredWithDestination()) {
                                fp.info("Now GeoServer is registered with the destination");
                                connectionInfo.getModel().setObject(ConnectionConfigurationStatus.enabled.toString());
                                break;
                            } else {
                                if (repReg > max) {
                                    fp.error("Abort registration");
                                    connectionInfo.getModel().setObject(ConnectionConfigurationStatus.disabled.toString());
                                } else {
                                    fp.warn("Impossible to register GeoServer with destination, waiting...");
                                }
                            }
                            try {
                                Thread.sleep(maxWait);
                            } catch (InterruptedException e) {
                                fp.warn(e.getLocalizedMessage());
                                LOGGER.error(e.getLocalizedMessage(), e);
                            }
                        }
                    } else {
                        fp.error("Impossible to start a connection to destination.");
                        connectionInfo.getModel().setObject(ConnectionConfigurationStatus.disabled.toString());
                        c.stop();
                        fp.info("Disconnected");
                    }
                }
                target.addComponent(connectionInfo);
                target.addComponent(fp);
            }

        };
        connection.setOutputMarkupId(true);
        connection.setOutputMarkupPlaceholderTag(true);
        form.add(connection);

        // add MASTER toggle
        addToggle(ToggleConfiguration.TOGGLE_MASTER_KEY, ToggleType.MASTER,
                ToggleConfiguration.TOGGLE_MASTER_KEY, "toggleMasterB", form, fp);

        // add SLAVE toggle
        addToggle(ToggleConfiguration.TOGGLE_SLAVE_KEY, ToggleType.SLAVE,
                ToggleConfiguration.TOGGLE_SLAVE_KEY, "toggleSlaveB", form, fp);

        // add Read Only switch
        final TextField<String> readOnlyInfo = new TextField<String>(
                ReadOnlyConfiguration.READ_ONLY_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        readOnlyInfo.setType(String.class);

        readOnlyInfo.setOutputMarkupId(true);
        readOnlyInfo.setOutputMarkupPlaceholderTag(true);
        readOnlyInfo.setEnabled(false);
        form.add(readOnlyInfo);

        final AjaxButton readOnly = new AjaxButton("readOnlyB", new StringResourceModel(
                ReadOnlyConfiguration.READ_ONLY_KEY, this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                ReadOnlyGeoServerLoader loader = getReadOnlyGeoServerLoader();
                if (loader.isEnabled()) {
                    readOnlyInfo.getModel().setObject("disabled");
                    loader.setEnabled(false);
                } else {
                    readOnlyInfo.getModel().setObject("enabled");
                    loader.setEnabled(true);
                }
                target.addComponent(this.getParent());
            }
        };
        form.add(readOnly);

        final Button save = new Button("saveB", new StringResourceModel("save", this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                try {
                    getConfig().storeTempConfig();
                    fp.info("Configuration saved");
                } catch (IOException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                    fp.error(e.getLocalizedMessage());
                }
            }
        };
        form.add(save);

        // TODO change status if it is changed due to reset
        // final Button reset = new Button("resetB", new StringResourceModel("reset", this, null)) {
        // @Override
        // public void onSubmit() {
        // try {
        // getConfig().loadTempConfig();
        // fp.info("Configuration reloaded");
        // } catch (FileNotFoundException e) {
        // LOGGER.error(e.getLocalizedMessage(), e);
        // fp.error(e.getLocalizedMessage());
        // } catch (IOException e) {
        // LOGGER.error(e.getLocalizedMessage(), e);
        // fp.error(e.getLocalizedMessage());
        // }
        // }
        // };
        // form.add(reset);

        // add the form
        add(form);

        // add the status monitor
        add(fp);

    }

    // final JMSConfiguration config,
    private void addToggle(final String configKey, final ToggleType type, final String textFieldId,
            final String buttonId, final Form<?> form, final FeedbackPanel fp) {
        // final String producerStatusString = getConfig().getConfiguration(configKey);
        // final Model<String> producerStatusModel;
        // if (producerStatusString != null
        // && Boolean.parseBoolean(null)) { // TODO
        // producerStatusModel = new Model<String>(producerStatusString);
        // } else {
        // producerStatusModel = new Model<String>("true");
        // }

        final TextField<String> toggleInfo = new TextField<String>(textFieldId);

        // https://issues.apache.org/jira/browse/WICKET-2426
        toggleInfo.setType(String.class);

        toggleInfo.setOutputMarkupId(true);
        toggleInfo.setOutputMarkupPlaceholderTag(true);
        toggleInfo.setEnabled(false);
        form.add(toggleInfo);

        final AjaxButton toggle = new AjaxButton(buttonId) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                fp.error("ERROR");

                target.addComponent(fp);
            };

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                final boolean switchTo = !Boolean.parseBoolean(toggleInfo.getModel().getObject());
                final ApplicationContext ctx = getGeoServerApplication().getApplicationContext();
                ctx.publishEvent(new ToggleEvent(switchTo, type));
                // getConfig().putConfiguration(configKey,
                // Boolean.toString(switchTo));
                if (switchTo) {
                    fp.info("The " + type + " toggle is now ENABLED");
                } else {
                    fp.warn("The " + type
                            + " toggle is now DISABLED no event will be posted to the broker");
                    fp.info("Note that the " + type
                            + " is still registered to the topic destination");
                }
                toggleInfo.getModel().setObject(Boolean.toString(switchTo));
                target.addComponent(toggleInfo);
                target.addComponent(fp);

            }
        };
        toggle.setRenderBodyOnly(false);

        form.add(toggle);

        // add(new Monitor(Duration.seconds(10)));
    }

    // private static class Monitor extends AjaxSelfUpdatingTimerBehavior {
    //
    //
    // public Monitor(Duration updateInterval) {
    // super(updateInterval);
    // }
    //
    // }
}
