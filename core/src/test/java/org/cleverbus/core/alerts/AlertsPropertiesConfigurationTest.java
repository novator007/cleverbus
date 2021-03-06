/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cleverbus.core.alerts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.cleverbus.spi.alerts.AlertInfo;

import org.junit.Test;


/**
 * Test suite for {@link AlertsPropertiesConfiguration}.
 *
 * @author <a href="mailto:petr.juza@cleverlance.com">Petr Juza</a>
 * @since 0.4
 */
public class AlertsPropertiesConfigurationTest {

    @Test
    public void testConf() {
        // prepare properties
        Properties props = new Properties();

        // add alert (min. version)
        String prefix = AlertsPropertiesConfiguration.ALERT_PROP_PREFIX + "1.";
        props.put(prefix + AlertsPropertiesConfiguration.ID_PROP, "ID");
        props.put(prefix + AlertsPropertiesConfiguration.LIMIT_PROP, "11");
        props.put(prefix + AlertsPropertiesConfiguration.SQL_PROP, "select COUNT()");

        // add alert (min. version)
        prefix = AlertsPropertiesConfiguration.ALERT_PROP_PREFIX + "66.";
        props.put(prefix + AlertsPropertiesConfiguration.ID_PROP, "ID2");
        props.put(prefix + AlertsPropertiesConfiguration.LIMIT_PROP, "11");
        props.put(prefix + AlertsPropertiesConfiguration.SQL_PROP, "select COUNT()");
        props.put(prefix + AlertsPropertiesConfiguration.ENABLED_PROP, "true");
        props.put(prefix + AlertsPropertiesConfiguration.MAIL_SBJ_PROP, "subject");
        props.put(prefix + AlertsPropertiesConfiguration.MAIL_BODY_PROP, "body");

        // create configuration
        AlertsPropertiesConfiguration conf = new AlertsPropertiesConfiguration(props);

        // verify
        assertThat(conf.getAlert("ID"), notNullValue());
        assertThat(conf.getAlert("ID2"), notNullValue());

        AlertInfo alert1 = conf.getAlert("ID");
        assertThat(alert1.isEnabled(), is(true));
        assertThat(alert1.getSql(), is("select COUNT()"));
        assertThat(alert1.getLimit(), is(11L));
        assertThat(alert1.getNotificationSubject(), nullValue());
        assertThat(alert1.getNotificationBody(), nullValue());

        AlertInfo alert2 = conf.getAlert("ID2");
        assertThat(alert2.isEnabled(), is(true));
        assertThat(alert2.getSql(), is("select COUNT()"));
        assertThat(alert2.getLimit(), is(11L));
        assertThat(alert2.getNotificationSubject(), is("subject"));
        assertThat(alert2.getNotificationBody(), is("body"));
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateId() {
        Properties props = new Properties();

        // add alert (min. version)
        String prefix = AlertsPropertiesConfiguration.ALERT_PROP_PREFIX + "1.";
        props.put(prefix + AlertsPropertiesConfiguration.ID_PROP, "ID");
        props.put(prefix + AlertsPropertiesConfiguration.ID_PROP, "ID");

        new AlertsPropertiesConfiguration(props);
    }
}
