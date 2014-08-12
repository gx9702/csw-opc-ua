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
* cd testclient/target/universal/stage/bin; ./testclient -Dlog4j.configuration=log.properties localhost 10000 1 2

Note that currently, the test results are not valid unless both the server and client are started new each time.
Currently the client exits without cleaning up, which might cause the problem.

Command line arguments
----------------------

(Specify all four arguments, or none for the default values):

*  hostname: host where OPC server is running (default: localhost)
*  count: number of times to set the OPC variable or fire the event (default: 1000)
*  delay: sleep time in ms between settings or events (default: 100)
*  testNo: The variable to set: 0: events, 1: scalar value, 2: analog array, 3: static array (default: 2)

The test client prints out statistics after the count is reached. 

