#include <stdio.h>
#include <zmq.hpp>

int main ()
{
    try {
        zmq::context_t ctx (1, 1);
        zmq::socket_t s (ctx, ZMQ_P2P);
        s.connect ("tcp://localhost:8000");
        zmq::message_t msg(100);
        for(int i=0; i<50000; ++i)
            s.send(msg);
        sleep(1);
    }
    catch (std::exception &e) {
        // 0MQ throws standard exceptions just like any other C++ API
        printf ("An error occurred: %s\n", e.what());
        return 1;
    }

    return 0;
}