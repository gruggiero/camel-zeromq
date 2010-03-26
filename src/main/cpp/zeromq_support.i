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
%module zeromq_support

%{
#include "zeromq_support.h"
%}

%include exception.i
%include "std_string.i"
%include "std_map.i"
%include "various.i"

namespace std {
    %template(Properties) map<string, string>; 
};

%exception {
	try {
		$action
	} catch(const std::runtime_error& e) {
		SWIG_exception(SWIG_RuntimeError, e.what());
	} catch(...) {
		SWIG_exception(SWIG_RuntimeError,"Unknown exception");
	}
}

%typemap(in) (void *buffer, long size) {
  /* %typemap(in) void * */
  $1 = jenv->GetDirectBufferAddress($input);
  $2 = (long)(jenv->GetDirectBufferCapacity($input));
}

/* These 3 typemaps tell SWIG what JNI and Java types to use */
%typemap(jni) (void *buffer, long size) "jobject"
%typemap(jtype) (void *buffer, long size) "java.nio.ByteBuffer"
%typemap(jstype) (void *buffer, long size) "java.nio.ByteBuffer"
%typemap(javain) (void *buffer, long size) "$javainput"
%typemap(javaout) (void *buffer, long size) {
    return $jnicall;
}

#include "zeromq_support.h"