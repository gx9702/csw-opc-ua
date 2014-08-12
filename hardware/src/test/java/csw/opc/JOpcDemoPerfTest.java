package csw.opc;

import csw.opc.client.JOpcDemoClient;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.scalatest.DoNotDiscover;

// This test requires that OpcDemoServer server is running
@DoNotDiscover
public class JOpcDemoPerfTest {
    private static Logger log = Logger.getLogger(JOpcDemoPerfTest.class);

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configureAndWatch(JOpcDemoClient.class.getResource("/log.properties").getFile(), 5000);

        // number of times to set the OPC variable
        int count = 1000;

        // sleep time in ms between settings
        int delay = 100;

        // The variable to set: 1: scalar value, 2: analog array, 3: static array
        int testNo = 2;

        if (args.length == 3) {
            count = Integer.valueOf(args[0]);
            delay = Integer.valueOf(args[1]);
            testNo = Integer.valueOf(args[2]);
        }

        final JOpcDemoClient client = new JOpcDemoClient(new JOpcDemoClient.Listener() {

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
        });


        /**
         * Start a performance test on the server, setting an OPC variable count times, with the given
         * delay in ms between settings. Args:
         */
        client.startPerfTest(count, delay, testNo);


        log.info("XXX analog ar[0] is set to: " + client.getAnalogArrayVarValue()[0]);

    }
}
