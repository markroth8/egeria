/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.graph;

import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.odpi.openmetadata.accessservices.assetlineage.model.GraphContext;
import org.odpi.openmetadata.accessservices.assetlineage.model.LineageEntity;
import org.odpi.openmetadata.accessservices.assetlineage.model.LineageRelationship;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.governanceservers.openlineage.ffdc.OpenLineageException;
import org.odpi.openmetadata.governanceservers.openlineage.graph.LineageGraphConnectorBase;
import org.odpi.openmetadata.governanceservers.openlineage.model.LineageVertex;
import org.odpi.openmetadata.governanceservers.openlineage.model.LineageVerticesAndEdges;
import org.odpi.openmetadata.governanceservers.openlineage.model.Scope;
import org.odpi.openmetadata.governanceservers.openlineage.responses.LineageResponse;
import org.odpi.openmetadata.governanceservers.openlineage.responses.LineageVertexResponse;
import org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.factory.GraphFactory;
import org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.model.ffdc.JanusConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.addE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.addV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.bothE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasLabel;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.graph.LineageGraphTransactionManager.commit;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.model.JanusConnectorErrorCode.GRAPH_DISCONNECT_ERROR;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.model.JanusConnectorErrorCode.PROCESS_MAPPING_ERROR;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.ASSET_SCHEMA_TYPE;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.ATTRIBUTE_FOR_SCHEMA;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.DATA_FILE_AND_SUBTYPES;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.EVENT_SCHEMA_ATTRIBUTE;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.LINEAGE_MAPPING;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.NESTED_SCHEMA_ATTRIBUTE;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.PORT_DELEGATION;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.PORT_IMPLEMENTATION;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.PORT_SCHEMA;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.PROCESS;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.PROCESS_PORT;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.RELATIONAL_COLUMN;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.RELATIONAL_TABLE;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.SCHEMA_TYPE_OPTION;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.TABULAR_COLUMN;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.TABULAR_FILE_COLUMN;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.Constants.TOPIC;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.EDGE_LABEL_CLASSIFICATION;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.EDGE_LABEL_COLUMN_DATA_FLOW;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.EDGE_LABEL_INCLUDED_IN;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.EDGE_LABEL_TABLE_DATA_FLOW;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.NODE_LABEL_SUB_PROCESS;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_COLUMN_IN_GUID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_COLUMN_OUT_GUID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_DISPLAY_NAME;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_CREATED_BY;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_CREATE_TIME;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_GUID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_NODE_ID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_UPDATED_BY;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_UPDATE_TIME;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_ENTITY_VERSION;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_INSTANCEPROP_DISPLAY_NAME;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_LABEL;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_METADATA_ID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_PREFIX_ELEMENT;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_PREFIX_INSTANCE_PROPERTY;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_PROCESS_GUID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_KEY_RELATIONSHIP_GUID;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.PROPERTY_NAME_PORT_TYPE;
import static org.odpi.openmetadata.openconnectors.governancedaemonconnectors.openlineageconnectors.janusconnector.utils.GraphConstants.VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME;

@EqualsAndHashCode(callSuper = true)
public class LineageGraphConnector extends LineageGraphConnectorBase {

    public static final String KV = "kv";
    private static final Logger log = LoggerFactory.getLogger(LineageGraphConnector.class);
    public static final String CLOSE_LINEAGE_GRAPH_EXCEPTION = "Exception while closing lineage graph";
    public static final String EXCEPTION_WHILE_CLOSING_LINEAGE_GRAPH_MESSAGE = CLOSE_LINEAGE_GRAPH_EXCEPTION + ": ";
    public static final String CLOSE_LINEAGE_GRAPH_EXCEPTION_MESSAGE = EXCEPTION_WHILE_CLOSING_LINEAGE_GRAPH_MESSAGE;
    public static final String UNABLE_TO_ADD_PROPERTIES = "Unable to add properties on vertex from entity with type ";
    public static final String AND_GUID = " and guid ";
    public static final String UNABLE_TO_CREATE_EDGE_WITH_LABEL = "Unable to create edge with label ";
    public static final String FROM = "from";
    public static final String UNABLE_TO_CREATE_VERTEX_WITH_TYPE = "Unable to create vertex with type ";

    public static final String ASSET_LINEAGE_VARIABLES = "ASSET_LINEAGE_VARIABLES";
    public static final String INPUT_PORT = "INPUT_PORT";
    public static final String OLS_HAS_CORRESPONDING_ELEMENTS = "OLS has added the corresponding subProcess node and edges for input column {}, output column {} and process {} ";
    public static final String VERTICES_AND_RELATIONSHIP_CREATION_EXCEPTION = "An exception happened when trying to create vertices and relationships in LineageGraph. The error is";
    public static final String COULD_NOT_DROP_EDGE = "Could not drop edge ";
    public static final String PROPERTIES = "properties";
    public static final String V = "v";
    public static final String VERTEX_GUID_NOT_FOUND_WHEN_UPDATE = "When trying to update, vertex with guid {} was not found  ";
    public static final String PROPERTIES_UPDATE_EXCEPTION = "An exception happened during update of the properties with exception: ";
    public static final String UNABLE_TO_ADD_PROPERTIES_ON_EDGE_FROM_RELATIONSHIP_WITH_TYPE = "Unable to add properties on edge from relationship with type ";
    public static final String EDGE_GUID_NOT_FOUND_WHEN_UPDATE = "When trying to update, edge with guid {} was not found";
    public static final String CLASSIFICATION_WITH_GUID_NOT_FOUND = "Classification with guid {} not found";
    public static final String S = "s";
    public static final String DELETE_CLASSIFICATION_EXCEPTION = "An exception happened during delete of classifications with error:";
    public static final String VERTEX_WITH_GUID_IS_NOT_PRESENT = "Vertex with guid is not present {}";
    public static final String VERTEX_WITH_GUID_DELETED = "Vertex with guid {} deleted";
    public static final String EDGE_WITH_GUID_DID_NOT_DELETE = "Edge with guid did not delete {}";
    public static final String EDGE_WITH_GUID_DELETED = "Edge with guid {} deleted";
    public static final String EDGE = "edge";
    public static final String VERTEX_NOT_FOUND = "Vertex does not exist with guid {} and display name {}";
    public static final String THE_LINEAGE_GRAPH_COULD_NOT_BE_INITIALIZED_DUE_TO_AN_ERROR = "The Lineage graph could not be initialized due to an error";
    public static final String SOMETHING_WENT_WRONG_WHEN_TRYING_TO_MAP_A_PROCESS = "Something went wrong when trying to map a process.";
    public static final String SOMETHING_WENT_WRONG_WHEN_TRYING_TO_MAP_A_PROCESS_THE_ERROR_IS = SOMETHING_WENT_WRONG_WHEN_TRYING_TO_MAP_A_PROCESS + " The error is: ";
    public static final String FAILED_TO_UPDATE_CLASSIFICATION_WITH_GUID = "failed to update classification with guid";
    private LineageGraphConnectorHelper helper;

    private GraphFactory graphFactory;
    private AuditLog auditLog;

    /**
     * Instantiates the graph based on the configuration passed.
     */
    public void initializeGraphDB(AuditLog auditLog) throws OpenLineageException {
        this.auditLog = auditLog;
        try {
            graphFactory = new GraphFactory();
            graphFactory.openGraph(connectionProperties.getConnectorType().getConnectorProviderClassName(),
                    connectionProperties.getConfigurationProperties(), auditLog);
            this.helper = new LineageGraphConnectorHelper(graphFactory, graphFactory.isSupportingTransactions());

        } catch (JanusConnectorException error) {
            log.error(THE_LINEAGE_GRAPH_COULD_NOT_BE_INITIALIZED_DUE_TO_AN_ERROR, error);
            throw new OpenLineageException(500, error.getReportingClassName(), error.getReportingActionDescription(),
                    error.getReportedErrorMessage(), error.getReportedSystemAction(), error.getReportedUserAction());
        }
    }

    @Override
    public synchronized void disconnect() throws ConnectorCheckedException {
        try {
            graphFactory.closeGraph();
            super.disconnect();
        } catch (ConnectorCheckedException e) {
            log.error(CLOSE_LINEAGE_GRAPH_EXCEPTION_MESSAGE, e);
            this.auditLog.logException(CLOSE_LINEAGE_GRAPH_EXCEPTION, GRAPH_DISCONNECT_ERROR.getMessageDefinition(), e);
            throw e;
        }
    }

    @Override
    public void performLineageGraphJob() {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        try {
            //TODO investigate possibility of adding the PROPERTY_KEY_PROCESS_LINEAGE_COMPLETED_FLAG again
            List<Vertex> vertices = g.V().has(PROPERTY_KEY_LABEL, PROCESS).toList();
            List<String> guidList = new ArrayList<>();
            for (Vertex v : vertices) {
                String s = g.V(v.id()).elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID).toString();
                guidList.add(s);
            }
            commitTransaction(g);

            g = graphFactory.getGraphTraversalSource();
            for (String guid : guidList) {
                findInputColumns(g, guid);
            }
            commitTransaction(g);
        } catch (Exception e) {
            log.error(SOMETHING_WENT_WRONG_WHEN_TRYING_TO_MAP_A_PROCESS_THE_ERROR_IS, e);
            auditLog.logException(SOMETHING_WENT_WRONG_WHEN_TRYING_TO_MAP_A_PROCESS, PROCESS_MAPPING_ERROR.getMessageDefinition(), e);
            rollbackTransaction(g);
        }
    }

    @Override
    public void saveAssetLineageUpdateTime(Long lastUpdateTime) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        GraphTraversal<Vertex, Vertex> lineageVariables = g.V().hasLabel(ASSET_LINEAGE_VARIABLES);
        if (!lineageVariables.hasNext()) {
            g.addV(ASSET_LINEAGE_VARIABLES).property(VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME, lastUpdateTime).next();
        } else {
            g.V(lineageVariables.next().id()).property(VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME, lastUpdateTime).next();
        }
        commitTransaction(g);
    }

    @Override
    public Optional<Long> getAssetLineageUpdateTime() {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        GraphTraversal<Vertex, Map<Object, List<Long>>> lineageVariables = g.V().hasLabel(ASSET_LINEAGE_VARIABLES).valueMap();
        if (lineageVariables.hasNext()) {

            Map<Object, List<Long>> next = lineageVariables.next();
            if (next.containsKey(VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME)) {
                return Optional.of(next.get(VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME).get(0));
            }
        } else {
            return getLineageUpdateTimeFromGraphVariables(g);

        }
        return Optional.empty();
    }

    private Optional<Long> getLineageUpdateTimeFromGraphVariables(GraphTraversalSource g) {
        try {
            return g.getGraph().variables().get(VARIABLE_NAME_ASSET_LINEAGE_LAST_UPDATE_TIME);
        } catch (UnsupportedOperationException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds the paths to the input columns from all the processes in the graph.
     *
     * @param g    - Graph traversal object
     * @param guid - The unique identifier of a Process
     */
    private void findInputColumns(GraphTraversalSource g, String guid) {
        List<Vertex> inputPathsForColumns = g.V().has(PROPERTY_KEY_ENTITY_GUID, guid).out(PROCESS_PORT).out(PORT_DELEGATION)
                .has(PORT_IMPLEMENTATION, PROPERTY_NAME_PORT_TYPE, INPUT_PORT)
                .out(PORT_SCHEMA).out(ATTRIBUTE_FOR_SCHEMA).in(LINEAGE_MAPPING)
                .or(__.in(ATTRIBUTE_FOR_SCHEMA).in(ASSET_SCHEMA_TYPE).has(PROPERTY_KEY_LABEL, P.within(DATA_FILE_AND_SUBTYPES)),
                        __.in(NESTED_SCHEMA_ATTRIBUTE).has(PROPERTY_KEY_LABEL, RELATIONAL_TABLE),
                        __.in(ATTRIBUTE_FOR_SCHEMA).in(SCHEMA_TYPE_OPTION).in(ASSET_SCHEMA_TYPE).has(PROPERTY_KEY_LABEL, TOPIC)).toList();

        commitTransaction(g);
        Vertex process = g.V().has(PROPERTY_KEY_ENTITY_GUID, guid).next();
        inputPathsForColumns.forEach(columnIn -> findOutputColumns(g, columnIn, process));
    }

    /**
     * Finds the output columns of a Process based on the input.
     *
     * @param g        - Graph traversal object
     * @param columnIn - THe vertex of the schema element before processing.
     * @param process  - The vertex of the process.
     */
    private void findOutputColumns(GraphTraversalSource g, Vertex columnIn, Vertex process) {
        List<Vertex> schemaElementVertices = g.V()
                .has(PROPERTY_KEY_ENTITY_GUID, g.V(columnIn.id()).elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID))
                .out(LINEAGE_MAPPING)
                .toList();

        Vertex vertexToStart;
        if (schemaElementVertices != null) {
            List<Vertex> columnOutList = new ArrayList<>();
            for (Vertex schemaElementVertex : schemaElementVertices) {
                vertexToStart = isSchemaElementLinkedToProcess(g, schemaElementVertex, process);
                if (vertexToStart != null) {
                    columnOutList.addAll(findPathForOutputAsset(vertexToStart, g, columnIn));
                }
                for (Vertex columnOut : columnOutList) {
                    addNodesAndEdgesForQuerying(columnIn, columnOut, process);
                }
            }
        }
    }

    /**
     * Returns true if the schemaElementVertex is linked to a process using the lineage related relationships
     *
     * @param g                   - Graph traversal object
     * @param schemaElementVertex - THe vertex of the column before processing.
     * @param process             - The vertex of the process.
     * @return Return the vertex of the initial column
     */
    private Vertex isSchemaElementLinkedToProcess(GraphTraversalSource g, Vertex schemaElementVertex, Vertex process) {
        List<Vertex> initialProcess = g.V(schemaElementVertex.id())
                .bothE(ATTRIBUTE_FOR_SCHEMA)
                .otherV().inE(PORT_SCHEMA).otherV()
                .inE(PORT_DELEGATION).otherV()
                .inE(PROCESS_PORT).otherV()
                .has(PROPERTY_KEY_ENTITY_GUID,
                        g.V(process.id()).elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID)).toList();

        commitTransaction(g);

        if (!initialProcess.isEmpty()) {
            return schemaElementVertex;
        }
        return null;
    }

    /**
     * Returns true if the vertex is null or doesn't have a valid guid
     *
     * @param column - The vertex to of the schema element
     */
    private boolean isColumnEmpty(Vertex column) {
        return (column == null || !StringUtils.isNotEmpty(getGuid(column)));
    }

    /**
     * Add nodes and edges that are going to be used for lineage UI
     *
     * @param columnIn  - The vertex of the input schema element
     * @param columnOut - THe vertex of the output schema element
     * @param process   - The vertex of the process.
     */
    private void addNodesAndEdgesForQuerying(Vertex columnIn, Vertex columnOut, Vertex process) {
        if (isColumnEmpty(columnIn) || isColumnEmpty(columnOut)) {
            return;
        }
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        final String processGuid = getGuid(process);
        final String columnInGuid = getGuid(columnIn);
        final String columnOutGuid = getGuid(columnOut);
        final String processName =
                g.V(process.id()).elementMap(PROPERTY_KEY_INSTANCEPROP_DISPLAY_NAME).toList().get(0).get(PROPERTY_KEY_INSTANCEPROP_DISPLAY_NAME).toString();

        Iterator<Vertex> t = g.V(columnIn.id()).outE(EDGE_LABEL_COLUMN_DATA_FLOW).inV()
                .has(PROPERTY_KEY_COLUMN_OUT_GUID, columnOutGuid)
                .has(PROPERTY_KEY_PROCESS_GUID, processGuid);

        if (!t.hasNext()) {
            Vertex subProcess = g.addV(NODE_LABEL_SUB_PROCESS)
                    .property(PROPERTY_KEY_ENTITY_NODE_ID, UUID.randomUUID().toString())
                    .property(PROPERTY_KEY_DISPLAY_NAME, processName)
                    .property(PROPERTY_KEY_PROCESS_GUID, processGuid)
                    .property(PROPERTY_KEY_COLUMN_IN_GUID, columnInGuid)
                    .property(PROPERTY_KEY_COLUMN_OUT_GUID, columnOutGuid)
                    .next();

            commitTransaction(g);
            g = graphFactory.getGraphTraversalSource();
            g.V(columnIn.id()).addE(EDGE_LABEL_COLUMN_DATA_FLOW).to(__.V(subProcess.id())).next();
            g.V(subProcess.id()).addE(EDGE_LABEL_COLUMN_DATA_FLOW).to(__.V(columnOut.id())).next();
            g.V(subProcess.id()).addE(EDGE_LABEL_INCLUDED_IN).to(__.V(process.id())).next();
            commitTransaction(g);

            addAssetToProcessEdges(columnIn, process, columnOut);

            log.info(OLS_HAS_CORRESPONDING_ELEMENTS, columnInGuid, columnOutGuid, processGuid);
        }
    }

    /**
     * Connects the tables and the processes with edges
     *
     * @param columnIn  - The vertex of the input schema element
     * @param process   - The vertex of the process.
     * @param columnOut - The vertex of the output schema element
     */
    private void addAssetToProcessEdges(Vertex columnIn, Vertex process, Vertex columnOut) {
        GraphTraversalSource localG = graphFactory.getGraphTraversalSource();
        Optional<Vertex> assetIn = getAsset(columnIn);
        if (assetIn.isPresent()) {
            Iterator<Vertex> tableVertex = localG.V(assetIn.get().id()).outE(EDGE_LABEL_TABLE_DATA_FLOW).inV().hasId(process.id());
            if (!tableVertex.hasNext()) {
                localG.V(assetIn.get().id()).addE(EDGE_LABEL_TABLE_DATA_FLOW).to(__.V(process.id())).next();
            }
        }

        Optional<Vertex> assetOut = getAsset(columnOut);
        if (assetOut.isPresent()) {
            Iterator<Vertex> tableVertex = localG.V(assetOut.get().id()).inE(EDGE_LABEL_TABLE_DATA_FLOW).outV().hasId(process.id());
            if (!tableVertex.hasNext()) {
                localG.V(process.id()).addE(EDGE_LABEL_TABLE_DATA_FLOW).to(__.V(assetOut.get().id())).next();
            }
        }
        commitTransaction(localG);
    }

    /**
     * Retrieves vertex--guid property from a vertex
     *
     * @param vertex - the queried vertex
     */
    private String getGuid(Vertex vertex) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        String guid = g.V(vertex.id()).elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID).toString();
        commitTransaction(g);
        return guid;
    }

    /**
     * Retrieves the table or the data file node for a schemaElement
     *
     * @param asset - The vertex of the input schema element
     */
    private Optional<Vertex> getAsset(Vertex asset) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Object vertexGuid = g.V(asset.id()).elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID);
        Vertex graphVertex = g.V().has(PROPERTY_KEY_ENTITY_GUID, vertexGuid).next();
        Object vertexId = graphVertex.id();
        Iterator<Vertex> result = null;
        if (RELATIONAL_COLUMN.equalsIgnoreCase(asset.label())) {
             result = g.V(vertexId).emit().repeat(bothE().otherV().simplePath()).times(1).or(hasLabel(RELATIONAL_TABLE));
        }
        if (TABULAR_COLUMN.equalsIgnoreCase(asset.label()) || TABULAR_FILE_COLUMN.equalsIgnoreCase(asset.label())) {
            result = g.V(vertexId).emit().repeat(bothE().otherV().simplePath()).times(2).
                    or(hasLabel(P.within(DATA_FILE_AND_SUBTYPES)));
        }
        if (EVENT_SCHEMA_ATTRIBUTE.equalsIgnoreCase(asset.label())) {
            result = g.V(vertexId).emit().repeat(bothE().otherV().simplePath()).times(3).or(hasLabel(TOPIC));
        }
        commitTransaction(g);
        if (result == null) {
            return Optional.empty();
        } else {
            return Optional.of(result.next());
        }

    }

    /**
     * Creates vertices and the relationships between them
     *
     * @param graphContext - graph Collection that contains vertices and edges to be stored
     */
    @Override
    public void storeToGraph(Set<GraphContext> graphContext) {

        graphContext.forEach(entry -> {
            try {
                LineageEntity fromEntity = entry.getFromVertex();
                LineageEntity toEntity = entry.getToVertex();

                upsertToGraph(fromEntity, toEntity, entry.getRelationshipType(), entry.getRelationshipGuid());
            } catch (Exception e) {
                log.error(VERTICES_AND_RELATIONSHIP_CREATION_EXCEPTION, e);
            }
        });
    }

    /**
     * Updates the neighbours of a node by removing all the entities that no longer have a relationship with it.
     *
     * @param nodeGUID        - the identifier of the entity that was updated
     * @param neighboursGUIDS - the identifiers of the nodes that have a direct relationship to the entity
     */
    @Override
    public void updateNeighbours(String nodeGUID, Set<String> neighboursGUIDS) {
        List<String> existingNeighboursGUIDs = getAllNeighbours(nodeGUID);
        if (isDifferentGraphContext(neighboursGUIDS, existingNeighboursGUIDs)) {
            removeObsoleteEdges(nodeGUID, neighboursGUIDS, existingNeighboursGUIDs);
        }
    }

    private List<String> getAllNeighbours(String entityGUID) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        GraphTraversal<Vertex, Vertex> exitingVertices = g.V().has(PROPERTY_KEY_ENTITY_GUID, entityGUID).bothE().otherV();

        List<String> existingGUIDs = new ArrayList<>();
        while (exitingVertices.hasNext()) {
            Map<Object, Object> valueMap = g.V(exitingVertices.next().id()).valueMap(PROPERTY_KEY_ENTITY_GUID).next();
            if (valueMap.containsKey(PROPERTY_KEY_ENTITY_GUID)) {
                existingGUIDs.addAll((List<String>) valueMap.get(PROPERTY_KEY_ENTITY_GUID));
            }
        }
        commitTransaction(g);
        return existingGUIDs;
    }

    private boolean isDifferentGraphContext(Set<String> newVertices, List<String> neighboursGUIDs) {
        return neighboursGUIDs.size() != newVertices.size() || !neighboursGUIDs.containsAll(newVertices);
    }

    private void removeObsoleteEdges(String entityGUID, Set<String> newVertices, List<String> neighboursGUIDs) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Function<Edge, GraphTraversal<Edge, Edge>> dropEdgeFromGraph = e -> g.E(e.id()).drop().iterate();

        List<String> obsoleteNeighbours = neighboursGUIDs.stream().filter(existingVertex -> !newVertices.contains(existingVertex)).collect(Collectors.toList());
        if (obsoleteNeighbours.isEmpty()) {
            return;
        }
        Iterator<Edge> existingEdges = g.V().has(PROPERTY_KEY_ENTITY_GUID, entityGUID).bothE();
        while (existingEdges.hasNext()) {
            Edge edge = existingEdges.next();
            List<String> inVertexGuid = (List<String>) g.V(edge.inVertex()).valueMap(PROPERTY_KEY_ENTITY_GUID).next().get(PROPERTY_KEY_ENTITY_GUID);
            List<String> outVertexGuid = (List<String>) g.V(edge.outVertex()).valueMap(PROPERTY_KEY_ENTITY_GUID).next().get(PROPERTY_KEY_ENTITY_GUID);
            if (obsoleteNeighbours.containsAll(inVertexGuid) || obsoleteNeighbours.containsAll(outVertexGuid)) {
                commit(graphFactory, g, dropEdgeFromGraph, edge, COULD_NOT_DROP_EDGE + edge.id());
            }
        }
    }

    private void upsertToGraph(LineageEntity fromEntity, LineageEntity toEntity,
                               final String relationshipLabel, final String relationshipGuid) {

        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Vertex to = addVertex(g, toEntity);
        Vertex from = addVertex(g, fromEntity);
        addEdge(g, relationshipLabel, relationshipGuid, to, from);
        if (graphFactory.isSupportingTransactions()) {
            try {
                g.tx().commit();
            } catch (Exception e) {
                log.error("could not create things upsert to graph vertex {} {}, edge {}",
                        fromEntity.getGuid(), toEntity.getGuid(), relationshipGuid);
                g.tx().rollback();
            }
        }
    }

    private Vertex addVertex(GraphTraversalSource g, LineageEntity toEntity) {
        Map<String, Object> toEntityProperties = getProperties(toEntity);
        GraphTraversal<Object, Vertex> objectVertexGraphTraversal =
                addV(toEntity.getTypeDefName());
        objectVertexGraphTraversal.property(PROPERTY_KEY_ENTITY_GUID, toEntity.getGuid());
        for (Map.Entry<String, Object> propertiesEntry : toEntityProperties.entrySet()) {
            objectVertexGraphTraversal.property(propertiesEntry.getKey(), propertiesEntry.getValue());
        }
        return g.V().has(PROPERTY_KEY_ENTITY_GUID, toEntity.getGuid())
                .fold()
                .coalesce(unfold(), objectVertexGraphTraversal)
                .next();
    }

    private void addEdge(GraphTraversalSource g, String relationshipLabel, String relationshipGuid, Vertex to, Vertex from) {
        g.V(from.id()).as(FROM).V(to.id())
                .coalesce(inE(relationshipLabel).where(outV().as(FROM)),
                        addE(relationshipLabel).from(FROM)).property(PROPERTY_KEY_RELATIONSHIP_GUID, relationshipGuid).next();
    }

    /**
     * Adds or updates properties of a vertex.
     *
     * @param vertex        - vertex to be updated
     * @param lineageEntity - LineageEntity object that has the updates values
     * @return
     */
    private void addOrUpdatePropertiesVertex(GraphTraversalSource g, Vertex vertex, LineageEntity lineageEntity) {
        Map<String, Object> properties = getProperties(lineageEntity);
        g.inject(properties)
                .unfold()
                .as(PROPERTIES)
                .V(vertex.id())
                .as(V)
                .sideEffect(__.select(PROPERTIES)
                        .unfold()
                        .as(KV)
                        .select(V)
                        .property(__.select(KV).by(Column.keys), __.select(KV).by(Column.values))).iterate();
    }

    private Map<String, Object> getProperties(LineageEntity lineageEntity) {
        Map<String, Object> properties = lineageEntity.getProperties().entrySet().stream()
                .filter(e -> StringUtils.isNotEmpty(e.getValue()))
                .collect(Collectors.toMap(
                        e -> PROPERTY_KEY_PREFIX_ELEMENT + PROPERTY_KEY_PREFIX_INSTANCE_PROPERTY + e.getKey(),
                        Map.Entry::getValue));

        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_CREATE_TIME, val -> lineageEntity.getCreateTime());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_CREATED_BY, val -> lineageEntity.getCreatedBy());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_UPDATE_TIME, val -> lineageEntity.getUpdateTime());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_UPDATED_BY, val -> lineageEntity.getUpdatedBy());
        properties.computeIfAbsent(PROPERTY_KEY_LABEL, val -> lineageEntity.getTypeDefName());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_VERSION, val -> lineageEntity.getVersion());
        properties.computeIfAbsent(PROPERTY_KEY_METADATA_ID, val -> lineageEntity.getMetadataCollectionId());
        return properties;
    }

    /**
     * Updates the properties of a vertex
     *
     * @param lineageEntity - LineageEntity object that has the updated values
     */
    @Override
    public void updateEntity(LineageEntity lineageEntity) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Iterator<Vertex> vertex = g.V().has(PROPERTY_KEY_ENTITY_GUID, lineageEntity.getGuid());
        if (!vertex.hasNext()) {
            log.debug(VERTEX_GUID_NOT_FOUND_WHEN_UPDATE, lineageEntity.getGuid());
            rollbackTransaction(g);
            return;
        }
        commit(graphFactory, g, this::addOrUpdatePropertiesVertex, g, vertex.next(), lineageEntity, PROPERTIES_UPDATE_EXCEPTION);
    }

    /**
     * Create or update the relationship between two edges
     * In case the vertexes are not created, they are firstly created
     *
     * @param lineageRelationship relationship to be updated or created
     */
    @Override
    public void upsertRelationship(LineageRelationship lineageRelationship) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        LineageEntity firstEnd = lineageRelationship.getSourceEntity();
        LineageEntity secondEnd = lineageRelationship.getTargetEntity();

        upsertToGraph(firstEnd, secondEnd, lineageRelationship.getTypeDefName(), lineageRelationship.getGuid());

        BiConsumer<GraphTraversalSource, LineageRelationship> addOrUpdatePropertiesEdge = this::addOrUpdatePropertiesEdge;
        commit(graphFactory, g, addOrUpdatePropertiesEdge, g, lineageRelationship,
                UNABLE_TO_ADD_PROPERTIES_ON_EDGE_FROM_RELATIONSHIP_WITH_TYPE +
                        lineageRelationship.getTypeDefName() + AND_GUID + lineageRelationship.getGuid());
    }

    /**
     * Updates the properties of an edge
     *
     * @param lineageRelationship - lineageRelationship object that has the updated values
     */
    @Override
    public void updateRelationship(LineageRelationship lineageRelationship) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Iterator<Edge> edge = g.E().has(PROPERTY_KEY_RELATIONSHIP_GUID, lineageRelationship.getGuid());
        if (!edge.hasNext()) {
            log.debug(EDGE_GUID_NOT_FOUND_WHEN_UPDATE, lineageRelationship.getGuid());
            rollbackTransaction(g);
            return;
        }
        commit(graphFactory, g, this::addOrUpdatePropertiesEdge, g, lineageRelationship, PROPERTIES_UPDATE_EXCEPTION);
    }

    /**
     * Updates the classification of a vertex
     *
     * @param classificationContext - LineageEntity object that has the updated values
     */
    @Override
    public void updateClassification(Set<GraphContext> classificationContext) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        for (GraphContext graphContext : classificationContext) {
            String classificationGuid = graphContext.getToVertex().getGuid();
            Iterator<Vertex> vertexIterator = g.V().has(PROPERTY_KEY_ENTITY_GUID, classificationGuid);
            if (!vertexIterator.hasNext()) {
                log.debug(CLASSIFICATION_WITH_GUID_NOT_FOUND, classificationGuid);
                rollbackTransaction(g);
                continue;
            }

            Vertex storedClassification = vertexIterator.next();
            long storedClassificationVersion = (long) g.V(storedClassification.id()).elementMap(PROPERTY_KEY_ENTITY_VERSION)
                    .toList().get(0).get(PROPERTY_KEY_ENTITY_VERSION);
            if (storedClassificationVersion < graphContext.getToVertex().getVersion()) {
                addOrUpdatePropertiesVertex(g, storedClassification, graphContext.getToVertex());
                commit(graphFactory, g, this::addOrUpdatePropertiesVertex, g, storedClassification, graphContext.getToVertex(),
                        FAILED_TO_UPDATE_CLASSIFICATION_WITH_GUID + classificationGuid);
            }
        }
    }

    /**
     * Deletes a classification of a vertex
     *
     * @param classificationContext - any remaining classifications, empty map if none
     */
    @Override
    public void deleteClassification(Set<GraphContext> classificationContext) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        for (GraphContext context : classificationContext) {
            Graph entityAndClassificationsGraph = (Graph) g.V().has(PROPERTY_KEY_ENTITY_GUID, context.getFromVertex().getGuid())
                    .bothE(EDGE_LABEL_CLASSIFICATION).subgraph(S).cap(S).next();

            Iterator<Edge> edges = entityAndClassificationsGraph.edges();

            while (edges.hasNext()) {
                Edge edge = edges.next();
                String storedClassificationGuid =
                        (String) g.E(edge.id()).inV().elementMap(PROPERTY_KEY_ENTITY_GUID).toList().get(0).get(PROPERTY_KEY_ENTITY_GUID);
                if (context.getToVertex().getGuid().equals(storedClassificationGuid)) {
                    commit(graphFactory, g, this::dropEdge, g, edge, storedClassificationGuid, DELETE_CLASSIFICATION_EXCEPTION);
                    break;
                }
            }
        }
    }

    private void dropEdge(GraphTraversalSource g, Edge edge, String storedClassificationGuid) {
        g.V().has(PROPERTY_KEY_ENTITY_GUID, storedClassificationGuid).drop().iterate();
        g.E(edge.id()).drop().iterate();
    }

    @Override
    public void deleteEntity(String guid, Object version) {
        /*
         * TODO need to take into account the version of the entity once we have history
         * */
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Iterator<Vertex> vertex = g.V().has(PROPERTY_KEY_ENTITY_GUID, guid);

        //TODO add check when we will have classifications to delete classifications first
        if (!vertex.hasNext()) {
            rollbackTransaction(g);
            log.debug(VERTEX_WITH_GUID_IS_NOT_PRESENT, guid);
            return;
        }

        g.V().has(PROPERTY_KEY_ENTITY_GUID, guid).drop().iterate();
        commitTransaction(g);
        log.debug(VERTEX_WITH_GUID_DELETED, guid);
    }

    @Override
    public void deleteRelationship(String guid) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        Iterator<Edge> edge = g.E().has(PROPERTY_KEY_RELATIONSHIP_GUID, guid);
        if (!edge.hasNext()) {
            rollbackTransaction(g);
            log.debug(EDGE_WITH_GUID_DID_NOT_DELETE, guid);
            return;
        }

        g.E(edge.next().id()).drop().iterate();
        commitTransaction(g);
        log.debug(EDGE_WITH_GUID_DELETED, guid);
    }

    /**
     * Adds or updates properties of an edge.
     *
     * @param lineageRelationship - LineageEntity object that has the updates values
     */
    private void addOrUpdatePropertiesEdge(GraphTraversalSource g, LineageRelationship lineageRelationship) {
        Map<String, Object> properties = lineageRelationship.getProperties().entrySet().stream().collect(Collectors.toMap(
                e -> PROPERTY_KEY_PREFIX_ELEMENT + PROPERTY_KEY_PREFIX_INSTANCE_PROPERTY + e.getKey(),
                Map.Entry::getValue
        ));

        properties.values().remove(null);
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_CREATE_TIME, val -> lineageRelationship.getCreateTime());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_CREATED_BY, val -> lineageRelationship.getCreatedBy());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_UPDATE_TIME, val -> lineageRelationship.getUpdateTime());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_UPDATED_BY, val -> lineageRelationship.getUpdatedBy());
        properties.computeIfAbsent(PROPERTY_KEY_LABEL, val -> lineageRelationship.getTypeDefName());
        properties.computeIfAbsent(PROPERTY_KEY_ENTITY_VERSION, val -> lineageRelationship.getVersion());
        properties.computeIfAbsent(PROPERTY_KEY_METADATA_ID, val -> lineageRelationship.getMetadataCollectionId());

        g.inject(properties)
                .as(PROPERTIES)
                .V(lineageRelationship.getSourceEntity().getGuid())
                .outE()
                .where(inV().hasId(lineageRelationship.getTargetEntity().getGuid()))
                .as(EDGE)
                .sideEffect(__.select(PROPERTIES)
                        .unfold()
                        .as(KV)
                        .select(EDGE)
                        .property(__.select(KV).by(Column.keys), __.select(KV).by(Column.values))).iterate();


    }

    /**
     * Returns a list of vertices of output schema elements
     *
     * @param endingVertex   - The vertex that is being checked if it is the output schema element
     * @param g              - Graph traversal object
     * @param startingVertex - The vertex of the input schema element
     * @return Return a list of vertices of output schema elements
     */
    private List<Vertex> findPathForOutputAsset(Vertex endingVertex, GraphTraversalSource g, Vertex startingVertex) {
        //add null check for endingVertex
        if (endingVertex == null) {
            return null;
        }

        List<Vertex> endVertices = new ArrayList<>();
        try {
            if (isEndColumn(g, endingVertex)) {
                endVertices.add(endingVertex);
            } else {
                List<Vertex> nextVertices = g.V(endingVertex.id()).out(LINEAGE_MAPPING).toList();

                for (Vertex vertex : nextVertices) {
                    if (vertex.equals(startingVertex)) {
                        continue;
                    }
                    Optional.ofNullable(findPathForOutputAsset(vertex, g, endingVertex)).ifPresent(endVertices::addAll);
                }

            }
            return endVertices;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(VERTEX_NOT_FOUND, startingVertex.id(),
                        startingVertex.property(PROPERTY_KEY_DISPLAY_NAME).value());
            }
            return null;
        }
    }

    /**
     * Returns true if the vertex is part of an asset
     *
     * @param g      - Graph traversal object
     * @param vertex - The vertex of the schema element
     */
    private boolean isEndColumn(GraphTraversalSource g, Vertex vertex) {
        final String VERTEX = "vertex";

        Iterator<Vertex> end = g.V(vertex.id())
                .or(__.in(ATTRIBUTE_FOR_SCHEMA).in(ASSET_SCHEMA_TYPE).has(PROPERTY_KEY_LABEL, P.within(DATA_FILE_AND_SUBTYPES))
                                .aggregate(org.apache.tinkerpop.gremlin.process.traversal.Scope.local, VERTEX),
                        __.in(NESTED_SCHEMA_ATTRIBUTE).has(PROPERTY_KEY_LABEL, RELATIONAL_TABLE).
                                aggregate(org.apache.tinkerpop.gremlin.process.traversal.Scope.local, VERTEX),
                        __.in(ATTRIBUTE_FOR_SCHEMA).in(SCHEMA_TYPE_OPTION).in(ASSET_SCHEMA_TYPE).has(PROPERTY_KEY_LABEL, TOPIC)
                                .aggregate(org.apache.tinkerpop.gremlin.process.traversal.Scope.local, VERTEX))
                .select(VERTEX).unfold();

        return end.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public LineageResponse lineage(Scope scope, String guid, String displayNameMustContain, boolean includeProcesses) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        GraphTraversal<Vertex, Vertex> vertexGraphTraversal = g.V().has(PROPERTY_KEY_ENTITY_GUID, guid);
        commitTransaction(g);
        if (!vertexGraphTraversal.hasNext()) {
            return new LineageResponse();
        }
        Optional<LineageVerticesAndEdges> lineageVerticesAndEdges = Optional.empty();

        switch (scope) {
            case END_TO_END:
                lineageVerticesAndEdges = helper.endToEnd(guid, includeProcesses);
                break;
            case ULTIMATE_SOURCE:
                lineageVerticesAndEdges = helper.ultimateSource(guid);
                break;
            case ULTIMATE_DESTINATION:
                lineageVerticesAndEdges = helper.ultimateDestination(guid);
                break;
            case VERTICAL:
                lineageVerticesAndEdges = helper.verticalLineage(guid);
                break;
        }
        if (lineageVerticesAndEdges.isPresent() && !displayNameMustContain.isEmpty()) {
            helper.filterDisplayName(lineageVerticesAndEdges.get(), displayNameMustContain);
        }
        return new LineageResponse(lineageVerticesAndEdges.orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LineageVertexResponse getEntityDetails(String guid) {
        LineageVertex lineageVertex = helper.getLineageVertexByGuid(guid);

        return new LineageVertexResponse(lineageVertex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEntityInGraph(String guid) {
        GraphTraversalSource g = graphFactory.getGraphTraversalSource();
        return !g.V().has(PROPERTY_KEY_ENTITY_GUID, guid).toList().isEmpty();
    }

    /**
     * commit the graph transaction if graph transactions are enabled
     *
     * @param g the graph traversal
     */
    private void commitTransaction(GraphTraversalSource g) {
        if (graphFactory.isSupportingTransactions()) {
            g.tx().commit();
        }
    }

    /**
     * rollback the transaction if graph transactions are enabled
     */
    private void rollbackTransaction(GraphTraversalSource g) {
        if (graphFactory.isSupportingTransactions()) {
            g.tx().rollback();
        }
    }
}

