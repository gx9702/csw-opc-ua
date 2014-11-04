#pragma once

class ExampleClient {
private:
    const char *host;
    int port;
    int count;
    int delay;
    int testNo;
    int eventSize;

public:
    ExampleClient(char const *host, int port, int count, int delay, int testNo, int eventSize)
            : host(host), port(port), count(count), delay(delay), testNo(testNo), eventSize(eventSize) {
    }

    void start();

};