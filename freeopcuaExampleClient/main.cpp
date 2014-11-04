
#include <iostream>
#include <sstream>
#include <string.h>
#include "ExampleClient.h"

void usage() {
    std::cout << "Usage: freeopcuaExampleClient [options]" << std::endl;
    std::cout << "Options" << std::endl;
    std::cout << " -host hostname" << std::endl;
    std::cout << " -port port" << std::endl;
    std::cout << " -count count" << std::endl;
    std::cout << " -delay delay" << std::endl;
    std::cout << " -testNo testNo" << std::endl;
    std::cout << " -eventSize eventSize" << std::endl;
    exit(1);
}


// freeopcuaExampleClient main
//
// optional args: (Note: more args than the java version, since OPC methods are not implemented yet)
//
// host - hostname to bind to (default: localhost)
// port - port to listen on: default (localhost 52520)
// count - number of times to set the OPC variable or fire the event (default: 100000))
// delay - sleep time in microsec between settings or events (default: 100)
// testNo: 0 to 4 to indicate the performance test to run (default: 1, scalar var):
//      - 0: send events,
//      - 1: set a scalar variable,
//      - 2: set an analog array value,
//      - 3: set a static array value
// eventSize - size of the event payload (default: 512 bytes)
int main(int argc, const char** argv)
{
    if (--argc % 2 != 0) usage();

    const char *host = "localhost";
    int port = 52520;
    int count = 1000000;
    int delay = 10;
    int testNo = 1;
    int eventSize = 512;

    for(int i = 0; i < argc; i += 2) {
        const char* option = argv[i];
        const char* value = argv[i+1];
        if (strcmp(option, "-host") == 0) host = value;
        else if (strcmp(option, "-port") == 0) port = atoi(value);
        else if (strcmp(option, "-count") == 0) count = atoi(value);
        else if (strcmp(option, "-delay") == 0) delay = atoi(value);
        else if (strcmp(option, "-testNo") == 0) testNo = atoi(value);
        else if (strcmp(option, "-eventSize") == 0) eventSize = atoi(value);
        else usage();
    }

    ExampleClient client(host, port,count,delay, testNo, eventSize);
    client.start();

    return -1;
}

