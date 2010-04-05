/*
 *  Copyright 2009 dgreco.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.camel.component.zeromq;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

public final class ZeroMQConsumer extends DefaultConsumer {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQConsumer.class);

    private ZeroMQConsumerSupport zeroMQConsumerSupport;

    private PollingThread pollingThread;

    public ZeroMQConsumer(DefaultEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    public ZeroMQSupport getZeroMQSupport() {
        return zeroMQConsumerSupport;
    }

    @Override
    protected void doStart() {
        try {
            LOG.trace("Begin ZeroMQConsumer.doStart");
            Properties params = new Properties();
            for (Object obj : ((ZeroMQEndpoint) getEndpoint()).getZeroMQProperties().entrySet()) {
                Map.Entry e = (Map.Entry) obj;
                params.set((String) e.getKey(), (String) e.getValue());
            }
            zeroMQConsumerSupport = new ZeroMQConsumerSupport();
            zeroMQConsumerSupport.start(((ZeroMQEndpoint) getEndpoint()).getZeroMQURI(), params);
            pollingThread = new PollingThread(getEndpoint(), getProcessor(), zeroMQConsumerSupport);
            pollingThread.start();
            super.doStart();
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQConsumer.doStart");
        }
    }

    @Override
    protected void doStop() {
        try {
            LOG.trace("Begin ZeroMQConsumer.doStop");
            zeroMQConsumerSupport.stop();
            pollingThread.end();
            pollingThread.join();           
            super.doStop();
        } catch (InterruptedException ex) {

        } catch (Exception ex) {
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQConsumer.doStop");
        }
    }

}

class PollingThread extends Thread {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQConsumer.class);

    private volatile boolean stop = false;

    private final Endpoint endpoint;
    private final Processor processor;
    private final ZeroMQConsumerSupport zeroMQConsumerSupport;

    PollingThread(Endpoint endpoint, Processor processor, ZeroMQConsumerSupport zeroMQConsumerSupport) {
        setDaemon(true);
        this.endpoint = endpoint;
        this.processor = processor;
        this.zeroMQConsumerSupport = zeroMQConsumerSupport;
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                int size = zeroMQConsumerSupport.waitForMessage();
                if (size != -1) {
                    byte[] buffer = new byte[size];
                    zeroMQConsumerSupport.copy(buffer, size);
                    Exchange exchange = endpoint.createExchange();
                    Message message = new DefaultMessage();
                    message.setBody(buffer);
                    exchange.setIn(message);
                    try {
                        processor.process(exchange);
                    } catch (Exception ex) {
                        LOG.fatal(ex, ex);
                        throw new RuntimeCamelException(ex);
                    }
                }
            }
        }
        catch (Exception ex) {

        }

    }

    public void end() {
        stop = true;
    }
}
