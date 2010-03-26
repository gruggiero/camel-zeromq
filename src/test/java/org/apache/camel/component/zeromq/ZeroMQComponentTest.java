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
    public void testConsumer() throws Exception {

        int size = 100000;

        final CountDownLatch count = new CountDownLatch(size);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("zeromq:tcp://lo0:8000?p1=v1&p2=v2").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        byte[] buffer = (byte[]) exchange.getIn().getBody();
                        //System.out.println(exchange.getIn().getBody(String.class));
                        count.countDown();
                    }
                });
            }
        });
        context.start();

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        for (int i = 0; i < size; ++i) {
            this.template.sendBody("zeromq:tcp://localhost:8000?p1=v1&p2=v2", "CIAO DAVID");
        }

        count.await();
        long stop = System.currentTimeMillis();

        System.out.println("done. " + (stop - start));
    }
}