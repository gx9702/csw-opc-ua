package csw.opc.opcuasdktest.server;

import com.inductiveautomation.opcua.sdk.server.api.MethodInvocationHandler;
import com.inductiveautomation.opcua.sdk.server.nodes.UaVariableNode;
import com.inductiveautomation.opcua.stack.core.StatusCodes;
import com.inductiveautomation.opcua.stack.core.types.builtin.DataValue;
import com.inductiveautomation.opcua.stack.core.types.builtin.DiagnosticInfo;
import com.inductiveautomation.opcua.stack.core.types.builtin.StatusCode;
import com.inductiveautomation.opcua.stack.core.types.builtin.Variant;
import com.inductiveautomation.opcua.stack.core.types.structured.CallMethodRequest;
import com.inductiveautomation.opcua.stack.core.types.structured.CallMethodResult;

import java.util.concurrent.CompletableFuture;


/**
 * Handler for the setFilter and setDisperser methods
 */
class SetVariableInvocationHandler implements MethodInvocationHandler {

    private String name;
    private UaVariableNode variableNode;

    public SetVariableInvocationHandler(String name, UaVariableNode variableNode) {
        this.name = name;
        this.variableNode = variableNode;
    }

    @Override
    public void invoke(CallMethodRequest request, CompletableFuture<CallMethodResult> result) {
        Variant[] inputs = request.getInputArguments();

        if (inputs.length != 1) {
            result.complete(new CallMethodResult(
                    new StatusCode(StatusCodes.Bad_ArgumentsMissing),
                    new StatusCode[0],
                    new DiagnosticInfo[0],
                    new Variant[0]
            ));
        }

        Object value = inputs[0].getValue();

        if (value instanceof String) {
            String s = (String)value;
            // XXX TODO check for valid value ...
            variableNode.setValue(new DataValue(new Variant(s)));
            result.complete(new CallMethodResult(
                    StatusCode.Good,
                    new StatusCode[]{StatusCode.Good},
                    new DiagnosticInfo[0],
                    new Variant[]{new Variant(true)}
            ));
        } else {
            result.complete(new CallMethodResult(
                    new StatusCode(StatusCodes.Bad_TypeMismatch),
                    new StatusCode[]{new StatusCode(StatusCodes.Bad_TypeMismatch)},
                    new DiagnosticInfo[0],
                    new Variant[0]
            ));
        }
    }
}
