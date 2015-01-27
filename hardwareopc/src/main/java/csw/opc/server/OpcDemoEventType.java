package csw.opc.server;

import com.prosysopc.ua.TypeDefinitionId;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.types.opcua.server.BaseEventTypeNode;

@TypeDefinitionId(nsu = OpcDemoNodeManager.NAMESPACE, i = OpcDemoEventType.DEMO_EVENT_ID)
public class OpcDemoEventType extends BaseEventTypeNode {

    private static Logger log = Logger.getLogger(OpcDemoEventType.class);

    public static final int DEMO_EVENT_ID = 10000;
    public static final UnsignedInteger DEMO_PROPERTY_ID = UnsignedInteger.valueOf(10001);
    public static final String DEMO_PROPERTY_NAME = "OpcDemoProperty";
    public static final UnsignedInteger DEMO_VARIABLE_ID = UnsignedInteger.valueOf(10002);
    public static final String DEMO_VARIABLE_NAME = "OpcDemoVariable";

    /**
     * The constructor is used by the NodeBuilder and should not be used
     * directly by the application. Therefore we define it with protected
     * visibility.
     */
    protected OpcDemoEventType(NodeManagerUaNode nodeManager, NodeId nodeId,
                               QualifiedName browseName, LocalizedText displayName) {
        super(nodeManager, nodeId, browseName, displayName);
    }

    public String getPropertyValue() {
        UaProperty property = getPropertyNode();
        if (property == null)
            return null;
        return (String) property.getValue().getValue().getValue();
    }

    /**
     * @return the myProperty node object
     */
    public UaProperty getPropertyNode() {
        return getProperty(new QualifiedName(getNodeManager()
                .getNamespaceIndex(), DEMO_PROPERTY_NAME));
    }

    /**
     * @return the value of MyVariable
     */
    public Integer getVariableValue() {
        UaVariable variable = getVariableNode();
        if (variable == null)
            return null;
        return (Integer) variable.getValue().getValue().getValue();
    }

    /**
     * @return the MyVariable node object
     */
    public UaVariable getVariableNode() {
        return (UaVariable) getComponent(new QualifiedName(
                getNodeManager().getNamespaceIndex(), DEMO_VARIABLE_NAME));
    }

    public void setPropertyValue(String myValue) {
        UaProperty property = getPropertyNode();
        if (property != null)
            try {
                property.setValue(myValue);
            } catch (StatusException e) {
                throw new RuntimeException(e);
            }
        else
           log.info("No property");
    }

    public void setVariableValue(int myValue) {
        UaVariable variable = getVariableNode();
        if (variable != null)
            try {
                variable.setValue(myValue);
            } catch (StatusException e) {
                throw new RuntimeException(e);
            }
        else
           log.info("No variable");
    }

}
