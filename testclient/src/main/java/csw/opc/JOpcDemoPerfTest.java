package csw.opc;

import csw.opc.client.JOpcDemoClient;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DateTime;

/**
 * Starts a performance test, setting an OPC variable or firing an OPC event count times,
 * with the given delay in &mu;s between settings.
 *
 * The command line args: (specify all four, or none for the default values):
 *  hostname: host where OPC server is running (default: localhost)
 *  count: number of times to set the OPC variable
 *  delay: sleep time in &mu;s between settings
 *  testNo: The variable to set: 1: scalar value, 2: analog array, 3: static array
 */
public class JOpcDemoPerfTest {
    private static Logger log = Logger.getLogger(JOpcDemoPerfTest.class);
    private static DateTime startTime;

    private final JOpcDemoClient client;

    private int receivedEvents = 0;
    private int receivedVarUpdates = 0;
    private int receivedAnalogArrayUpdates = 0;
    private int receivedStaticArrayUpdates = 0;

    JOpcDemoPerfTest(String host, int count, int delay, int testNo) throws Exception {
        client = initClient(host, count, delay, testNo);

        // Start a performance test on the server, setting an OPC variable count times, with the given
        // delay in &mu;s between settings. Args:
        startTime = DateTime.currentTime();
        client.startPerfTest(count, delay, testNo);
    }

    private JOpcDemoClient initClient(String host, final int count, final int delay, final int testNo) throws Exception {
        return new JOpcDemoClient(host, new JOpcDemoClient.Listener() {

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
                receivedVarUpdates++;
                if (value >= count) {
                    logResults(receivedVarUpdates, count, delay, testNo);
                }
            }

            @Override
            public void analogArrayVarChanged(Integer[] value) {
                receivedAnalogArrayUpdates++;
                if (value[0] >= count) {
                    logResults(receivedAnalogArrayUpdates, count, delay, testNo);
                }
            }

            @Override
            public void staticArrayVarChanged(Integer[] value) {
                receivedStaticArrayUpdates++;
                if (value[0] >= count) {
                    logResults(receivedStaticArrayUpdates, count, delay, testNo);
                }
            }

            @Override
            public void onEvent(int value) {
                receivedEvents++;
                if (value >= count) {
                    logResults(receivedEvents, count, delay, testNo);
                }
            }
        });
    }


    // Log results of performance test
    private void logResults(int count, int expected, int delay, int testNo) {
        double secs = (DateTime.currentTime().getTimeInMillis() - startTime.getTimeInMillis()) / 1000.0;
        double rate = Math.round(count / secs);
        double expectedRate = Math.round(expected / secs);
        switch (testNo) {
            case 0:
                log.info("Done: Received " + count + " of expected " + expected + " events in " + secs
                        + " seconds (" + rate + "/sec, with skipped " + expectedRate + "/sec), delay in microsecs was: "
                        + delay);
                break;
            case 1:
                log.info("Done: Received " + count + " of expected " + expected + " variable updates in " + secs
                        + " seconds (" + rate + "/sec, with skipped " + expectedRate + "/sec), delay in microsecs was: "
                        + delay);
                break;
            case 2:
                log.info("Done: Received " + count + " of expected " + expected + " analog array variable updates in " + secs
                        + " seconds (" + rate + "/sec, with skipped " + expectedRate + "/sec), delay in microsecs was: "
                        + delay);
                break;
            case 3:
                log.info("Done: Received " + count + " of expected " + expected + " static array variable updates in " + secs
                        + " seconds (" + rate + "/sec, with skipped: " + expectedRate + "/sec), delay in microsecs was: "
                        + delay);
                break;
        }
        client.disconnect();
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configureAndWatch(JOpcDemoClient.class.getResource("/log.properties").getFile(), 5000);

        // The server host
        String host = "localhost";

        // number of times to set the OPC variable
        int count = 100000;

        // sleep time in &mu;s between settings
        int delay = 100;

        // The variable to set: 1: scalar value, 2: analog array, 3: static array
        int testNo = 0;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length == 4) {
            count = Integer.valueOf(args[1]);
            delay = Integer.valueOf(args[2]);
            testNo = Integer.valueOf(args[3]);
        }

        new JOpcDemoPerfTest(host, count, delay, testNo);
    }
}
