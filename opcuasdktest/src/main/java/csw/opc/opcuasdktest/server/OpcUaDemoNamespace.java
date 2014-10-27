/*
 * Copyright 2014 Inductive Automation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csw.opc.opcuasdktest.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.inductiveautomation.opcua.sdk.core.AccessLevel;
import com.inductiveautomation.opcua.sdk.core.Reference;
import com.inductiveautomation.opcua.sdk.core.ValueRank;
import com.inductiveautomation.opcua.sdk.core.nodes.Node;
import com.inductiveautomation.opcua.sdk.server.OpcUaServer;
import com.inductiveautomation.opcua.sdk.server.api.DataItem;
import com.inductiveautomation.opcua.sdk.server.api.MethodInvocationHandler;
import com.inductiveautomation.opcua.sdk.server.api.MonitoredItem;
import com.inductiveautomation.opcua.sdk.server.api.Namespace;
import com.inductiveautomation.opcua.sdk.server.nodes.UaMethodNode;
import com.inductiveautomation.opcua.sdk.server.nodes.UaNode;
import com.inductiveautomation.opcua.sdk.server.nodes.UaObjectNode;
import com.inductiveautomation.opcua.sdk.server.nodes.UaVariableNode;
import com.inductiveautomation.opcua.sdk.server.util.SubscriptionModel;
import com.inductiveautomation.opcua.stack.core.Identifiers;
import com.inductiveautomation.opcua.stack.core.StatusCodes;
import com.inductiveautomation.opcua.stack.core.UaException;
import com.inductiveautomation.opcua.stack.core.types.builtin.DataValue;
import com.inductiveautomation.opcua.stack.core.types.builtin.LocalizedText;
import com.inductiveautomation.opcua.stack.core.types.builtin.NodeId;
import com.inductiveautomation.opcua.stack.core.types.builtin.QualifiedName;
import com.inductiveautomation.opcua.stack.core.types.builtin.StatusCode;
import com.inductiveautomation.opcua.stack.core.types.builtin.Variant;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UInteger;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UShort;
import com.inductiveautomation.opcua.stack.core.types.enumerated.NodeClass;
import com.inductiveautomation.opcua.stack.core.types.enumerated.TimestampsToReturn;
import com.inductiveautomation.opcua.stack.core.types.structured.Argument;
import com.inductiveautomation.opcua.stack.core.types.structured.ReadValueId;
import com.inductiveautomation.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

public class OpcUaDemoNamespace implements Namespace {

    public static final String NamespaceUri = "http://www.tmt.org/opcua/demoAddressSpace";
    public static final UShort NamespaceIndex = ushort(2);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<NodeId, UaNode> nodes = Maps.newConcurrentMap();

    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;


    public OpcUaDemoNamespace(OpcUaServer server, int eventSize, int delay) {
        this.server = server;

        UaNode folderNode = makeFolderNode(Identifiers.ObjectsFolder, "OpcDemoDevice");

        UaNode methodFolder = addMethodFolder(folderNode);

        subscriptionModel = new SubscriptionModel(this, server.getExecutorService());

        addPlainVariable("Filter", Identifiers.String, new Variant("None"),
                folderNode, Optional.of(methodFolder), delay);
        addPlainVariable("Disperser", Identifiers.String, new Variant("Mirror"),
                folderNode, Optional.of(methodFolder), delay);

        // XXX TODO: add events when available in API

        addPerfTest(folderNode, methodFolder, eventSize, delay);
    }

    private UaNode makeFolderNode(NodeId parentFolderId, String name) {
        NodeId folderNodeId = new NodeId(NamespaceIndex, name);

        UaNode folderNode = UaObjectNode.builder()
                .setNodeId(folderNodeId)
                .setBrowseName(new QualifiedName(NamespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(Identifiers.FolderType)
                .build();

        nodes.put(folderNodeId, folderNode);

        try {
            server.getUaNamespace().addReference(
                    parentFolderId,
                    Identifiers.Organizes,
                    true, server.getServerTable().getUri(0),
                    folderNodeId.expanded(), NodeClass.Object);
        } catch (UaException e) {
            logger.error("Error adding reference to Connections folder.", e);
        }

        return folderNode;
    }

    private UaNode addMethodFolder(UaNode folderNode) {
        NodeId methodFolderId = new NodeId(NamespaceIndex, folderNode.getNodeId().getIdentifier() + "/" + "Methods");

        UaObjectNode methodFolder = UaObjectNode.builder()
                .setNodeId(methodFolderId)
                .setBrowseName(new QualifiedName(NamespaceIndex, "Methods"))
                .setDisplayName(LocalizedText.english("Methods"))
                .setTypeDefinition(Identifiers.FolderType)
                .build();

        folderNode.addReference(new Reference(
                methodFolderId,
                Identifiers.Organizes,
                methodFolderId.expanded(),
                methodFolder.getNodeClass(),
                true
        ));

        nodes.put(methodFolderId, methodFolder);

        return methodFolder;
    }

    // OPC UA Variable to hold the current filter value
    private UaVariableNode addPlainVariable(String name, NodeId typeId, Variant variant,
                                  UaNode folderNode, Optional<UaNode> methodFolderOpt,
                                  int delay) {
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder()
//                .setNodeId(new NodeId(NamespaceIndex, folderNode.getNodeId().getIdentifier() + "/" + name))
                .setNodeId(new NodeId(NamespaceIndex, name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.ReadOnly)))
                .setBrowseName(new QualifiedName(NamespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDescription(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setMinimumSamplingInterval(delay/2.0)
                .build();

        node.setValue(new DataValue(variant));

        Reference reference = new Reference(
                folderNode.getNodeId(),
                Identifiers.Organizes,
                node.getNodeId().expanded(),
                node.getNodeClass(),
                true
        );

        nodes.put(node.getNodeId(), node);
        folderNode.addReference(reference);

        if (methodFolderOpt.isPresent()) {
            addMethodNode(name, node, methodFolderOpt.get());
        }

        return node;
    }


    // Adds an OPC UA method set$name that sets the variable (just to test using methods)
    private void addMethodNode(String name, UaVariableNode variableNode, UaNode methodFolder) {

        String methodName = "set" + name;
        UaMethodNode methodNode = UaMethodNode.builder()
//                .setNodeId(new NodeId(NamespaceIndex, methodFolder.getNodeId().getIdentifier() + "/" + methodName))
                .setNodeId(new NodeId(NamespaceIndex, methodName))
                .setBrowseName(new QualifiedName(NamespaceIndex, methodName))
                .setDisplayName(new LocalizedText(null, methodName))
                .setDescription(LocalizedText.english("Sets the value of the '" + name + "' OPC variable"))
                .build();

        Argument input = new Argument(
                "value", Identifiers.String,
                ValueRank.Scalar, new UInteger[0],
                LocalizedText.english("The value."));

        Argument output = new Argument(
                "status", Identifiers.Boolean,
                ValueRank.Scalar, new UInteger[0],
                LocalizedText.english("True if the value was valid."));

        methodNode.setInvocationHandler(new SetVariableInvocationHandler(name, variableNode));

        UaVariableNode inputNode = methodNode.setInputArguments(new Argument[]{input});
        UaVariableNode outputNode = methodNode.setOutputArguments(new Argument[]{output});

        nodes.put(inputNode.getNodeId(), inputNode);
        nodes.put(outputNode.getNodeId(), outputNode);
        nodes.put(methodNode.getNodeId(), methodNode);

        methodFolder.addReference(new Reference(
                methodFolder.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
        ));
    }


    private void addPerfTest(UaNode folderNode, UaNode methodFolder, int eventSize, int delay) {

/**
 * //        opcVar.setMinimumSamplingInterval(Optional.of(delay / 2.0));
 //        staticArrayNode.setMinimumSamplingInterval(Optional.of(delay / 2.0));

 */

        // Add a plain int test var
        UaVariableNode perfTestVar = addPlainVariable("perfTestVar", Identifiers.Integer,
                new Variant(-1), folderNode, Optional.empty(), delay);

        // Add analog and static array vars
        final Integer[] ar = new Integer[10000];
        for (int i = 0; i < ar.length; i++) ar[i] = i;

        final UaVariableNode staticArrayNode = createStaticArrayVariable("StaticInt32Array",
                Identifiers.Int32, ar, folderNode);

        // XXX TODO
        final UaVariableNode analogArrayNode = createStaticArrayVariable("Int32ArrayAnalogItem",
                Identifiers.Int32, ar, folderNode);

        // Add method that starts the timer
        createPerfTestMethodNode(perfTestVar, analogArrayNode, staticArrayNode, methodFolder, eventSize);
    }


    // Creates a 'perfTest' OPC method that continually sets the pertTestVar OPC variable to different values in a
    // background thread.
    // The perfTest method takes three int arguments:
    // * The number of times to increment the variable,
    // * The delay in μs between settings.
    // * The variable to set: 1: scalar value, 2: analog array, 3: static array
    private void createPerfTestMethodNode(UaVariableNode perfTestVar,
                                          UaVariableNode analogArrayNode,
                                          UaVariableNode staticArrayNode,
                                          UaNode methodFolder, int eventSize) {
        String methodName = "perfTest";
        UaMethodNode methodNode = UaMethodNode.builder()
//                .setNodeId(new NodeId(NamespaceIndex, methodFolder.getNodeId().getIdentifier() + "/" + methodName))
                .setNodeId(new NodeId(NamespaceIndex, methodName))
                .setBrowseName(new QualifiedName(NamespaceIndex, methodName))
                .setDisplayName(new LocalizedText(null, methodName))
                .setDescription(LocalizedText.english("Performance test method"))
                .build();

        Argument[] inputs = new Argument[]{
                new Argument("count", Identifiers.Integer, ValueRank.Scalar, new UInteger[0],
                        LocalizedText.english("Loop count.")),
                new Argument("delay", Identifiers.Integer, ValueRank.Scalar, new UInteger[0],
                        LocalizedText.english("Delay in μs.")),
                new Argument("testNo", Identifiers.Integer, ValueRank.Scalar, new UInteger[0],
                        LocalizedText.english("Test to run (1, 2 or 3)."))
        };

        Argument output = new Argument(
                "result", Identifiers.Boolean,
                ValueRank.Scalar, new UInteger[0],
                LocalizedText.english("Return status."));

        methodNode.setInvocationHandler(new PerfTestInvocationHandler(perfTestVar, analogArrayNode, staticArrayNode, eventSize));

        UaVariableNode inputNode = methodNode.setInputArguments(inputs);
        UaVariableNode outputNode = methodNode.setOutputArguments(new Argument[]{output});

        nodes.put(inputNode.getNodeId(), inputNode);
        nodes.put(outputNode.getNodeId(), outputNode);
        nodes.put(methodNode.getNodeId(), methodNode);

        methodFolder.addReference(new Reference(
                methodFolder.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
        ));
    }


    private UaVariableNode createStaticArrayVariable(String name, NodeId typeId, Integer[] array,
                                                     UaNode folder) {
        Variant variant = new Variant(array);

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder()
                .setNodeId(new NodeId(NamespaceIndex, name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.ReadOnly)))
                .setBrowseName(new QualifiedName(NamespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setValueRank(ValueRank.OneDimension)
                .setArrayDimensions(new UInteger[]{uint(array.length)})
                .build();

        node.setValue(new DataValue(variant));

        folder.addReference(new Reference(
                folder.getNodeId(),
                Identifiers.Organizes,
                node.getNodeId().expanded(),
                node.getNodeClass(),
                true
        ));

        nodes.put(node.getNodeId(), node);
        return node;
    }



    @Override
    public UShort getNamespaceIndex() {
        return NamespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NamespaceUri;
    }

    @Override
    public boolean containsNodeId(NodeId nodeId) {
        return nodes.containsKey(nodeId);
    }

    @Override
    public Optional<Node> getNode(NodeId nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public Optional<List<Reference>> getReferences(NodeId nodeId) {
        UaNode node = nodes.get(nodeId);

        if (node != null) {
            return Optional.of(node.getReferences());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void read(List<ReadValueId> readValueIds,
                     Double maxAge,
                     TimestampsToReturn timestamps,
                     CompletableFuture<List<DataValue>> future) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId id : readValueIds) {
            UaNode node = nodes.get(id.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                        id.getAttributeId().intValue(),
                        timestamps,
                        id.getIndexRange()
                );

                if (logger.isTraceEnabled()) {
                    Variant variant = value.getValue();
                    Object o = variant != null ? variant.getValue() : null;
                    logger.trace("Read value={} from attributeId={} of {}",
                            o, id.getAttributeId(), id.getNodeId());
                }

                results.add(value);
            } else {
                results.add(new DataValue(new StatusCode(StatusCodes.Bad_NodeIdInvalid)));
            }
        }

        future.complete(results);
    }

    @Override
    public void write(List<WriteValue> writeValues, CompletableFuture<List<StatusCode>> future) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            try {
                UaNode node = Optional.ofNullable(nodes.get(writeValue.getNodeId()))
                        .orElseThrow(() -> new UaException(StatusCodes.Bad_NodeIdUnknown));

                node.writeAttribute(
                        server.getNamespaceManager(),
                        writeValue.getAttributeId().intValue(),
                        writeValue.getValue(),
                        writeValue.getIndexRange()
                );

                if (logger.isTraceEnabled()) {
                    Variant variant = writeValue.getValue().getValue();
                    Object o = variant != null ? variant.getValue() : null;
                    logger.trace("Wrote value={} to attributeId={} of {}",
                            o, writeValue.getAttributeId(), writeValue.getNodeId());
                }

                results.add(StatusCode.Good);
            } catch (UaException e) {
                results.add(e.getStatusCode());
            }
        }

        future.complete(results);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        dataItems.stream().forEach(item -> {
            if (item.getSamplingInterval() < 100) item.setSamplingInterval(100.0);
        });

        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        dataItems.stream().forEach(item -> {
            if (item.getSamplingInterval() < 100) item.setSamplingInterval(100.0);
        });

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
