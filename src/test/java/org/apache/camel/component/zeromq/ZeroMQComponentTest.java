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
import org.apache.camel.TypeConverter;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;
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
                            System.out.println("start");
                        }
                        byte[] buffer = (byte[]) exchange.getIn().getBody();
                        countDownLatch.countDown();
                        if (countDownLatch.getCount() == 0) {
                            stop = System.currentTimeMillis();
                            System.out.println("stop");
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
            this.template.sendBody("zeromq:tcp://127.0.0.1:8000", new byte[1024]);
        }

        count.await();

        System.out.println("done. " + (builder.getStop() - builder.getStart()));
        context.stop();
    }

    @Test
    public void testProducerConsumerWithObject() throws Exception {

        class ByteArrayToEventTypeConverter implements TypeConverter {

            public <T> T convertTo(Class<T> type, Object value) {
                ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) value);
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(bis);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Event ev = null;
                try {
                    ev = (Event) ois.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return (T) ev;
            }

            public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
                return convertTo(type, value);
            }

            public <T> T mandatoryConvertTo(Class<T> type, Object value) {
                return convertTo(type, value);
            }

            public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) {
                return convertTo(type, value);
            }
        }

        class EventToByteArrayTypeConverter implements TypeConverter {

            public <T> T convertTo(Class<T> type, Object value) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(bos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    oos.writeObject(value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return (T) bos.toByteArray();
            }

            public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
                return convertTo(type, value);
            }

            public <T> T mandatoryConvertTo(Class<T> type, Object value) {
                return convertTo(type, value);
            }

            public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) {
                return convertTo(type, value);
            }
        }

        context.getTypeConverterRegistry().addTypeConverter(byte[].class, Event.class, new EventToByteArrayTypeConverter());
        context.getTypeConverterRegistry().addTypeConverter(Event.class, byte[].class, new ByteArrayToEventTypeConverter());

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
                            System.out.println("start");
                        }
                        Event ev = exchange.getIn().getBody(Event.class);
                        countDownLatch.countDown();
                        if (countDownLatch.getCount() == 0) {
                            stop = System.currentTimeMillis();
                            System.out.println("stop");
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
            Event ev = new Event();
            ev.setId(UUID.randomUUID().toString());
            ev.setWhen(new Timestamp(System.currentTimeMillis()));
            ev.setWhat("Event" + i);
            this.template.sendBody("zeromq:tcp://127.0.0.1:8000", ev);
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
            this.template.sendBody("netty:tcp://127.0.0.1:5155", new byte[1024]);
        }

        count.await();
        long stop = System.currentTimeMillis();

        System.out.println("done. " + (stop - start));
        context.stop();
    }

}

class Event implements Serializable {

    private String id;

    private Timestamp when;

    private String what;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getWhen() {
        return when;
    }

    public void setWhen(Timestamp when) {
        this.when = when;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }
}