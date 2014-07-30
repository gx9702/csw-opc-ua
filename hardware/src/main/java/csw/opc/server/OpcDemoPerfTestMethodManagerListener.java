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

import java.util.Timer;
import java.util.TimerTask;

public class OpcDemoPerfTestMethodManagerListener implements CallableListener {
    private static Logger log = Logger.getLogger(OpcDemoPerfTestMethodManagerListener.class);

    OpcDemoNodeManager opcDemoNodeManager;
    final private UaNode method;
    final private PlainVariable<Integer> opcVar;

    public OpcDemoPerfTestMethodManagerListener(OpcDemoNodeManager opcDemoNodeManager,
                                                PlainVariable<Integer> opcVar, UaNode method) {
        this.opcDemoNodeManager = opcDemoNodeManager;
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
            MethodManager.checkInputArguments(new Class[]{Integer.class, Integer.class}, inputArguments,
                    inputArgumentResults, inputArgumentDiagnosticInfos, false);
            int count, delay;
            try {
                count = inputArguments[0].intValue();
                delay = inputArguments[1].intValue();
            } catch (ClassCastException e) {
                throw inputError(1, e.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
            }

            simulateBackgroundWork(count, delay);

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

    // Starts a background thread that continuously sets the OPC variable
    private void simulateBackgroundWork(final int count, final int delay) {
        opcVar.setMinimumSamplingInterval((double)delay);
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int i = opcVar.getCurrentValue() + 1;
                opcVar.setCurrentValue(i);
                log.info("set perfTestVar to " + i);
                if (i >= count) timer.cancel();
            }
        }, delay, delay);
    }
}
