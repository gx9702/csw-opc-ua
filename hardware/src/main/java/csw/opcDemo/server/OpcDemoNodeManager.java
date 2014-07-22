package csw.opcDemo.server;

import java.util.Locale;

import com.prosysopc.ua.types.opcua.server.*;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNodeFactoryException;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaObjectType;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.nodes.PlainVariable;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaObjectTypeNode;

/**
 * A sample customized node manager, which actually just overrides the standard
 * NodeManagerUaNode and initializes the nodes for the demo.
 */
public class OpcDemoNodeManager extends NodeManagerUaNode {
    public static final String NAMESPACE = "http://www.tmt.org/opcua/demoAddressSpace";

    public OpcDemoNodeManager(UaServer server, String namespaceUri)
            throws StatusException, UaInstantiationException {
        super(server, namespaceUri);
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
        UaObjectNode device = new UaObjectNode(this, deviceId, "OpcDemoDevice", Locale.ENGLISH);
        device.setTypeDefinition(deviceType);
        objectsFolder.addReference(device, Identifiers.HasComponent, false);

        // Note: Normally there would be range checking for the filter and disperser index (and they would be in
        // different servers), but this is just for testing...
        PlainVariable<Integer> filterIndex = new PlainVariable<>(this, new NodeId(ns, "FilterIndex"), "FilterIndex",
                LocalizedText.NO_LOCALE);
        filterIndex.setDataTypeId(Identifiers.Integer);
        filterIndex.setTypeDefinitionId(Identifiers.BaseDataVariableType);
        device.addComponent(filterIndex);
        filterIndex.setCurrentValue(0);

        PlainVariable<Integer> disperserIndex = new PlainVariable<>(this, new NodeId(ns, "DisperserIndex"), "DisperserIndex",
                LocalizedText.NO_LOCALE);
        disperserIndex.setDataTypeId(Identifiers.Integer);
        disperserIndex.setTypeDefinitionId(Identifiers.BaseDataVariableType);
        device.addComponent(disperserIndex);
        disperserIndex.setCurrentValue(0);
    }

    @Override
    protected void init() throws StatusException, UaNodeFactoryException {
        super.init();
        createAddressSpace();
    }

}
