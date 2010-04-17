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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ZeroMQComponentTest extends CamelTestSupport {

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testProducerConsumer() throws Exception {

        class TestRouteBuilder extends RouteBuilder {

            private long start;

            private long stop;

            private final CountDownLatch countDownLatch;

            private final int size;

            public TestRouteBuilder(CountDownLatch countDownLatch, int size) {
                this.countDownLatch = countDownLatch;
                this.size = size;
            }

            @Override
            public void configure() throws Exception {
                from("zeromq:tcp://127.0.0.1:8000?concurrentConsumers=1").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        if (countDownLatch.getCount() == size) {
                            start = System.currentTimeMillis();
                        }
                        byte[] buffer = (byte[]) exchange.getIn().getBody();
                        countDownLatch.countDown();
                        if (countDownLatch.getCount() == 0) {
                            stop = System.currentTimeMillis();
                        }
                    }
                });
            }

            public long getStart() {
                return start;
            }

            public long getStop() {
                return stop;
            }
        }

        int size = 100000;

        CountDownLatch count = new CountDownLatch(size);

        TestRouteBuilder builder = new TestRouteBuilder(count, size);

        context.addRoutes(builder);

        context.start();

        for (int i = 0; i < size; ++i) {
            this.template.sendBody("zeromq:tcp://127.0.0.1:8000?concurrentConsumers=4", new byte[1024]);
        }

        count.await();

        System.out.println("done. " + (builder.getStop() - builder.getStart()));
        context.stop();
    }


    @Test
    public void testProducerConsumerWithNetty() throws Exception {

        int size = 100000;

        final CountDownLatch count = new CountDownLatch(size);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("netty:tcp://127.0.0.1:5155").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        byte[] buffer = (byte[]) exchange.getIn().getBody();
                        count.countDown();
                    }
                });
            }
        });
        context.start();

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        for (int i = 0; i < size; ++i) {
            this.template.sendBody("netty:tcp://127.0.0.1:5155", new byte[64]);
        }

        count.await();
        long stop = System.currentTimeMillis();

        System.out.println("done. " + (stop - start));
        context.stop();
    }

}