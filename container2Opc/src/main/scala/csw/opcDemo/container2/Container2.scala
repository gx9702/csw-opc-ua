package csw.opcDemo.container2

import csw.services.apps.containerCmd.ContainerCmd

/**
 * Creates container2 based on resources/container2.conf
 */
object Container2 extends App {
  // This defines the names that can be used with the --start option and the config files used ("" is the default entry)
  val m = Map(
    "" -> "container2.conf" // default value
  )
  // Parse command line args for the application (app name is container2opc, like the sbt project)
  ContainerCmd("container2opc", args, m)
}
