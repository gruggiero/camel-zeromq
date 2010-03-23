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

import org.apache.camel.util.IOHelper;

import java.io.*;

public final class NativeLibraryLoader {

    private NativeLibraryLoader() {
    }

    public static void loadLibrary(String libname) throws IOException {
        String actualLibName = System.mapLibraryName(libname);
        File lib = extractResource(actualLibName);
        System.load(lib.getAbsolutePath());
    }

    static File extractResource(String resourcename) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcename);
        if (in == null) {
            throw new IOException("Unable to find library " + resourcename + " on classpath");
        }
        File tmpDir = new File(System.getProperty("java.tmpdir", "tmplib"));
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new IOException("Unable to create JNI library working directory " + tmpDir);
        }
        File outfile = new File(tmpDir, resourcename);
        OutputStream out = new FileOutputStream(outfile);
        try {
            IOHelper.copy(in, out);
        } finally {
            out.close();
            in.close();
        }
        return outfile;
    }

}