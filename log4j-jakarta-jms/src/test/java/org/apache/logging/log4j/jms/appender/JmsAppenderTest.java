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
package org.apache.logging.log4j.jms.appender;

import java.util.Map;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.MapMessage;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.test.categories.Appenders;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.jndi.test.junit.JndiRule;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@Category(Appenders.Jms.class)
public class JmsAppenderTest {

    private static final String CONNECTION_FACTORY_NAME = "jms/connectionFactory";
    private static final String QUEUE_FACTORY_NAME = "jms/queues";
    private static final String TOPIC_FACTORY_NAME = "jms/topics";
    private static final String DESTINATION_NAME = "jms/destination";
    private static final String DESTINATION_NAME_ML = "jms/destination-ml";
    private static final String QUEUE_NAME = "jms/queue";
    private static final String TOPIC_NAME = "jms/topic";
    private static final String LOG_MESSAGE = "Hello, world!";

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private final Connection connection = mock(Connection.class);
    private final Session session = mock(Session.class);
    private final Destination destination = mock(Destination.class);
    private final Destination destinationMl = mock(Destination.class);
    private final MessageProducer messageProducer = mock(MessageProducer.class);
    private final MessageProducer messageProducerMl = mock(MessageProducer.class);
    private final TextMessage textMessage = mock(TextMessage.class);
    private final ObjectMessage objectMessage = mock(ObjectMessage.class);
    private final MapMessage mapMessage = mock(MapMessage.class);

    private final JndiRule jndiRule = new JndiRule(createBindings());
    private final LoggerContextRule ctx = new LoggerContextRule("src/test/resources/JmsAppenderTest.xml");

    @Rule
    public RuleChain rules = RuleChain.outerRule(jndiRule).around(ctx);

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(Log4jPropertyKey.JNDI_ENABLE_JMS.getSystemKey(), "true");
    }

    public JmsAppenderTest() throws Exception {
        // this needs to set up before LoggerContextRule
        given(connectionFactory.createConnection()).willReturn(connection);
        given(connectionFactory.createConnection(anyString(), anyString())).willThrow(IllegalArgumentException.class);
        given(connection.createSession(eq(false), eq(Session.AUTO_ACKNOWLEDGE))).willReturn(session);
        given(session.createProducer(eq(destination))).willReturn(messageProducer);
        given(session.createProducer(eq(destinationMl))).willReturn(messageProducerMl);
        given(session.createTextMessage(anyString())).willReturn(textMessage);
        given(session.createMapMessage()).willReturn(mapMessage);
    }

    private Map<String, Object> createBindings() {
        return Map.of(
                CONNECTION_FACTORY_NAME, connectionFactory,
                DESTINATION_NAME, destination,
                DESTINATION_NAME_ML, destinationMl,
                QUEUE_FACTORY_NAME, connectionFactory,
                QUEUE_NAME, destination,
                TOPIC_FACTORY_NAME, connectionFactory,
                TOPIC_NAME, destination
        );
    }

    private  Log4jLogEvent createLogEvent() {
        return createLogEvent(new SimpleMessage(LOG_MESSAGE));
    }

    private Log4jLogEvent createLogEvent(final Message message) {
        // @formatter:off
        return Log4jLogEvent.newBuilder()
            .setLoggerName(JmsAppenderTest.class.getName())
            .setLoggerFqcn(JmsAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(message)
            .build();
        // @formatter:on
    }

    private Log4jLogEvent createMapMessageLogEvent() {
        final StringMapMessage mapMessage = new StringMapMessage();
        return createLogEvent(mapMessage.with("testMesage", LOG_MESSAGE));
    }

    @Before
    public void setUp() throws Exception {
        // we have 4 appenders all connecting to the same ConnectionFactory
        then(connection).should(times(4)).start();
    }

    @Test
    public void testAppendToQueue() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender");
        final LogEvent event = createLogEvent();
        appender.append(event);
        then(session).should().createTextMessage(eq(LOG_MESSAGE));
        then(textMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(textMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testAppendToQueueWithMessageLayout() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender-MessageLayout");
        final LogEvent event = createMapMessageLogEvent();
        appender.append(event);
        then(session).should().createMapMessage();
        then(mapMessage).should().setJMSTimestamp(anyLong());
        then(messageProducerMl).should().send(mapMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testJmsQueueAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsQueueAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(session).should().createTextMessage(eq(expected.getMessage().getFormat()));
        then(textMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(textMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testJmsTopicAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsTopicAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(session).should().createTextMessage(eq(expected.getMessage().getFormat()));
        then(textMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(textMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

}