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
#include "zeromq_support.h"
#include <zmq.hpp>
#include <iostream>

using namespace std;
using namespace std::tr1;
using namespace boost;
using namespace zmq;

mutex ZeroMQSupport::mut_ctx_socket;
shared_ptr<zmq::context_t> ZeroMQSupport::ctx;
shared_ptr<zmq::socket_t> ZeroMQSupport::socket;

class run
{

    ZeroMQConsumerSupport& support;

    shared_ptr<socket_t> socket;

    string uri;

public:

    run(ZeroMQConsumerSupport& support, const shared_ptr<socket_t>& socket, const string& uri) : support(support), socket(socket), uri(uri) {
    }

    void operator()() {
        while(true) {
            lock_guard<mutex> lock(ZeroMQConsumerSupport::mut_ctx_socket);
            message_t msg;
            while(!socket->recv(&msg, ZMQ_NOBLOCK)) {
                this_thread::interruption_point();
            }
            support.put(msg.data(), msg.size());
        }
    }
};

ZeroMQSupport::ZeroMQSupport() {
}

ZeroMQSupport::~ZeroMQSupport() {
}

ZeroMQConsumerSupport::ZeroMQConsumerSupport() {
}

ZeroMQConsumerSupport::~ZeroMQConsumerSupport() {
}

void ZeroMQConsumerSupport::start(const string& uri, const map<string, string>& properties) {
    lock_guard<mutex> lock(mut_ctx_socket);

    if(ctx.get() == 0) {} {
        ctx.reset(new context_t(2, 2));
    }
    if(socket.get() == 0) {
        socket.reset(new socket_t(*ctx, ZMQ_P2P));
        socket->bind(uri.c_str());
    }
    run callable(*this, socket, uri);
    this->thread = boost::thread(callable);
}

void ZeroMQConsumerSupport::stop() {
    this->thread.interrupt();
    {
        lock_guard<mutex> lock1(mut_data_ready);
        data_ready=true;
        this->size = -1;
        cond_data_ready.notify_one();
    }
}

int ZeroMQConsumerSupport::waitForMessage() {
    unique_lock<mutex> lock(mut_data_ready);
    while(!data_ready)
    {
        cond_data_ready.wait(lock);
    }

    return size;
}

void ZeroMQConsumerSupport::copy(char * buffer, int size) {
    lock_guard<mutex> lock(mut_data_ready);
    memcpy(buffer, this->buffer, size);
    this->buffer = NULL;
    this->size = -1;
    data_ready = false;
    cond_data_ready.notify_one();
}

void ZeroMQConsumerSupport::put(void * buffer, int size) {
    {
        lock_guard<mutex> lock1(mut_data_ready);
        this->buffer = buffer;
        this->size = size;
        data_ready=true;
        cond_data_ready.notify_one();
    }
    {
        unique_lock<mutex> lock2(mut_data_ready);
        while(data_ready)
        {
            cond_data_ready.wait(lock2);
        }
    }
}

ZeroMQProducerSupport::ZeroMQProducerSupport() {
}

ZeroMQProducerSupport::~ZeroMQProducerSupport() {
}

void ZeroMQProducerSupport::start(const string& uri, const map<string, string>& properties) {
    ctx = shared_ptr<context_t>(new context_t(1, 1));
    socket = shared_ptr<socket_t>(new socket_t(*ctx, ZMQ_P2P));
    socket->connect(uri.c_str());
}

void ZeroMQProducerSupport::stop() {
}

void ZeroMQProducerSupport::send(char * buffer, int size) {
    message_t msg(size);
    memcpy(msg.data(), buffer, size);
    socket->send(msg);
}
