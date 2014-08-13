Performance Test OPC UA Client
==============================

This project contains a test OPC UA client that does a performance test, 
setting an OPC variable, array or firing an OPC event count times
with the given delay in ms between settings.

Dependencies
------------

This project depends on the ../hardware project, which contains the OPC UA server,
which needs to be running before the client starts for this test. It also depends on
the csw/csw/pkg project.

Build
-----

Type `sbt stage` to create the testclient start script.

Running
-------

To run the test client: Open terminal windows or tabs in these directories and run these commands:

* cd hardware/target/universal/stage/bin; ./hardware -Dlog4j.configuration=log.properties
* cd testclient/target/universal/stage/bin; ./testclient -Dlog4j.configuration=log.properties localhost 100000 100 0

Note that currently, the test results may not be valid unless both the server and client are started new each time.
If you restart the client without restarting the server, it will use the same id and might receive events from
methods started in the previous session.

testclient Command line arguments
---------------------------------

You can specify only the server host name, all four arguments, or no args for the default values:

*  hostname: host where OPC server is running (default: localhost)
*  count: number of times to set the OPC variable or fire the event (default: 100000)
*  delay: sleep time in microsec between settings or events (default: 100)
*  testNo: 0 to 4 to indicate the performance test to run (default: 0, events):
    - 0: send events,
    - 1: set a scalar variable,
    - 2: set an analog array value,
    - 3: set a static array value

The test client prints out statistics after the count is reached. 

