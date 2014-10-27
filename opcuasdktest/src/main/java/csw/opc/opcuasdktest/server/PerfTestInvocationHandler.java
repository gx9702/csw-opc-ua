package csw.opc.opcuasdktest.server;

import com.inductiveautomation.opcua.sdk.server.api.MethodInvocationHandler;
import com.inductiveautomation.opcua.sdk.server.nodes.UaVariableNode;
import com.inductiveautomation.opcua.stack.core.StatusCodes;
import com.inductiveautomation.opcua.stack.core.types.builtin.DataValue;
import com.inductiveautomation.opcua.stack.core.types.builtin.DateTime;
import com.inductiveautomation.opcua.stack.core.types.builtin.DiagnosticInfo;
import com.inductiveautomation.opcua.stack.core.types.builtin.StatusCode;
import com.inductiveautomation.opcua.stack.core.types.builtin.Variant;
import com.inductiveautomation.opcua.stack.core.types.structured.CallMethodRequest;
import com.inductiveautomation.opcua.stack.core.types.structured.CallMethodResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Handler for the setFilter and setDisperser methods
 */
class PerfTestInvocationHandler implements MethodInvocationHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    UaVariableNode opcVar;
    UaVariableNode analogArrayNode;
    UaVariableNode staticArrayNode;
    int eventSize = 0;

    // The event payload, for performance testing
    private final String eventPayload;

    public PerfTestInvocationHandler(UaVariableNode opcVar,
                                     UaVariableNode analogArrayNode,
                                     UaVariableNode staticArrayNode,
                                     int eventSize) {
        this.opcVar = opcVar;
        this.analogArrayNode = analogArrayNode;
        this.staticArrayNode = staticArrayNode;
        this.eventSize = eventSize;

        final char[] array = new char[eventSize];
        Arrays.fill(array, 'x');
        this.eventPayload = new String(array);
    }

    @Override
    public void invoke(CallMethodRequest request, CompletableFuture<CallMethodResult> result) {
        Variant[] inputs = request.getInputArguments();

        if (inputs.length != 3) {
            result.complete(new CallMethodResult(
                    new StatusCode(StatusCodes.Bad_ArgumentsMissing),
                    new StatusCode[0],
                    new DiagnosticInfo[0],
                    new Variant[0]
            ));
        }

        for (Variant input : inputs) {
            if (!(input.getValue() instanceof Integer)) {
                result.complete(new CallMethodResult(
                        new StatusCode(StatusCodes.Bad_TypeMismatch),
                        new StatusCode[]{new StatusCode(StatusCodes.Bad_TypeMismatch)},
                        new DiagnosticInfo[0],
                        new Variant[0]
                ));
                return;
            }
        }

        int count = (Integer) inputs[0].getValue();
        int delay = (Integer) inputs[1].getValue();
        int testNo = (Integer) inputs[2].getValue();
//        opcVar.setMinimumSamplingInterval(Optional.of(delay / 2.0));
//        staticArrayNode.setMinimumSamplingInterval(Optional.of(delay / 2.0));
        simulateBackgroundWork(count, delay, testNo);

        result.complete(new CallMethodResult(
                StatusCode.Good,
                new StatusCode[]{StatusCode.Good},
                new DiagnosticInfo[0],
                new Variant[]{new Variant(true)}
        ));
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
                        opcVar.setValue(new DataValue(new Variant(i), StatusCode.Good, t, t));
                        break;
                    default:
                        // change array variable
                        UaVariableNode varNode = (testNo == 2) ? analogArrayNode : staticArrayNode;
                        final DataValue dv = varNode.getValue();
                        Integer[] ar = (Integer[]) dv.getValue().getValue();
                        Integer[] ar2 = Arrays.copyOf(ar, ar.length);
                        ar2[0] = i;
                        varNode.setValue(new DataValue(new Variant(ar2), StatusCode.Good, t, t));
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
                log.info("Done: Sent " + count + " events in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 1:
                log.info("Done: Updated scalar variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 2:
                log.info("Done: Updated analog array variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
                break;
            case 3:
                log.info("Done: Updated static array variable " + count + " times in " + secs + " seconds, delay in μs was: " + delay);
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
