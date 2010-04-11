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
#include <boost/lexical_cast.hpp>
#include <iostream>

using namespace std;
using namespace std::tr1;
using namespace boost;
using namespace zmq;

mutex ZeroMQConsumerSupport::mut_receive;
mutex ZeroMQConsumerSupport::mut_ctx_socket;
shared_ptr<zmq::context_t> ZeroMQConsumerSupport::ctx;
shared_ptr<zmq::socket_t> ZeroMQConsumerSupport::socket;

mutex ZeroMQProducerSupport::mut_ctx_socket;
shared_ptr<zmq::context_t> ZeroMQProducerSupport::ctx;
shared_ptr<zmq::socket_t> ZeroMQProducerSupport::socket;

ZeroMQSupport::ZeroMQSupport() {
}

ZeroMQSupport::~ZeroMQSupport() {
}

ZeroMQConsumerSupport::ZeroMQConsumerSupport() : isStopped(false) {
}

ZeroMQConsumerSupport::~ZeroMQConsumerSupport() {
}

void ZeroMQConsumerSupport::start(const string& uri, const map<string, string>& properties) {
    lock_guard<mutex> lock(mut_ctx_socket);
    int concurrentConsumers = 1;
    for(map<string, string>::const_iterator it = properties.begin(); it != properties.end(); ++it) {
        if(it->first == "concurrentConsumers") {
            try {
                concurrentConsumers = boost::lexical_cast< int >(it->second);
            }
            catch( const boost::bad_lexical_cast & ) {
            }
        }
    }

    if(ctx.get() == 0) {
        ctx.reset(new context_t(concurrentConsumers, concurrentConsumers));
    }
    if(socket.get() == 0) {
        socket.reset(new socket_t(*ctx, ZMQ_P2P));
        socket->bind(uri.c_str());
    }
}

void ZeroMQConsumerSupport::stop() {
    isStopped = true;
}

int ZeroMQConsumerSupport::receive() {
    lock_guard<mutex> lock(mut_receive);
    this->message.reset(new message_t);
    if(!socket->recv(this->message.get(), ZMQ_NOBLOCK) || isStopped) {
        return -1;
    };
    return message->size();
}

void ZeroMQConsumerSupport::copy(char * buffer, int size) {
    memcpy(buffer, this->message->data(), size);
}

ZeroMQProducerSupport::ZeroMQProducerSupport() {
}

ZeroMQProducerSupport::~ZeroMQProducerSupport() {
}

void ZeroMQProducerSupport::start(const string& uri, const map<string, string>& properties) {
    lock_guard<mutex> lock(mut_ctx_socket);

    if(ctx.get() == 0) {
        ctx.reset(new context_t(1, 1));
    }
    if(socket.get() == 0) {
        socket.reset(new socket_t(*ctx, ZMQ_P2P));
        socket->connect(uri.c_str());
    }
}

void ZeroMQProducerSupport::stop() {
}

void ZeroMQProducerSupport::send(char * buffer, int size) {
    message_t msg(size);
    memcpy(msg.data(), buffer, size);
    socket->send(msg);
}
