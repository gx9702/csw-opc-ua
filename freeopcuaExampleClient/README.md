freeopcExampleClient
====================

This directory contains an example C++ freeopcua client that works together with 
../freeopcuaExampleServer to run performance tests.

Dependencies
------------

Before building, you need to build and install freeopcua.
The version I tested with was checked out from: https://github.com/oroulet/freeopcua.
I originally attempted to build under CentOs-6, but had problems with the required dependencies.
Upgrading to CentOs-7 and installing the necessary dependencies (by trial and error) worked.

Build
-----

The build uses cmake, since at the time, this was a requirement, in order to use the Intellij CLion IDE. 
Typing `cmake .` in this directory produces a Makefile. Then just type `make`.
You can open this directory with CLion to generate a project.

Running
-------

Before running the client, start the freeopcuaExampleServer.

Command Line Arguments
----------------------

The arguments are all optional.

Note: Here we have more args than the java version, since OPC methods are not implemented yet in freeopcua.

* -host _hostname_ - hostname where the server is running (default: localhost)
* -port _port_ - port where the server is running on host: default (localhost 52520)
* -count _count_ - number of times for server to set the OPC variable or fire the event (default: 100000))
* -delay _delay_ - sleep time in microsec between settings or events on server (default: 100)
* -testNo _testNo_: 0 to 4 to indicate the performance test to run (default: 1, scalar var):
      - 0: send events,
      - 1: set a scalar variable,
      - 2: set an analog array value,
      - 3: set a static array value
* -eventSize _eventSize_ - size of the event payload on server (default: 512 bytes)


