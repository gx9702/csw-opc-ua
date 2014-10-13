package csw.opc;

import csw.opc.client.JFreeOpcUaDemoClient;
import csw.opc.client.JOpcDemoClient;
import org.apache.log4j.Logger;
import org.scalatest.DoNotDiscover;

// This test requires that OpcDemoServer server is running
@DoNotDiscover
public class JFreeOpcUaDemoClientTests {
    private static Logger log = Logger.getLogger(JOpcDemoClientTests.class);

    public static void main(String[] args) throws Exception {
        String host = (args.length == 0) ? "localhost" : args[0];
        final JFreeOpcUaDemoClient client = new JFreeOpcUaDemoClient(host, new JFreeOpcUaDemoClient.Listener() {

            @Override
            public void filterChanged(String value) {
                log.info("filter changed to: " + value);
            }

            @Override
            public void disperserChanged(String value) {
                log.info("disperser changed to: " + value);
            }

            @Override
            public void perfTestVarChanged(int value) {
                log.info("perfTestVar changed to: " + value);
            }

            @Override
            public void analogArrayVarChanged(Integer[] value) {
                log.info("analogArrayVar[0] changed to: " + value[0]);
            }

            @Override
            public void staticArrayVarChanged(Integer[] value) {
                log.info("staticArrayVar[0] changed to: " + value[0]);
            }

            @Override
            public void onEvent(int value) {
                log.info("event " + value);
            }
        });

        client.setFilter("NewFilter");
        client.setDisperser("NewDisperser");
        client.setFilter("NewFilter2");
    }
}
