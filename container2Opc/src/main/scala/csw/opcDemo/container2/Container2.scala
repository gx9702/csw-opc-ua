package csw.opcDemo.container2

import csw.services.apps.containerCmd.ContainerCmd

/**
 * Creates container2 based on resources/container2.conf
 */
object Container2 extends App {
  val a = args // Required to avoid null args below
  ContainerCmd("Container2opc", a, Some("container2.conf"))
}
