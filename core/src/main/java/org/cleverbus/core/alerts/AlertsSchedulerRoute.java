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

import org.cleverbus.api.route.AbstractBasicRoute;
import org.cleverbus.api.route.CamelConfiguration;
import org.cleverbus.core.common.asynch.repair.RepairProcessingMsgRoute;

import org.apache.camel.spring.SpringRouteBuilder;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;


/**
 * Route definition that starts checking alerts by scheduler.
 *
 * @author <a href="mailto:petr.juza@cleverlance.com">Petr Juza</a>
 * @since 0.4
 */
@CamelConfiguration(value = AlertsSchedulerRoute.ROUTE_BEAN)
@Profile("prod")
public class AlertsSchedulerRoute extends SpringRouteBuilder {

    public static final String ROUTE_BEAN = "alertsSchedulerRoute";

    private static final String JOB_NAME = "alerts";

    /**
     * How often to run checking of alerts (in seconds).
     */
    @Value("${alerts.repeatTime}")
    private int repeatInterval;

    @Override
    public final void configure() throws Exception {
        String uri = RepairProcessingMsgRoute.JOB_GROUP_NAME + "/" + JOB_NAME
                + "?trigger.repeatInterval=" + (repeatInterval * 1000)
                + "&trigger.repeatCount=" + SimpleTrigger.REPEAT_INDEFINITELY;

        from("quartz2://" + uri)
                .routeId("alerts" + AbstractBasicRoute.ROUTE_SUFFIX)

                .beanRef(AlertsCheckingService.BEAN, "checkAlerts");
    }
}
