/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.camel.component.zeromq;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.ManagementAware;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Map;

@ManagedResource(description = "Managed Native Endpoint")
public class ZeroMQEndpoint extends DefaultEndpoint implements ManagementAware<ZeroMQEndpoint> {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQEndpoint.class);

    private final String zeroMQURI;
    private final Map zeroMQProperties;

    private ZeroMQProducer producer;
    private ZeroMQConsumer consumer;

    private int concurrentConsumers = 1;

    @ManagedAttribute
    public final int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public final void setConcurrentConsumer(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public final String getZeroMQURI() {
        return zeroMQURI;
    }

    public final Map getZeroMQProperties() {
        return zeroMQProperties;
    }

    public ZeroMQEndpoint(String endpointUri, String remaining, Map parameters, CamelContext context) {
        super(endpointUri, context);
        LOG.trace("Begin ZeroMQEndpoint.ZeroMQEndpoint");
        this.zeroMQURI = remaining;
        this.zeroMQProperties = parameters;
        LOG.trace("End ZeroMQEndpoint.ZeroMQEndpoint");
    }

    public final Consumer createConsumer(Processor processor) {
        try {
            LOG.trace("Begin ZeroMQEndpoint.createConsumer");
            consumer = new ZeroMQConsumer(this, processor);
            return consumer;
        }
        catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        }
        finally {
            LOG.trace("End ZeroMQEndpoint.createConsumer");
        }
    }

    public final Producer createProducer() {
        try {
            LOG.trace("Begin ZeroMQEndpoint.createProducer");
            producer = new ZeroMQProducer(this);
            return producer;
        }
        catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        }
        finally {
            LOG.trace("End ZeroMQEndpoint.createProducer");
        }
    }

    @Override
    public final boolean isLenientProperties() {
        return true;
    }

    @ManagedAttribute
    public final boolean isSingleton() {
        return true;
    }


    public final ZeroMQConsumer getConsumer() {
        return consumer;
    }

    public final ZeroMQProducer getProducer() {
        return producer;
    }

    public final Object getManagedObject(ZeroMQEndpoint endpoint) {
        return this;
    }

    @ManagedAttribute(description = "Camel id")
    public final String getCamelId() {
        return getCamelContext().getName();
    }

}
