/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.zeromq;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

public class ZeroMQProducer extends DefaultProducer {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQProducer.class);

    private ZeroMQProducerSupport zeroMQProducerSupport;

    public ZeroMQProducer(ZeroMQEndpoint endpoint) {
        super(endpoint);
        try {
            LOG.trace("Begin ZeroMQProducer.ZeroMQProducer");
        } finally {
            LOG.trace("End ZeroMQProducer.ZeroMQProducer");
        }
    }

    @Override
    protected final void doStart() {
        try {
            LOG.trace("Begin ZeroMQProducer.doStart");
            Properties params = new Properties();
            for (Object obj : ((ZeroMQEndpoint) getEndpoint()).getZeroMQProperties().entrySet()) {
                Map.Entry e = (Map.Entry) obj;
                params.set((String) e.getKey(), (String) e.getValue());
            }
            zeroMQProducerSupport = new ZeroMQProducerSupport();
            zeroMQProducerSupport.start(((ZeroMQEndpoint) getEndpoint()).getZeroMQURI(), params);

            super.doStart();
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQProducer.doStart");
        }
    }

    protected final void doStop() {
        try {
            LOG.trace("Begin ZeroMQProducer.doStop");
            zeroMQProducerSupport.stop();
        } catch (Exception ex) {
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQProducer.doStop");
        }
    }

    public final void process(Exchange exchange) {
        try {
            LOG.trace("Begin ZeroMQProducer.process");
            byte[] body = exchange.getIn().getBody(byte[].class);
            if (body == null) {
                LOG.warn("No payload for exchange: " + exchange);
            } else {
                zeroMQProducerSupport.send(body, body.length);
            }
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQProducer.process");
        }

    }

}

