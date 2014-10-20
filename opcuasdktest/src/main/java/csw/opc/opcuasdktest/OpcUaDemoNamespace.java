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

package csw.opc.opcuasdktest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.inductiveautomation.opcua.sdk.core.AccessLevel;
import com.inductiveautomation.opcua.sdk.core.AttributeIds;
import com.inductiveautomation.opcua.sdk.core.ValueRank;
import com.inductiveautomation.opcua.sdk.server.OpcUaServer;
import com.inductiveautomation.opcua.sdk.server.api.*;
import com.inductiveautomation.opcua.sdk.server.api.nodes.*;
import com.inductiveautomation.opcua.sdk.server.api.nodes.UaVariableNode.UaVariableNodeBuilder;
import com.inductiveautomation.opcua.sdk.server.util.SubscriptionModel;
import com.inductiveautomation.opcua.stack.core.Identifiers;
import com.inductiveautomation.opcua.stack.core.StatusCodes;
import com.inductiveautomation.opcua.stack.core.UaException;
import com.inductiveautomation.opcua.stack.core.types.builtin.*;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UInteger;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UShort;
import com.inductiveautomation.opcua.stack.core.types.enumerated.NodeClass;
import com.inductiveautomation.opcua.stack.core.types.enumerated.TimestampsToReturn;
import com.inductiveautomation.opcua.stack.core.types.structured.Argument;
import com.inductiveautomation.opcua.stack.core.types.structured.ReadValueId;
import com.inductiveautomation.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

public class OpcUaDemoNamespace implements Namespace {

    public static final String NamespaceUri = "http://www.tmt.org/opcua/demoAddressSpace";
    public static final UShort NamespaceIndex = ushort(2);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<NodeId, UaNode> nodes = Maps.newConcurrentMap();

    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;


    public OpcUaDemoNamespace(OpcUaServer server) {
        this.server = server;

        // folder
        NodeId folderNodeId = new NodeId(NamespaceIndex, "/OpcDemoObjectsFolder");

        UaNode folderNode = UaObjectNode.builder()
                .setNodeId(folderNodeId)
                .setBrowseName(new QualifiedName(NamespaceIndex, "OpcDemoObjects"))
                .setDisplayName(LocalizedText.english("OpcDemoObjects"))
                .setTypeDefinition(Identifiers.FolderType)
                .build();

        nodes.put(folderNodeId, folderNode);

        try {
            server.getUaNamespace().addReference(
                    Identifiers.ObjectsFolder,
                    Identifiers.Organizes,
                    true, server.getServerTable().getUri(0),
                    folderNodeId.expanded(), NodeClass.Object);
        } catch (UaException e) {
            logger.error("Error adding reference to Connections folder.", e);
        }

        // method folder
        NodeId methodFolderId = new NodeId(NamespaceIndex, folderNodeId.getIdentifier() + "/" + "Methods");

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


        subscriptionModel = new SubscriptionModel(this, server.getExecutorService());

        addPlainVariable("Filter", Identifiers.String, new Variant("None"), folderNode, methodFolder);
        addPlainVariable("Disperser", Identifiers.String, new Variant("Mirror"), folderNode, methodFolder);
    }

    // OPC UA Variable to hold the current filter value
    private void addPlainVariable(String name, NodeId typeId, Variant variant,
                                  UaNode folderNode, UaNode methodFolder) {
        UaVariableNode node = new UaVariableNodeBuilder()
                .setNodeId(new NodeId(NamespaceIndex, folderNode.getNodeId().getIdentifier() + "/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.ReadOnly)))
                .setBrowseName(new QualifiedName(NamespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDescription(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
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

        addMethodNode(name, node, methodFolder);
    }


    // Adds an OPC UA method set$name that sets the variable (just to test using methods)
    private void addMethodNode(String name, UaVariableNode variableNode, UaNode methodFolder) {

        String methodName = "set" + name;
        UaMethodNode methodNode = UaMethodNode.builder()
                .setNodeId(new NodeId(NamespaceIndex, methodFolder.getNodeId().getIdentifier() + "/" + methodName))
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

        methodNode.setInputArguments(new Argument[]{input}, nodes::put);
        methodNode.setOutputArguments(new Argument[]{output}, nodes::put);
        methodNode.setInvocationHandler(new SetVariableInvocationHandler(name, variableNode));

        nodes.put(methodNode.getNodeId(), methodNode);

        Reference folder2method = new Reference(
                methodFolder.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
        );

        methodFolder.addReference(folder2method);
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

            DataValue value = (node != null) ?
                    node.readAttribute(id.getAttributeId()) :
                    new DataValue(StatusCodes.Bad_NodeIdUnknown);

            value = id.getAttributeId().intValue() == AttributeIds.Value ?
                    DataValue.derivedValue(value, timestamps) :
                    DataValue.derivedNonValue(value, timestamps);

            results.add(value);
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

                node.writeAttribute(writeValue.getAttributeId(), writeValue.getValue(), server.getNamespaceManager());

                results.add(StatusCode.Good);
            } catch (UaException e) {
                results.add(e.getStatusCode());
            }
        }

        future.complete(results);
    }

    @Override
    public void onSampledItemsCreated(List<SampledItem> sampledItems) {
        sampledItems.stream().forEach(item -> {
            if (item.getSamplingInterval() < 100) item.setSamplingInterval(100.0);
        });

        subscriptionModel.onSampledItemsCreated(sampledItems);
    }

    @Override
    public void onSampledItemsModified(List<SampledItem> sampledItems) {
        sampledItems.stream().forEach(item -> {
            if (item.getSamplingInterval() < 100) item.setSamplingInterval(100.0);
        });

        subscriptionModel.onSampledItemsModified(sampledItems);
    }

    @Override
    public void onSampledItemsDeleted(List<SampledItem> sampledItems) {
        subscriptionModel.onSampledItemsDeleted(sampledItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        UaNode node = nodes.get(methodId);

        if (node instanceof UaMethodNode) {
            return ((UaMethodNode) node).getHandler();
        } else {
            return Optional.empty();
        }
    }
}
