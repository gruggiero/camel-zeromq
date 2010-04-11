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

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

@SuppressWarnings("unchecked")
public class ZeroMQComponent extends DefaultComponent {

    private static final transient Log LOG = LogFactory.getLog(ZeroMQComponent.class);

    private static final String LIBNAME = "zeromq_support";

    public ZeroMQComponent() {
        LOG.trace("Begin ZeroMQComponent.ZeroMQComponent");
        try {
            try {
                NativeLibraryLoader.loadLibrary(LIBNAME);
            } catch (java.io.IOException e) {
                System.loadLibrary(LIBNAME);
            }
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
        } finally {
            LOG.trace("End ZeroMQComponent.ZeroMQComponent");
        }
    }

    public ZeroMQComponent(CamelContext context) {
        super(context);
        LOG.trace("Begin ZeroMQComponent.ZeroMQComponent");
        try {
            try {
                NativeLibraryLoader.loadLibrary(LIBNAME);
            } catch (java.io.IOException e) {
                System.loadLibrary(LIBNAME);
            }
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
        } finally {
            LOG.trace("End ZeroMQComponent.ZeroMQComponent");
        }
    }

    @Override
    protected final Endpoint createEndpoint(String uri, String remaining, Map parameters) {
        LOG.trace("Begin ZeroMQComponent.createEndpoint");
        try {
            return new ZeroMQEndpoint(uri, remaining, parameters, this.getCamelContext());
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("End ZeroMQComponent.createEndpoint");
        }
    }

    @Override
    public final void stop() {
        try {
            LOG.trace("Begin ZeroMQComponent.stop");
            super.stop();
        } catch (Exception ex) {
            LOG.fatal(ex, ex);
            throw new RuntimeCamelException(ex);
        } finally {
            LOG.trace("Begin ZeroMQComponent.stop");
        }
    }
}
