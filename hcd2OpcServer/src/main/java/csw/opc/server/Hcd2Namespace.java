package csw.opc.server;

/*
 * Modified from com.digitalpetri.opcua.server.ctt.CttNamespace for test.
 */

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.digitalpetri.opcua.sdk.core.AccessLevel;
import com.digitalpetri.opcua.sdk.core.Reference;
import com.digitalpetri.opcua.sdk.server.OpcUaServer;
import com.digitalpetri.opcua.sdk.server.api.DataItem;
import com.digitalpetri.opcua.sdk.server.api.MethodInvocationHandler;
import com.digitalpetri.opcua.sdk.server.api.MonitoredItem;
import com.digitalpetri.opcua.sdk.server.api.UaNamespace;
import com.digitalpetri.opcua.sdk.server.model.UaFolderNode;
import com.digitalpetri.opcua.sdk.server.model.UaMethodNode;
import com.digitalpetri.opcua.sdk.server.model.UaNode;
import com.digitalpetri.opcua.sdk.server.model.UaObjectNode;
import com.digitalpetri.opcua.sdk.server.model.UaVariableNode;
import com.digitalpetri.opcua.sdk.server.model.UaVariableNode.UaVariableNodeBuilder;
import com.digitalpetri.opcua.sdk.server.util.SubscriptionModel;
import com.digitalpetri.opcua.stack.core.Identifiers;
import com.digitalpetri.opcua.stack.core.StatusCodes;
import com.digitalpetri.opcua.stack.core.UaException;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.ExpandedNodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.LocalizedText;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.QualifiedName;
import com.digitalpetri.opcua.stack.core.types.builtin.StatusCode;
import com.digitalpetri.opcua.stack.core.types.builtin.Variant;
import com.digitalpetri.opcua.stack.core.types.builtin.unsigned.UShort;
import com.digitalpetri.opcua.stack.core.types.enumerated.NodeClass;
import com.digitalpetri.opcua.stack.core.types.enumerated.TimestampsToReturn;
import com.digitalpetri.opcua.stack.core.types.structured.ReadValueId;
import com.digitalpetri.opcua.stack.core.types.structured.WriteValue;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.digitalpetri.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public class Hcd2Namespace implements UaNamespace {

    public static final String NAMESPACE_URI = "urn:digitalpetri:opcua:hcd2-namespace";

    public static final String NAMESPACE_PREFIX = "/Static/AllProfiles/Scalar/";

    public static final String[] FILTERS = new String[]{
            "None",
            "g_G0301",
            "r_G0303",
            "i_G0302",
            "z_G0304",
            "Z_G0322",
            "Y_G0323",
            "GG455_G0305",
            "OG515_G0306",
            "RG610_G0307",
            "CaT_G0309",
            "Ha_G0310",
            "HaC_G0311",
            "DS920_G0312",
            "SII_G0317",
            "OIII_G0318",
            "OIIIC_G0319",
            "HeII_G0320",
            "HeIIC_G0321",
            "HartmannA_G0313 + r_G0303",
            "HartmannB_G0314 + r_G0303",
            "g_G0301 + GG455_G0305",
            "g_G0301 + OG515_G0306",
            "r_G0303 + RG610_G0307",
            "i_G0302 + CaT_G0309",
            "z_G0304 + CaT_G0309",
            "u_G0308"
    };

    public static final String[] DISPERSERS = new String[]{
            "Mirror",
            "B1200_G5301",
            "R831_G5302",
            "B600_G5303",
            "B600_G5307",
            "R600_G5304",
            "R400_G5305",
            "R150_G5306"
    };


    // --

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<NodeId, UaNode> nodes = Maps.newConcurrentMap();

    private final UaFolderNode cttFolder;
    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;
    private final UShort namespaceIndex;

    private final NodeId filterNodeId;
    private final NodeId filterPosNodeId;
    private final NodeId disperserNodeId;
    private final NodeId disperserPosNodeId;


    // --

    public Hcd2Namespace(OpcUaServer server, UShort namespaceIndex) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;

        NodeId cttNodeId = new NodeId(namespaceIndex, "HCD2");

        cttFolder = new UaFolderNode(
                this,
                cttNodeId,
                new QualifiedName(namespaceIndex, "HCD2"),
                LocalizedText.english("HCD2"));

        nodes.put(cttNodeId, cttFolder);

        try {
            server.getUaNamespace().addReference(
                    Identifiers.ObjectsFolder,
                    Identifiers.Organizes,
                    true,
                    cttNodeId.expanded(),
                    NodeClass.Object);
        } catch (UaException e) {
            logger.error("Error adding reference to Connections folder.", e);
        }

        subscriptionModel = new SubscriptionModel(server, this);

        addStaticScalarNodes();

        filterNodeId = new NodeId(namespaceIndex, NAMESPACE_PREFIX + "filter");
        filterPosNodeId = new NodeId(namespaceIndex, NAMESPACE_PREFIX + "filterPos");
        disperserNodeId = new NodeId(namespaceIndex, NAMESPACE_PREFIX + "disperser");
        disperserPosNodeId = new NodeId(namespaceIndex, NAMESPACE_PREFIX + "disperserPos");
    }

    // For testing, the filter and disperser are the main variables that
    // are set and read by the clients. The filterPos and dispeserPos variables
    // are supposed to simulate variables that give the current positions as
    // intermediate values while the device is moving to the demand position.
    private static final Object[][] STATIC_SCALAR_NODES = new Object[][]{
            {"filter", Identifiers.String, new Variant("None")},
            {"filterPos", Identifiers.Int32, new Variant(0)},
            {"disperser", Identifiers.String, new Variant("Mirror")},
            {"disperserPos", Identifiers.Int32, new Variant(0)}
    };

    private void addStaticScalarNodes() {
        UaObjectNode folder = addFoldersToRoot(cttFolder, NAMESPACE_PREFIX);

        for (Object[] os : STATIC_SCALAR_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Variant variant = (Variant) os[2];

            UaVariableNode node = new UaVariableNodeBuilder(this)
                    .setNodeId(new NodeId(namespaceIndex, NAMESPACE_PREFIX + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(new QualifiedName(namespaceIndex, name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));

            folder.addReference(new Reference(
                    folder.getNodeId(),
                    Identifiers.Organizes,
                    node.getNodeId().expanded(),
                    node.getNodeClass(),
                    true
            ));

            logger.debug("Added reference: {} -> {}", folder.getNodeId(), node.getNodeId());

            nodes.put(node.getNodeId(), node);
        }
    }

    private UaObjectNode addFoldersToRoot(UaNode root, String path) {
        if (path.startsWith("/")) path = path.substring(1, path.length());
        String[] elements = path.split("/");

        LinkedList<UaObjectNode> folderNodes = processPathElements(
                Lists.newArrayList(elements),
                Lists.newArrayList(),
                Lists.newLinkedList()
        );

        UaObjectNode firstNode = folderNodes.getFirst();

        if (!nodes.containsKey(firstNode.getNodeId())) {
            nodes.put(firstNode.getNodeId(), firstNode);

            nodes.get(root.getNodeId()).addReference(new Reference(
                    root.getNodeId(),
                    Identifiers.Organizes,
                    firstNode.getNodeId().expanded(),
                    firstNode.getNodeClass(),
                    true
            ));

            logger.debug("Added reference: {} -> {}", root.getNodeId(), firstNode.getNodeId());
        }

        PeekingIterator<UaObjectNode> iterator = Iterators.peekingIterator(folderNodes.iterator());

        while (iterator.hasNext()) {
            UaObjectNode node = iterator.next();

            nodes.putIfAbsent(node.getNodeId(), node);

            if (iterator.hasNext()) {
                UaObjectNode next = iterator.peek();

                if (!nodes.containsKey(next.getNodeId())) {
                    nodes.put(next.getNodeId(), next);

                    nodes.get(node.getNodeId()).addReference(new Reference(
                            node.getNodeId(),
                            Identifiers.Organizes,
                            next.getNodeId().expanded(),
                            next.getNodeClass(),
                            true
                    ));

                    logger.debug("Added reference: {} -> {}", node.getNodeId(), next.getNodeId());
                }
            }
        }

        return folderNodes.getLast();
    }

    private LinkedList<UaObjectNode> processPathElements(List<String> elements, List<String> path, LinkedList<UaObjectNode> nodes) {
        if (elements.size() == 1) {
            String name = elements.get(0);
            String prefix = String.join("/", path) + "/";
            if (!prefix.startsWith("/")) prefix = "/" + prefix;

            UaObjectNode node = UaObjectNode.builder(this)
                    .setNodeId(new NodeId(namespaceIndex, prefix + name))
                    .setBrowseName(new QualifiedName(namespaceIndex, name))
                    .setDisplayName(LocalizedText.english(name))
                    .setTypeDefinition(Identifiers.FolderType)
                    .build();

            nodes.add(node);

            return nodes;
        } else {
            String name = elements.get(0);
            String prefix = String.join("/", path) + "/";
            if (!prefix.startsWith("/")) prefix = "/" + prefix;

            UaObjectNode node = UaObjectNode.builder(this)
                    .setNodeId(new NodeId(namespaceIndex, prefix + name))
                    .setBrowseName(new QualifiedName(namespaceIndex, name))
                    .setDisplayName(LocalizedText.english(name))
                    .setTypeDefinition(Identifiers.FolderType)
                    .build();

            nodes.add(node);
            path.add(name);

            return processPathElements(elements.subList(1, elements.size()), path, nodes);
        }
    }

    @Override
    public UShort getNamespaceIndex() {
        return namespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NAMESPACE_URI;
    }

    @Override
    public void addNode(UaNode node) {
        nodes.put(node.getNodeId(), node);
    }

    @Override
    public Optional<UaNode> getNode(NodeId nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public Optional<UaNode> getNode(ExpandedNodeId nodeId) {
        return nodeId.local().flatMap(this::getNode);
    }

    @Override
    public Optional<UaNode> removeNode(NodeId nodeId) {
        return Optional.ofNullable(nodes.remove(nodeId));
    }

    @Override
    public CompletableFuture<List<Reference>> getReferences(NodeId nodeId) {
        UaNode node = nodes.get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            CompletableFuture<List<Reference>> f = new CompletableFuture<>();
            f.completeExceptionally(new UaException(StatusCodes.Bad_NodeIdUnknown));
            return f;
        }
    }

    @Override
    public void read(ReadContext context, Double maxAge, TimestampsToReturn timestamps, List<ReadValueId> readValueIds) {
        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId id : readValueIds) {
            UaNode node = nodes.get(id.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                        id.getAttributeId().intValue(),
                        timestamps,
                        id.getIndexRange());

                if (logger.isTraceEnabled()) {
                    Variant variant = value.getValue();
                    Object o = variant != null ? variant.getValue() : null;
                    logger.trace("Read value={} from attributeId={} of {}",
                            o, id.getAttributeId(), id.getNodeId());
                }

                results.add(value);
            } else {
                results.add(new DataValue(new StatusCode(StatusCodes.Bad_NodeIdUnknown)));
            }
        }

        context.complete(results);
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            try {
                Variant variant = writeValue.getValue().getValue();
                String value = variant != null ? variant.getValue().toString() : null;
                // XXX React when the filter value is set
                if (writeValue.getNodeId().equals(filterNodeId)) {
                    simulateDemandDelay("filter", value, filterPosNodeId, FILTERS);
                } else if (writeValue.getNodeId().equals(disperserNodeId)) {
                    simulateDemandDelay("disperser", value, disperserPosNodeId, DISPERSERS);
                }

                UaNode node = Optional.ofNullable(nodes.get(writeValue.getNodeId()))
                        .orElseThrow(() -> new UaException(StatusCodes.Bad_NodeIdUnknown));

                node.writeAttribute(
                        server.getNamespaceManager(),
                        writeValue.getAttributeId().intValue(),
                        writeValue.getValue(),
                        writeValue.getIndexRange());

                if (logger.isTraceEnabled()) {
                    logger.trace("Wrote value={} to attributeId={} of {}",
                            value, writeValue.getAttributeId(), writeValue.getNodeId());
                }

                results.add(StatusCode.GOOD);
            } catch (UaException e) {
                results.add(e.getStatusCode());
            }
        }

        context.complete(results);
    }

    // Simulate the filter or disperser wheel turning past different settings on its
    // way to the demand setting...
    private void simulateDemandDelay(String name, String value, NodeId posNodeId, String[] choices) {
        logger.info("Setting " + name + " to = " + value);
        UaVariableNode node = (UaVariableNode)nodes.get(posNodeId);
        int currentPos = (Integer)node.getValue().getValue().getValue();
        while (!choices[currentPos].equals(value)) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                logger.error("Can't sleep", ex);
            }
            currentPos = (currentPos + 1) % choices.length;
            logger.info("Setting " + name + "Pos  to = " + currentPos);
            node.setValue(new DataValue(new Variant(currentPos)));
        }
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        UaNode node = nodes.get(methodId);

        if (node instanceof UaMethodNode) {
            return ((UaMethodNode) node).getInvocationHandler();
        } else {
            return Optional.empty();
        }
    }

}