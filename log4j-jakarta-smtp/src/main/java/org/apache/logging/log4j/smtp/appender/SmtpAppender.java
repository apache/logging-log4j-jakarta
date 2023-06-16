/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.smtp.appender;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.ValidPort;

/**
 * Send an e-mail when a specific logging event occurs, typically on errors or
 * fatal errors.
 *
 * <p>
 * The number of logging events delivered in this e-mail depend on the value of
 * <b>BufferSize</b> option. The <code>SmtpAppender</code> keeps only the last
 * <code>BufferSize</code> logging events in its cyclic buffer. This keeps
 * memory requirements at a reasonable level while still delivering useful
 * application context.
 *
 * By default, an email message will formatted as HTML. This can be modified by
 * setting a layout for the appender.
 *
 * By default, an email message will be sent when an ERROR or higher severity
 * message is appended. This can be modified by setting a filter for the
 * appender.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("SMTP")
public final class SmtpAppender extends AbstractAppender {

    private static final int DEFAULT_BUFFER_SIZE = 512;

    /** The SMTP Manager */
    private final SmtpManager manager;

    private SmtpAppender(final String name, final Filter filter, final Layout layout, final boolean ignoreExceptions,
                         Property[] properties, final SmtpManager manager) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
    }

    /**
     * @since 2.13.2
     */
    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<SmtpAppender> {
        @PluginAttribute
        private String to;

        @PluginAttribute
        private String cc;

        @PluginAttribute
        private String bcc;

        @PluginAttribute
        private String from;

        @PluginAttribute
        private String replyTo;

        @PluginAttribute
        private String subject;

        @PluginAttribute
        private String smtpProtocol = "smtp";

        @PluginAttribute
        private String smtpHost;

        @PluginAttribute
        @ValidPort
        private int smtpPort;

        @PluginAttribute
        private String smtpUsername;

        @PluginAttribute(sensitive = true)
        private String smtpPassword;

        @PluginAttribute
        private boolean smtpDebug;

        @PluginAttribute
        private int bufferSize = DEFAULT_BUFFER_SIZE;

        @PluginElement("SSL")
        private SslConfiguration sslConfiguration;

        /**
         * Comma-separated list of recipient email addresses.
         */
        public Builder setTo(final String to) {
            this.to = to;
            return this;
        }

        /**
         * Comma-separated list of CC email addresses.
         */
        public Builder setCc(final String cc) {
            this.cc = cc;
            return this;
        }

        /**
         * Comma-separated list of BCC email addresses.
         */
        public Builder setBcc(final String bcc) {
            this.bcc = bcc;
            return this;
        }

        /**
         * Email address of the sender.
         */
        public Builder setFrom(final String from) {
            this.from = from;
            return this;
        }

        /**
         * Comma-separated list of Reply-To email addresses.
         */
        public Builder setReplyTo(final String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        /**
         * Subject template for the email messages.
         * @see org.apache.logging.log4j.core.layout.PatternLayout
         */
        public Builder setSubject(final String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Transport protocol to use for SMTP such as "smtp" or "smtps". Defaults to "smtp".
         */
        public Builder setSmtpProtocol(final String smtpProtocol) {
            this.smtpProtocol = smtpProtocol;
            return this;
        }

        /**
         * Host name of SMTP server to send messages through.
         */
        public Builder setSmtpHost(final String smtpHost) {
            this.smtpHost = smtpHost;
            return this;
        }

        /**
         * Port number of SMTP server to send messages through.
         */
        public Builder setSmtpPort(final int smtpPort) {
            this.smtpPort = smtpPort;
            return this;
        }

        /**
         * Username to authenticate with SMTP server.
         */
        public Builder setSmtpUsername(final String smtpUsername) {
            this.smtpUsername = smtpUsername;
            return this;
        }

        /**
         * Password to authenticate with SMTP server.
         */
        public Builder setSmtpPassword(final String smtpPassword) {
            this.smtpPassword = smtpPassword;
            return this;
        }

        /**
         * Enables or disables mail session debugging on STDOUT. Disabled by default.
         */
        public Builder setSmtpDebug(final boolean smtpDebug) {
            this.smtpDebug = smtpDebug;
            return this;
        }

        /**
         * Number of log events to buffer before sending an email. Defaults to {@value #DEFAULT_BUFFER_SIZE}.
         */
        public Builder setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Specifies an SSL configuration for smtps connections.
         */
        public Builder setSslConfiguration(final SslConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            return this;
        }

        /**
         * Specifies the layout used for the email message body. By default, this uses the
         * {@linkplain HtmlLayout#createDefaultLayout() default HTML layout}.
         */
        @Override
        public Builder setLayout(final Layout layout) {
            return super.setLayout(layout);
        }

        /**
         * Specifies the filter used for this appender. By default, uses a {@link ThresholdFilter} with a level of
         * ERROR.
         */
        @Override
        public Builder setFilter(final Filter filter) {
            return super.setFilter(filter);
        }

        @Override
        public SmtpAppender build() {
            if (getLayout() == null) {
                setLayout(HtmlLayout.createDefaultLayout());
            }
            if (getFilter() == null) {
                setFilter(ThresholdFilter.createFilter(null, null, null));
            }
            final SmtpManager smtpManager = SmtpManager.getSmtpManager(getConfiguration(), to, cc, bcc, from, replyTo,
                    subject, smtpProtocol, smtpHost, smtpPort, smtpUsername, smtpPassword, smtpDebug,
                    getFilter().toString(), bufferSize, sslConfiguration);
            return new SmtpAppender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), smtpManager);
        }
    }

    /**
     * @since 2.13.2
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a SmtpAppender.
     * @deprecated Use {@link #newBuilder()} to create and configure a {@link Builder} instance.
     * @see Builder
     */
    public static SmtpAppender createAppender(final Configuration config, final String name, final String to,
                                              final String cc, final String bcc, final String from,
                                              final String replyTo, final String subject, final String smtpProtocol,
                                              final String smtpHost, final String smtpPortStr,
                                              final String smtpUsername, final String smtpPassword,
                                              final String smtpDebug, final String bufferSizeStr,
                                              Layout layout, Filter filter,
                                              final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for SmtpAppender");
            return null;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final int smtpPort = AbstractAppender.parseInt(smtpPortStr, 0);
        final boolean isSmtpDebug = Boolean.parseBoolean(smtpDebug);
        final int bufferSize = bufferSizeStr == null ? DEFAULT_BUFFER_SIZE : Integer.parseInt(bufferSizeStr);

        if (layout == null) {
            layout = HtmlLayout.createDefaultLayout();
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter(null, null, null);
        }
        final Configuration configuration = config != null ? config : new DefaultConfiguration();

        final SmtpManager manager = SmtpManager.getSmtpManager(configuration, to, cc, bcc, from, replyTo, subject, smtpProtocol,
            smtpHost, smtpPort, smtpUsername, smtpPassword, isSmtpDebug, filter.toString(),  bufferSize, null);
        if (manager == null) {
            return null;
        }

        return new SmtpAppender(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY, manager);
    }

    /**
     * Capture all events in CyclicBuffer.
     * @param event The Log event.
     * @return true if the event should be filtered.
     */
    @Override
    public boolean isFiltered(final LogEvent event) {
        final boolean filtered = super.isFiltered(event);
        if (filtered) {
            manager.add(event);
        }
        return filtered;
    }

    /**
     * Perform SmtpAppender specific appending actions, mainly adding the event
     * to a cyclic buffer and checking if the event triggers an e-mail to be
     * sent.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {
        manager.sendEvents(getLayout(), event);
    }
}
