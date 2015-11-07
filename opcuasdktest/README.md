opc-ua-sdk Test
===============

This project contains a server based on the open source opc-ua-sdk toolkit.

Dependencies
------------

Currently you need to install all three projects found at https://github.com/digitalpetri manually using maven
(uanodeset-parser, opc-ua-stack, opc-ua-sdk: in that order).

Running
-------

The client API is not available yet, so there is only a server: OpcUaDemoServer.
It defines the filter and disperser variables as well as the perfTest method, which starts
a performance test (The required client is not yet available).

Command Line Options
--------------------

The arguments are all optional.

* -host _hostname_ - hostname to bind to (default: localhost)
* -port _port_ - port to use on host: default (localhost 52520)
* -delay _delay_ - sleep time in microsec between settings or events (default: 100)
* -eventSize _eventSize_ - size of the event payload (default: 512 bytes)
