package csw.opc.server;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.nodes.PlainVariable;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.StatusCodes;

import java.util.Arrays;
import java.util.Random;

public class OpcDemoMethodManagerListener implements CallableListener {

    private static Logger log = Logger.getLogger(OpcDemoMethodManagerListener.class);
    OpcDemoNodeManager opcDemoNodeManager;
    final private UaNode method;
    final private String name;
    final private PlainVariable<String> opcVar;

    // Current background thread simulating the device busy working
    private Thread currentWorkThread = null;

    public OpcDemoMethodManagerListener(OpcDemoNodeManager opcDemoNodeManager, String name,
                                        PlainVariable<String> opcVar, UaNode method) {
        super();
        this.opcDemoNodeManager = opcDemoNodeManager;
        this.name = name;
        this.opcVar = opcVar;
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
            String methodName = "set" + name;
            log.info("Calling " + methodName + ": " + Arrays.toString(inputArguments));
            MethodManager.checkInputArguments(new Class[]{String.class}, inputArguments, inputArgumentResults,
                    inputArgumentDiagnosticInfos, false);
            String value;
            try {
                value = inputArguments[0].toString();
            } catch (ClassCastException e) {
                throw inputError(1, e.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
            }

            // If the work is already in progress, cancel it and start the new work
            // TODO: (should check if value is different)
            if (currentWorkThread != null) currentWorkThread.interrupt();
            currentWorkThread = simulateBackgroundWork(value);

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

    /**
     * Send an event
     * @param value some value
     */
    public void sendEvent(int value) {
        // If the type has TypeDefinitionId, you can use the class
        OpcDemoEventType ev = opcDemoNodeManager.createEvent(OpcDemoEventType.class);
        ev.setMessage(name + "changing");
        ev.setVariableValue(value);
        ev.setPropertyValue("Property Value " + ev.getVariableValue());
        ev.triggerEvent(null);
    }

    // Simulate a hardware device taking time to complete the action for the method in a background thread
    private Thread simulateBackgroundWork(final String value) {
        Runnable r = new Runnable() {
            public void run() {
                simulateWork(value);
            }
        };
        Thread t = new Thread(r);
        t.start();
        return t;
    }

    // Simulate a hardware device taking time to complete the action for the method
    private void simulateWork(final String value) {
        log.info("Starting background work to set " + name + " to " + value);
        int n = randInt(1, 5) * 100;
        log.info("XXX " + name + ": n = " + n);
        for(int i = 0; i < n; i++) {
            try {
                Thread.sleep(10);
                sendEvent(i);
            } catch (InterruptedException e) {
                log.info("setting of " + name + " to " + value + " was interrupted");
                return;
            }
        }
        log.info("Setting " + name + " to " + value);
        opcVar.setCurrentValue(value);
        currentWorkThread = null;
    }

    private static int randInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
