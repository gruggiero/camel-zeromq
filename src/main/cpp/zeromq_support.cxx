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
#include <iostream>
#include "zeromq_support.h"

class run
{

    ZeroMQSupport* support;
    
    zmq::socket_t* socket;

    std::string uri;

public:

    run(ZeroMQSupport* support, zmq::socket_t* socket, const std::string& uri) : support(support), socket(socket), uri(uri) {
    }

    void operator()() {
        socket->bind(uri.c_str());
        while(true) {
            zmq::message_t msg;
            while(!socket->recv(&msg, ZMQ_NOBLOCK)) {
                boost::this_thread::interruption_point();
            }
            support->put(msg.data(), msg.size());
        }
    }
};


ZeroMQSupport::ZeroMQSupport() : socket(NULL) {
}

ZeroMQSupport::~ZeroMQSupport() {
}

void ZeroMQSupport::send(void * buffer, long size) {
    zmq::message_t msg(size);
    memcpy(msg.data(), buffer, size);
    socket->send(msg);
}

long ZeroMQSupport::waitForMessage() {
    boost::unique_lock<boost::mutex> lock(mut_data_ready);
    while(!data_ready)
    {
        cond_data_ready.wait(lock);
    }

    return size;
}

void ZeroMQSupport::copy(void * buffer, long size) {
    memcpy(buffer, this->buffer, size);

    this->buffer = NULL;
    this->size = -1;

    boost::lock_guard<boost::mutex> lock(mut_data_ready);
    data_ready = false;
    cond_data_ready.notify_one();
}

void ZeroMQSupport::put(void * buffer, long size) {
    this->buffer = buffer;
    this->size = size;
    {
        boost::lock_guard<boost::mutex> lock1(mut_data_ready);
        data_ready=true;
        cond_data_ready.notify_all();
    }
    {
        boost::unique_lock<boost::mutex> lock2(mut_data_ready);
        while(data_ready)
        {
            cond_data_ready.wait(lock2);
        }
    }
}

void ZeroMQSupport::start(const std::string& uri, const std::map<std::string, std::string>& properties, bool consumer) {
    ctx = new zmq::context_t(1, 1);
    socket = new zmq::socket_t(*ctx, ZMQ_P2P);
    if(consumer) {
        run callable(this, socket, uri);
        this->thread = boost::thread(callable);
    } else {
        socket->connect(uri.c_str());
    }
}

void ZeroMQSupport::stop() {
    if(consumer) {
        this->thread.interrupt();
        {
            boost::lock_guard<boost::mutex> lock1(mut_data_ready);
            data_ready=true;
            this->size = -1;
            cond_data_ready.notify_one();
        }
    }
    delete ctx;
    delete socket;
}

