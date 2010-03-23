#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
cmake_minimum_required(VERSION 2.8)

if(NOT ZEROMQ_PREFIX)
    if(ENV{ZEROMQ_PREFIX})
        set(PICOORB_ROOT $ENV{ZEROMQ_PREFIX})
    else(ENV{ZEROMQ_PREFIX})
        set(ZEROMQ_PREFIX /usr/local/zeromq)
    endif(ENV{ZEROMQ_PREFIX})
endif(NOT ZEROMQ_PREFIX)
if(NOT EXISTS ${ZEROMQ_PREFIX})
    message(FATAL_ERROR "ZEROMQ_PREFIX: ${ZEROMQ_PREFIX} doesn't exist")
endif(NOT EXISTS ${ZEROMQ_PREFIX})

FIND_PACKAGE(SWIG REQUIRED)
FIND_PACKAGE(JNI)
FIND_PACKAGE(Boost REQUIRED thread)

INCLUDE(${SWIG_USE_FILE})

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
INCLUDE_DIRECTORIES(${Boost_INCLUDE_DIRS})
INCLUDE_DIRECTORIES(${ZEROMQ_PREFIX}/include)

SET(CMAKE_SWIG_FLAGS -package org.apache.camel.component.zeromq)

SET_SOURCE_FILES_PROPERTIES(zeromq_support.i PROPERTIES CPLUSPLUS ON)
SET_SOURCE_FILES_PROPERTIES(zeromq_support.i PROPERTIES SWIG_FLAGS "-includeall")
SWIG_ADD_MODULE(zeromq_support java zeromq_support.i zeromq_support.cxx)
ADD_EXECUTABLE(client client.cxx)
TARGET_LINK_LIBRARIES(zeromq_support ${Boost_LIBRARIES} ${ZEROMQ_PREFIX}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}zmq${CMAKE_STATIC_LIBRARY_SUFFIX})
TARGET_LINK_LIBRARIES(client ${ZEROMQ_PREFIX}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}zmq${CMAKE_STATIC_LIBRARY_SUFFIX})