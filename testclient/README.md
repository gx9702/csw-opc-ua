Performance Test OPC UA Client
==============================

This project contains a test OPC UA client that does a performance test, 
setting an OPC variable, array or firing an OPC event count times
with the given delay in ms between settings.

Dependencies
------------

This project depends on the ../hardware project, which contains the OPC UA server,
which needs to be running before the client starts for this test.

Build
-----

Type `sbt stage` to create the testclient start script.

Running
-------

To run the test client: Open terminal windows or tabs in these directories and run these commands:

* cd hardware/target/universal/stage/bin; ./hardware -Dlog4j.configuration=log.properties
* cd testclient/target/universal/stage/bin; ./testclient -Dlog4j.configuration=log.properties

Add command line arguments to testclient (see below).

Command line arguments
----------------------

(Specify all four, or none for the default values):

*  hostname: host where OPC server is running (default: localhost)
*  count: number of times to set the OPC variable or fire the event
*  delay: sleep time in ms between settings or events
*  testNo: The variable to set: 0: events, 1: scalar value, 2: analog array, 3: static array

The test client prints out statistics after the count is reached. 

