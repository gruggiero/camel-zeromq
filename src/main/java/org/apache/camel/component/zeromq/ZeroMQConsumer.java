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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ZeroMQConsumer extends DefaultConsumer {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQConsumer.class);

    private final int concurrentConsumers;
    private final Task[] tasks;
    private final ScheduledThreadPoolExecutor executor;

    public ZeroMQConsumer(DefaultEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.concurrentConsumers = ((ZeroMQEndpoint) endpoint).getConcurrentConsumers();
        this.tasks = new Task[concurrentConsumers];
        this.executor = new ScheduledThreadPoolExecutor(concurrentConsumers);
    }

    @Override
    protected void doStart() {
        try {
            LOG.trace("Begin ZeroMQConsumer.doStart");
            super.doStart();
            for (int i = 0; i < concurrentConsumers; ++i) {
                tasks[i] = new Task(getEndpoint(), getProcessor());
                executor.scheduleWithFixedDelay(tasks[i], 0, 1, TimeUnit.NANOSECONDS);
            }
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
            for (int i = 0; i < concurrentConsumers; ++i) {
                tasks[i].end();
            }
            executor.shutdown();
            super.doStop();
        } catch (InterruptedException ex) {

        } catch (Exception ex) {
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQConsumer.doStop");
        }
    }

}

class Task implements Runnable {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQConsumer.class);

    private volatile boolean stop = false;

    private final Endpoint endpoint;
    private final Processor processor;
    private final ZeroMQConsumerSupport zeroMQConsumerSupport;

    Task(Endpoint endpoint, Processor processor) {
        this.endpoint = endpoint;
        this.processor = processor;
        this.zeroMQConsumerSupport = new ZeroMQConsumerSupport();
        Properties params = new Properties();
        for (Object obj : ((ZeroMQEndpoint) endpoint).getZeroMQProperties().entrySet()) {
            Map.Entry e = (Map.Entry) obj;
            params.set((String) e.getKey(), (String) e.getValue());
        }
        zeroMQConsumerSupport.start(((ZeroMQEndpoint) endpoint).getZeroMQURI(), params);
    }

    public void run() {
        try {
            while(!stop) {
                int size = zeroMQConsumerSupport.receive();
                if (size != -1) {
                    byte[] buffer = new byte[size];
                    zeroMQConsumerSupport.copy(buffer, size);
                    Exchange exchange = endpoint.createExchange();
                    Message message = new DefaultMessage();
                    message.setBody(buffer);
                    exchange.setIn(message);
                    try {
                        processor.process(exchange);
                    } catch (InterruptedException ex) {
                        stop = true;
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
        zeroMQConsumerSupport.stop();
        stop = true;
    }
}
