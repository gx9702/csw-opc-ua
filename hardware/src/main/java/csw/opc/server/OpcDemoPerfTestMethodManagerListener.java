package csw.opc.server;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.nodes.PlainVariable;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.types.opcua.AnalogItemType;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.core.StatusCodes;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

// Creates a 'perfTest' OPC method that continually sets the pertTestVar OPC variable to different values in a
// background thread.
// The perfTest method takes three int arguments:
// * count: The number of times to increment the variable,
// * delay: The delay in ms between settings.
// * testNo: If 0, send event, otherwise: The variable to set: 1: scalar value, 2: analog array, 3: static array
public class OpcDemoPerfTestMethodManagerListener implements CallableListener {
    private static Logger log = Logger.getLogger(OpcDemoPerfTestMethodManagerListener.class);

    OpcDemoNodeManager opcDemoNodeManager;
    final private UaNode method;
    final private PlainVariable<Integer> opcVar;
    final private AnalogItemType analogArrayNode;
    final private UaVariableNode staticArrayNode;

    public OpcDemoPerfTestMethodManagerListener(OpcDemoNodeManager opcDemoNodeManager,
                                                PlainVariable<Integer> opcVar,
                                                AnalogItemType analogArrayNode, UaVariableNode staticArrayNode,
                                                UaNode method) {
        this.opcDemoNodeManager = opcDemoNodeManager;
        this.opcVar = opcVar;
        this.analogArrayNode = analogArrayNode;
        this.staticArrayNode = staticArrayNode;
        this.method = method;
    }

    @Override
    public boolean onCall(ServiceContext serviceContext, NodeId objectId,
                          UaNode object, NodeId methodId, UaMethod method,
                          final Variant[] inputArguments,
                          final StatusCode[] inputArgumentResults,
                          final DiagnosticInfo[] inputArgumentDiagnosticInfos,
                          final Variant[] outputs) throws StatusException {
        if (methodId.equals(this.method.getNodeId())) {
            MethodManager.checkInputArguments(new Class[]{Integer.class, Integer.class, Integer.class}, inputArguments,
                    inputArgumentResults, inputArgumentDiagnosticInfos, false);
            int count, delay, testNo;
            try {
                count = inputArguments[0].intValue();
                delay = inputArguments[1].intValue();
                testNo = inputArguments[2].intValue();
            } catch (ClassCastException e) {
                throw inputError(1, e.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
            }

            simulateBackgroundWork(count, delay, testNo);

            outputs[0] = new Variant(true);
            return true; // Handled here
        } else
            return false;
    }

    /**
     * Handle an error in method inputs.
     *
     * @param index                        index of the failing input
     * @param message                      error message
     * @param inputArgumentResults         the results array to fill in
     * @param inputArgumentDiagnosticInfos the diagnostics array to fill in
     * @return StatusException that can be thrown to break further method
     * handling
     */
    private StatusException inputError(final int index, final String message,
                                       StatusCode[] inputArgumentResults,
                                       DiagnosticInfo[] inputArgumentDiagnosticInfos) {
        log.info("inputError: #" + index + " message=" + message);
        inputArgumentResults[index] = new StatusCode(StatusCodes.Bad_InvalidArgument);
        final DiagnosticInfo di = new DiagnosticInfo();
        di.setAdditionalInfo(message);
        inputArgumentDiagnosticInfos[index] = di;
        return new StatusException(StatusCodes.Bad_InvalidArgument);
    }

    private void sendEvent(int value) {
        // If the type has TypeDefinitionId, you can use the class
        OpcDemoEventType ev = opcDemoNodeManager.createEvent(OpcDemoEventType.class);
        ev.setMessage("array changed");
        ev.setVariableValue(value);
        ev.setPropertyValue("Property Value " + ev.getVariableValue());
        ev.triggerEvent(null);
        log.info("Sent event: " + value);
    }


    // Starts a background thread that continuously sets the OPC variable (given by testNo)
    private void simulateBackgroundWork(final int count, final int delay, final int testNo) {
        if (testNo == 0) {
            // just send event
            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                private int eventValue = 0;

                @Override
                public void run() {
                    if (eventValue >= count) timer.cancel();
                    sendEvent(eventValue++);
                }
            }, delay, delay);
        } else if (testNo == 1) {
            // change scalar variable
            opcVar.setMinimumSamplingInterval((double) delay);
            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                private int i = 0;
                @Override
                public void run() {
                    if (i >= count) timer.cancel();
                    opcVar.setCurrentValue(i);
                    log.info("set perfTestVar to " + i);
                }
            }, delay, delay);
        } else {
            // change array variable
            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                private int i = 0;
                @Override
                public void run() {
                    if (i >= count) timer.cancel();
                    final DataValue dv = (testNo == 2) ? analogArrayNode.getValue() : staticArrayNode.getValue();
                    Integer[] ar = (Integer[])dv.getValue().getValue();
                    ar[0]++; // could update the whole array, but that is not the point of the test here
                    dv.setValue(new Variant(ar));
                    dv.setServerTimestamp(DateTime.currentTime());
                    dv.setSourceTimestamp(DateTime.currentTime());
                    try {
                        if (testNo == 2) {
                            // change analog array variable
                            analogArrayNode.setValue(dv);
                        } else {
                            // change static array variable
                            staticArrayNode.setValue(dv);
                        }
                    } catch (StatusException e) {
                        e.printStackTrace();
                    }
                }
            }, delay, delay);
        }
    }
}
