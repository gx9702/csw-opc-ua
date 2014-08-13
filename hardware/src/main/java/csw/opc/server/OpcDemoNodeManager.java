package csw.opc.server;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.*;
import com.prosysopc.ua.server.*;
import com.prosysopc.ua.server.nodes.*;
import com.prosysopc.ua.types.opcua.AnalogItemType;
import com.prosysopc.ua.types.opcua.server.*;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.common.NamespaceTable;
import org.opcfoundation.ua.core.*;

import com.prosysopc.ua.StatusException;

// Defines a demo node manager that manages a filter and a disperser value.
public class OpcDemoNodeManager extends NodeManagerUaNode {
    private static Logger log = Logger.getLogger(OpcDemoNodeManager.class);
    public static final String NAMESPACE = "http://www.tmt.org/opcua/demoAddressSpace";

    private UaObjectNode device;
    private final OpcDemoEventManagerListener eventManagerListener = new OpcDemoEventManagerListener();

    public OpcDemoNodeManager(UaServer server, String namespaceUri)
            throws StatusException, UaInstantiationException {
        super(server, namespaceUri);
    }


    private void createDemoEventType() throws StatusException {
        int ns = this.getNamespaceIndex();

        NodeId demoEventTypeId = new NodeId(ns, OpcDemoEventType.DEMO_EVENT_ID);
        UaObjectType demoEventType = new UaObjectTypeNode(this, demoEventTypeId,
                "DemoEventType", LocalizedText.NO_LOCALE);
        getServer().getNodeManagerRoot().getType(Identifiers.BaseEventType)
                .addSubType(demoEventType);

        NodeId demoVariableId = new NodeId(ns, OpcDemoEventType.DEMO_VARIABLE_ID);
        PlainVariable<Integer> demoVariable = new PlainVariable<Integer>(this,
                demoVariableId, OpcDemoEventType.DEMO_VARIABLE_NAME,
                LocalizedText.NO_LOCALE);
        demoVariable.setDataTypeId(Identifiers.Int32);
        // The modeling rule must be defined for the mandatory elements to
        // ensure that the event instances will also get the elements.
        demoVariable.addModellingRule(ModellingRule.Mandatory);
        demoEventType.addComponent(demoVariable);

        NodeId demoPropertyId = new NodeId(ns, OpcDemoEventType.DEMO_PROPERTY_ID);
        PlainProperty<Integer> demoProperty = new PlainProperty<Integer>(this,
                demoPropertyId, OpcDemoEventType.DEMO_PROPERTY_NAME,
                LocalizedText.NO_LOCALE);
        demoProperty.setDataTypeId(Identifiers.String);
        demoProperty.addModellingRule(ModellingRule.Mandatory);
        demoEventType.addProperty(demoProperty);

        getServer().registerClass(OpcDemoEventType.class, demoEventTypeId);
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
        CallableListener methodManagerListener = new OpcDemoMethodManagerListener(this, name, opcVar, method);
        MethodManagerUaNode m = (MethodManagerUaNode) this.getMethodManager();
        m.addCallListener(methodManagerListener);
    }


    // Creates a 'perfTest' OPC method that continually sets the pertTestVar OPC variable to different values in a
    // background thread.
    // The perfTest method takes three int arguments:
    // * The number of times to increment the variable,
    // * The delay in μs between settings.
    // * The variable to set: 1: scalar value, 2: analog array, 3: static array
    private void createPerfTestMethodNode(PlainVariable<Integer> perfTestVar,
                                          AnalogItemType analogArrayNode, UaVariableNode staticArrayNode) throws StatusException {
        String methodName = "perfTest";
        int ns = this.getNamespaceIndex();
        final NodeId methodId = new NodeId(ns, methodName);
        PlainMethod method = new PlainMethod(this, methodId, methodName, Locale.ENGLISH);

        Argument[] inputs = new Argument[3];
        inputs[0] = new Argument();
        inputs[0].setName("count");
        inputs[0].setDataType(Identifiers.Integer);
        inputs[0].setValueRank(ValueRanks.Scalar);
        inputs[0].setArrayDimensions(null);
        inputs[0].setDescription(new LocalizedText("Loop count", Locale.ENGLISH));

        inputs[1] = new Argument();
        inputs[1].setName("delay");
        inputs[1].setDataType(Identifiers.Integer);
        inputs[1].setValueRank(ValueRanks.Scalar);
        inputs[1].setArrayDimensions(null);
        inputs[1].setDescription(new LocalizedText("Delay in μs", Locale.ENGLISH));

        inputs[2] = new Argument();
        inputs[2].setName("testNo");
        inputs[2].setDataType(Identifiers.Integer);
        inputs[2].setValueRank(ValueRanks.Scalar);
        inputs[2].setArrayDimensions(null);
        inputs[2].setDescription(new LocalizedText("Test to run (1, 2 or 3)", Locale.ENGLISH));

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
        CallableListener methodManagerListener = new OpcDemoPerfTestMethodManagerListener(this, perfTestVar,
                analogArrayNode, staticArrayNode, method);
        MethodManagerUaNode m = (MethodManagerUaNode) this.getMethodManager();
        m.addCallListener(methodManagerListener);
    }

    private AnalogItemType createAnalogItem(String dataTypeName, NodeId dataTypeId, Object initialValue, UaNode folder)
            throws NodeBuilderException, StatusException {

        // Configure the optional nodes using a NodeBuilderConfiguration
        NodeBuilderConfiguration conf = new NodeBuilderConfiguration();

        // You can use NodeIds to define Optional nodes (good for standard UA
        // nodes as they always have namespace index of 0)
        conf.addOptional(Identifiers.AnalogItemType_EngineeringUnits);

        // You can also use ExpandedNodeIds with NamespaceUris if you don't know
        // the namespace index.
        conf.addOptional(new ExpandedNodeId(NamespaceTable.OPCUA_NAMESPACE,
                Identifiers.AnalogItemType_InstrumentRange.getValue()));

        // You can also use the BrowsePath from the type if you like (the type's
        // BrowseName is not included in the path, so this configuration will
        // apply to any type which has the same path)
        // You can use Strings for 0 namespace index, QualifiedNames for 1-step
        // paths and BrowsePaths for full paths
        // Each type interface has constants for it's structure (1-step deep)
        conf.addOptional(AnalogItemType.DEFINITION);

        // Use the NodeBuilder to create the node
        final AnalogItemType node = createNodeBuilder(AnalogItemType.class, conf)
                .setName(dataTypeName + "AnalogItem").build();

        node.setDefinition("Sample AnalogItem of type " + dataTypeName);
        node.setDataTypeId(dataTypeId);
        node.setValueRank(ValueRanks.Scalar);

        node.setEngineeringUnits(new EUInformation("http://www.example.com", 3,
                new LocalizedText("kg", LocalizedText.NO_LOCALE),
                new LocalizedText("kilogram", Locale.ENGLISH)));

        node.setEuRange(new Range(0.0, 100000.0));
        node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD,
                DateTime.currentTime(), DateTime.currentTime()));
        folder.addReference(node, Identifiers.HasComponent, false);
        return node;
    }

    private AnalogItemType createAnalogItemArray(String dataTypeName, NodeId dataType, Object initialValue, UaNode folder)
            throws StatusException, NodeBuilderException {
        AnalogItemType node = createAnalogItem(dataTypeName + "Array", dataType, initialValue, folder);
        node.setValueRank(ValueRanks.OneDimension);
        node.setArrayDimensions(new UnsignedInteger[]{UnsignedInteger
                .valueOf(Array.getLength(initialValue))});
        return node;
    }

    private UaVariableNode createStaticArrayVariable(String dataTypeName, NodeId dataType, Object initialValue,
                                           UaNode folder) throws StatusException {
        final NodeId nodeId = new NodeId(this.getNamespaceIndex(), dataTypeName);
        UaType type = this.getServer().getNodeManagerRoot().getType(dataType);
        UaVariableNode node = new CacheVariable(this, nodeId, dataTypeName, Locale.ENGLISH);
        node.setDataType(type);
        node.setTypeDefinition(type);
        node.setValueRank(ValueRanks.OneDimension);
        node.setArrayDimensions(new UnsignedInteger[] { UnsignedInteger.valueOf(Array.getLength(initialValue)) });

        node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD, new DateTime(), new DateTime()));
        folder.addReference(node, Identifiers.HasComponent, false);
        return node;
    }

    private void createAddressSpace() throws StatusException,
            UaInstantiationException, NodeBuilderException {

        this.getEventManager().setListener(eventManagerListener);

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
        disperser.setAccessLevel(AccessLevel.READONLY);
        disperser.setCurrentValue("Mirror");


        objectsFolder.addReference(device, Identifiers.HasNotifier, false);

        createDemoEventType();


        // OPC UA methods to set the filter and disperser
        createMethodNode(filter);
        createMethodNode(disperser);

        addPerfTest(ns, objectsFolder);
    }

    // Adds vars and a method for use in performance tests
    private void addPerfTest(int ns, FolderTypeNode objectsFolder) throws StatusException, NodeBuilderException {
        PlainVariable<Integer> perfTestVar = new PlainVariable<>(this, new NodeId(ns, "perfTestVar"), "perfTestVar",
                LocalizedText.NO_LOCALE);
        perfTestVar.setDataTypeId(Identifiers.Integer);
        perfTestVar.setTypeDefinitionId(Identifiers.BaseDataVariableType);
        device.addComponent(perfTestVar);
        perfTestVar.setAccessLevel(AccessLevel.READONLY);
        perfTestVar.setCurrentValue(-1);

        // Add analog and static array vars
        final Integer[] ar = new Integer[10000];
        for (int i = 0; i < ar.length; i++) ar[i] = i;

        final AnalogItemType analogArrayNode = createAnalogItemArray("Int32", Identifiers.Int32, ar, objectsFolder);
        final UaVariableNode staticArrayNode = createStaticArrayVariable("StaticInt32Array", Identifiers.Int32, ar, objectsFolder);

        device.addComponent(analogArrayNode);
        device.addComponent(staticArrayNode);

        analogArrayNode.setAccessLevel(AccessLevel.READONLY);
        staticArrayNode.setAccessLevel(AccessLevel.READONLY);

        // Add method that starts the timer
        createPerfTestMethodNode(perfTestVar, analogArrayNode, staticArrayNode);
    }

    @Override
    protected void init() throws StatusException, UaNodeFactoryException {
        super.init();
        try {
            createAddressSpace();
        } catch (NodeBuilderException e) {
            e.printStackTrace();
        }
    }

}
