package csw.opc.opcuasdktest.server.methods;

import com.inductiveautomation.opcua.sdk.server.model.UaVariableNode;
import com.inductiveautomation.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import com.inductiveautomation.opcua.sdk.server.util.UaInputArgument;
import com.inductiveautomation.opcua.sdk.server.util.UaMethod;
import com.inductiveautomation.opcua.sdk.server.util.UaOutputArgument;
import com.inductiveautomation.opcua.stack.core.types.builtin.DataValue;
import com.inductiveautomation.opcua.stack.core.types.builtin.DateTime;
import com.inductiveautomation.opcua.stack.core.types.builtin.StatusCode;
import com.inductiveautomation.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PerfTestMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    UaVariableNode opcVar;
    UaVariableNode analogArrayNode;
    UaVariableNode staticArrayNode;
    int eventSize = 0;

    public PerfTestMethod(UaVariableNode opcVar,
                          UaVariableNode analogArrayNode,
                          UaVariableNode staticArrayNode,
                          int eventSize) {
        this.opcVar = opcVar;
        this.analogArrayNode = analogArrayNode;
        this.staticArrayNode = staticArrayNode;
        this.eventSize = eventSize;

        final char[] array = new char[eventSize];
        Arrays.fill(array, 'x');
    }

    @UaMethod
    public void invoke(
            AnnotationBasedInvocationHandler.InvocationContext context,

            @UaInputArgument(
                    name = "count",
                    description = "Number of iterations for test.")
            Integer count,

            @UaInputArgument(
                    name = "delay",
                    description = "Number of microsecs between iterations.")
            Integer delay,

            @UaInputArgument(
                    name = "testNo",
                    description = "Test to run (0 - 3).")
            Integer testNo,

            @UaOutputArgument(
                    name = "result",
                    description = "True if the arguments were valid and the test could be started")
            AnnotationBasedInvocationHandler.Out<Boolean> result) {

        logger.debug(String.format("Invoking perfTest method of Object '%s'",
                context.getObjectNode().getBrowseName().getName()));

//        opcVar.setMinimumSamplingInterval(Optional.of(delay / 2.0));
//        staticArrayNode.setMinimumSamplingInterval(Optional.of(delay / 2.0));
        simulateBackgroundWork(count, delay, testNo);

        result.set(true);
    }

    // Starts a background thread that continuously sets the OPC variable (given by testNo)
    private void simulateBackgroundWork(final int count, final int delay, final int testNo) {
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() {
            private int i = 0;
            private DateTime startTime = DateTime.now();

            @Override
            public void run() {
                if (i > count) {
                    service.shutdown();
                    DateTime stopTime = DateTime.now();
                    long t = stopTime.getUtcTime() - startTime.getUtcTime();
                    logResults(t / 1000.0, count, delay, testNo);
//                    i = 0; // reset below for next test
                }
                i++;
                DateTime t = DateTime.now();
                switch (testNo) {
                    case 0:
                        // just send event
                        sendEvent(i);
                        break;
                    case 1:
                        // change scalar variable
                        opcVar.setValue(new DataValue(new Variant(i), StatusCode.GOOD, t, t));
                        break;
                    default:
                        // change array variable
                        UaVariableNode varNode = (testNo == 2) ? analogArrayNode : staticArrayNode;
                        final DataValue dv = varNode.getValue();
                        Integer[] ar = (Integer[]) dv.getValue().getValue();
                        Integer[] ar2 = Arrays.copyOf(ar, ar.length);
                        ar2[0] = i;
                        varNode.setValue(new DataValue(new Variant(ar2), StatusCode.GOOD, t, t));
                        break;
                }
            }
        };

        service.scheduleAtFixedRate(task, delay, delay, TimeUnit.MICROSECONDS);
    }

    // Log results of performance test
    private void logResults(double secs, int count, int delay, int testNo) {
        switch (testNo) {
            case 0:
                logger.info("Done: Sent " + count + " events in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 1:
                logger.info("Done: Updated scalar variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 2:
                logger.info("Done: Updated analog array variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 3:
                logger.info("Done: Updated static array variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
                break;
        }

    }

    private void sendEvent(int value) {
        // XXX TODO
//        OpcDemoEventType ev = opcDemoNodeManager.createEvent(OpcDemoEventType.class);
//        ev.setMessage("array changed");
//        ev.setVariableValue(value);
//        ev.setPropertyValue(eventPayload);
//        ev.triggerEvent(null);
    }

}
