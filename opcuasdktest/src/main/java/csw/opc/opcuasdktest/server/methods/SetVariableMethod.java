package csw.opc.opcuasdktest.server.methods;

import com.inductiveautomation.opcua.sdk.server.model.UaVariableNode;
import com.inductiveautomation.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import com.inductiveautomation.opcua.sdk.server.util.UaInputArgument;
import com.inductiveautomation.opcua.sdk.server.util.UaMethod;
import com.inductiveautomation.opcua.sdk.server.util.UaOutputArgument;
import com.inductiveautomation.opcua.stack.core.types.builtin.DataValue;
import com.inductiveautomation.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetVariableMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private UaVariableNode variableNode;

    public SetVariableMethod(String name, UaVariableNode variableNode) {
        this.name = name;
        this.variableNode = variableNode;
    }

    @UaMethod
    public void invoke(
            AnnotationBasedInvocationHandler.InvocationContext context,

            @UaInputArgument(
                    name = "value",
                    description = "A value.")
            String value,

            @UaOutputArgument(
                    name = "result",
                    description = "True if the value was valid and the variable was set")
            AnnotationBasedInvocationHandler.Out<Boolean> result) {

        logger.debug(String.format("Invoking set%s method of Object '%s'",
                name, context.getObjectNode().getBrowseName().getName()));

        variableNode.setValue(new DataValue(new Variant(value)));
        result.set(true);
    }
}
