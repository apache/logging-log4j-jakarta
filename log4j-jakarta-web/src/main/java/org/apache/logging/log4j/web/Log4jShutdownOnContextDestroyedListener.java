/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.web;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public class Log4jShutdownOnContextDestroyedListener implements ServletContextListener {

    private static final int DEFAULT_STOP_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_STOP_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;

    private static final String KEY_STOP_TIMEOUT = "log4j.stop.timeout";
    private static final String KEY_STOP_TIMEOUT_TIMEUNIT = "log4j.stop.timeout.timeunit";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        LOGGER.debug(Log4jShutdownOnContextDestroyedListener.class.getSimpleName() + 
        		" ensuring that Log4j started up properly.");
        servletContext = event.getServletContext();
        if (null == servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)) {
        	throw new IllegalStateException(
        			"Context did not contain required Log4jWebLifeCycle in the " 
        			+ Log4jWebSupport.SUPPORT_ATTRIBUTE + " attribute.");
        }
        this.initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (this.servletContext == null || this.initializer == null) {
            LOGGER.warn("Context destroyed before it was initialized.");
            return;
        }
        LOGGER.debug(Log4jShutdownOnContextDestroyedListener.class.getSimpleName() +
        		" ensuring that Log4j shuts down properly.");

        this.initializer.clearLoggerContext(); // the application is finished
        // shutting down now
        if (initializer instanceof LifeCycle2) {
            final String stopTimeoutStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT);
            final long stopTimeout = Strings.isEmpty(stopTimeoutStr) ? DEFAULT_STOP_TIMEOUT
                    : Long.parseLong(stopTimeoutStr);
            final String timeoutTimeUnitStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT_TIMEUNIT);
            final TimeUnit timeoutTimeUnit = Strings.isEmpty(timeoutTimeUnitStr) ? DEFAULT_STOP_TIMEOUT_TIMEUNIT
                    : TimeUnit.valueOf(timeoutTimeUnitStr.toUpperCase(Locale.ROOT));
            ((LifeCycle2) this.initializer).stop(stopTimeout, timeoutTimeUnit);
        } else {
            this.initializer.stop();
        }
    }
}
