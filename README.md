CSW OPC UA Demo
=============

This package contains demo code showing how OPC UA can be used in the command service to access HCDs.

It is similar to csw-pkg-test, except that the ZeroMQ hardware simulation has been replaced with OPC UA based code.
See the document "OSW TN009 - TMT CSW PACKAGING SOFTWARE DESIGN DOCUMENT" for a background description

Prosys OPC UA Dependency
----------------------------

Before compiling, you need to manually copy (or better, make links to) these two jar files from
the Prosys OPC UA distribution:

* Opc.Ua.Stack-*.jar
* Prosys-OPC-UA-Java-SDK-Client-Server-*.jar

in the following directories (create the lib dir if it does not exist):

* hardware/lib
* container2/lib
* testclient/lib

The other dependencies are all available from repositories and are automatically downloaded.

Dependency on other CSW Projects
--------------------------------

This project depends on the pkg project in csw/csw, so you should run `sbt publishLocal` and `sbt stage` there.


Sbt Build
---------

To compile, type "sbt" and then:

* compile
* stage

The stage task creates the distribution with the scripts for the applications
(found under the target/universal/stage/bin directories).

Note: See <a href="https://github.com/tmtsoftware/csw-extjs">csw-extjs</a> for how to setup the ExtJS
based web UI used below. You need to install and run some "sencha" commands once to prepare the web app, otherwise
the generated CSS file will not be found and the web app will not display properly.

See the <a href="https://github.com/tmtsoftware/csw-opc-ua/testclient">testclient README</a> for instructions on
setting up the performance test.

Running the Container/ExtJS Demo
--------------------------------

To run the demo: Open terminal windows or tabs in these directories and run these commands:

* cd hardware/target/universal/stage/bin; ./hardware

Start the location service (This has to be running before any HCDs or assemblies are started):

* cd ../csw/loc/target/universal/stage/bin; ./loc

Then start the two Akka containers (The order is not important here):

* cd container2/target/universal/stage/bin; ./container2
* cd container1/target/universal/stage/bin; ./container1

* open http://localhost:8089 in a browser for the Ext JS version and select the development
(JavaScript source) or production (compiled, minified) version. Note that you need to
compile the ExtJS code at least once to get the required CSS file generated.
See <a href="https://github.com/tmtsoftware/csw-extjs">csw-extjs</a> for instructions.

Select the values in the form and press Submit. The status of the command is shown below the button and updated
while the command is running.

TODO: Add the ability to pause and restart the queue, pause, cancel or abort a command, etc.

When the user fills out the web form and presses Submit, a JSON config is sent to the Spray/REST HTTP server
of the Assembly1 command service. It forwards different parts of the config to HCD1 and HCD2, which are in
a different container and JVM, but are registered as components with Assembly1, so that it forwards parts of
configs that match the keys they registered with.

HCD1 and HCD2 both talk to the OPC UA based hardware simulation code and then return a command status to the
original submitter (Assembly1).



