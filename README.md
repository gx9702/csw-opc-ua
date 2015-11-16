CSW OPC UA Demo
=============

This package contains demo code showing how OPC UA can be used in HCDs to control 
hardware (assuming there is an OPC UA server for the hardware).

It is similar to csw-pkg-test, except that the ZeroMQ hardware simulation has been replaced with OPC UA based code.
See the document "OSW TN009 - TMT CSW PACKAGING SOFTWARE DESIGN DOCUMENT" for a background description

Dependency on other CSW Projects
--------------------------------

This project depends on the main csw project as well as the csw-pkg-demo project, 
so you should run install.sh in csw/csw and csw-pkg-demo to install the necessary jars
and commands in the ../install directory.


Sbt Build
---------

To compile, run ./install.sh in this directory. This will install the necessary jars and scripts in ../install.

Run the demo
------------

To run the demo, cd to ../install/bin and run:

    test_containers_with_opcua.sh

This script runs the OPC-UA hardware simulation code, and the two containers for the test.
Container1 (which contains Assembly1) is the same as the one in csw-pkg-demo.
Container2opc contains the two OPC UA based HCDs. When you submit a config to Assembly,
different parts of it are sent to the two HCDs (for filter and disperser).
The HCDs set variables in the OPC server (which is a demo application here, but would normally
be provided by some hardware interface).


Test with the web app
---------------------

Open http://localhost:9000 in a browser for the web interface 

Select the filter and disperser values in the form and press Apply. 
The status of the command is shown below the button and updated
while the command is running.

The list of numbers below each item displays the telemetry that the app receives from
the OPC server and indicates that the filter or disperser wheel is moving past 
the different positions.

When the user fills out the web form and presses Submit, a config is sent to Assembly1. 
It forwards different parts of the config to HCD-2A (filter) and HCD-2B (disperser), 
which are in a different container and JVM, but are registered with the location service
and as required services for Assembly1. 
In this demo the assembly forwards the parts of the configs that match the prefixes they registered with.
However in a real application, an assembly might create completely new configs and send them
to different HCDs based on other logic.

HCD-2A and HCD-2B both talk to the OPC UA server and set state and telemetry variables based on
values received from the server. The assembly watches the state variables to determine when
a config has been "matched" and then returns the command status to the original sender
(in this case the web app in the browser).

If multiple users are accessing the web app at the same time, they will all see the same
telemetry values.



