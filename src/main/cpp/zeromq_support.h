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
#ifndef NATIVE_SUPPORT_H
#define NATIVE_SUPPORT_H

#ifndef SWIG
#include <boost/thread.hpp>
#include <string>
#include <map>
#include <zmq.hpp>
#endif

class ZeroMQSupport {

    void * buffer;
    long size;

    boost::condition_variable cond_data_ready;
    boost::mutex mut_data_ready;
    bool data_ready;

    zmq::context_t* ctx;

    zmq::socket_t* socket;
    boost::thread thread;

    bool consumer;

public:

	ZeroMQSupport();

	~ZeroMQSupport();

	void send(void * buffer, long size);

    long waitForMessage();

	void copy(void * buffer, long size);

	void start(const std::string& uri, const std::map<std::string, std::string>& properties, bool consumer);

	void stop();

	void put(void * buffer, long size);

};

#endif