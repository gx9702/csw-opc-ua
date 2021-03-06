#!/bin/sh
exec scala "$0" "$@"
!#

// Demonstrates starting the test containers with HCDs based on OPC-UA.
// This script should be run from the csw install/bin directory, or with csw/install/bin in the shell path.

import scala.sys.process._

// Start the OPC-UA based hardware simulation
"hcd2opcserver".run

// Start the containers with the default configuration
"container1".run
"container2opc".run
