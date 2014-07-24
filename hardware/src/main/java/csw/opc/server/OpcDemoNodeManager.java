package csw.opc.server;

import java.util.Locale;

import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.server.*;
import com.prosysopc.ua.server.nodes.PlainMethod;
import com.prosysopc.ua.types.opcua.server.*;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNodeFactoryException;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaObjectType;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.server.nodes.PlainVariable;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaObjectTypeNode;

// Defines a demo node manager that manages a filter and a disperser value.
public class OpcDemoNodeManager extends NodeManagerUaNode {
    private static Logger log = Logger.getLogger(OpcDemoNodeManager.class);
    public static final String NAMESPACE = "http://www.tmt.org/opcua/demoAddressSpace";

    private UaObjectNode device;

    public OpcDemoNodeManager(UaServer server, String namespaceUri)
            throws StatusException, UaInstantiationException {
        super(server, namespaceUri);
    }

    // Creates a method set$name, with one argument $name and a boolean return value.
    // The method returns immediately and the caller should subscribe to events or variable to follow...
    private void createMethodNode(PlainVariable<String> opcVar) throws StatusException {
        String name = opcVar.getBrowseName().getName();
        String methodName = "set" + name;
        log.info("Creating method " + methodName);
        int ns = this.getNamespaceIndex();
        final NodeId methodId = new NodeId(ns, methodName);
        PlainMethod method = new PlainMethod(this, methodId, methodName, Locale.ENGLISH);

        Argument[] inputs = new Argument[1];
        inputs[0] = new Argument();
        inputs[0].setName(name.toLowerCase());
        inputs[0].setDataType(Identifiers.String);
        inputs[0].setValueRank(ValueRanks.Scalar);
        inputs[0].setArrayDimensions(null);
        inputs[0].setDescription(new LocalizedText("Sets the " + name, Locale.ENGLISH));
        method.setInputArguments(inputs);

        Argument[] outputs = new Argument[1];
        outputs[0] = new Argument();
        outputs[0].setName("result");
        outputs[0].setDataType(Identifiers.Boolean);
        outputs[0].setValueRank(ValueRanks.Scalar);
        outputs[0].setArrayDimensions(null);
        outputs[0].setDescription(new LocalizedText("Return status", Locale.ENGLISH));
        method.setOutputArguments(outputs);

        this.addNodeAndReference(device, method, Identifiers.HasComponent);

        // Create the listener that handles the method calls
        CallableListener methodManagerListener = new OpcDemoMethodManagerListener(name, opcVar, method);
        MethodManagerUaNode m = (MethodManagerUaNode) this.getMethodManager();
        m.addCallListener(methodManagerListener);
    }

    private void createAddressSpace() throws StatusException,
            UaInstantiationException {

        int ns = getNamespaceIndex();

        // UA types and folders which we will use
        final UaObject rootObjectsFolder = getServer().getNodeManagerRoot().getObjectsFolder();
        final UaType baseObjectType = getServer().getNodeManagerRoot().getType(Identifiers.BaseObjectType);

        // Folder for objects
        final NodeId objectsFolderId = new NodeId(ns, "OpcDemoObjectsFolder");
        FolderTypeNode objectsFolder = createInstance(FolderTypeNode.class, "OpcDemoObjects", objectsFolderId);

        this.addNodeAndReference(rootObjectsFolder, objectsFolder, Identifiers.Organizes);

        // Device Type
        final NodeId deviceTypeId = new NodeId(ns, "OpcDemoDeviceType");
        UaObjectType deviceType = new UaObjectTypeNode(this, deviceTypeId, "OpcDemoDeviceType", Locale.ENGLISH);
        this.addNodeAndReference(baseObjectType, deviceType, Identifiers.HasSubtype);

        // Device
        final NodeId deviceId = new NodeId(ns, "OpcDemoDevice");
        device = new UaObjectNode(this, deviceId, "OpcDemoDevice", Locale.ENGLISH);
        device.setTypeDefinition(deviceType);
        objectsFolder.addReference(device, Identifiers.HasComponent, false);

        // OPC UA Variables to hold the current filter and disperser values
        PlainVariable<String> filter = new PlainVariable<>(this, new NodeId(ns, "Filter"), "Filter",
                LocalizedText.NO_LOCALE);
        filter.setDataTypeId(Identifiers.String);
        filter.setTypeDefinitionId(Identifiers.BaseDataVariableType);
        device.addComponent(filter);
        filter.setAccessLevel(AccessLevel.READONLY);
        filter.setCurrentValue("None");

        PlainVariable<String> disperser = new PlainVariable<>(this, new NodeId(ns, "Disperser"), "Disperser",
                LocalizedText.NO_LOCALE);
        disperser.setDataTypeId(Identifiers.String);
        disperser.setTypeDefinitionId(Identifiers.BaseDataVariableType);
        device.addComponent(disperser);
        filter.setAccessLevel(AccessLevel.READONLY);
        disperser.setCurrentValue("Mirror");

        // OPC UA methods to set the filter and disperser
        createMethodNode(filter);
        createMethodNode(disperser);
    }

    @Override
    protected void init() throws StatusException, UaNodeFactoryException {
        super.init();
        createAddressSpace();
    }

}
