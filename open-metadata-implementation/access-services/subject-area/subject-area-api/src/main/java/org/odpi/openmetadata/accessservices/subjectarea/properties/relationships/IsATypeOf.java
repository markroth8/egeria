/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
// This is a generated file - do not edit - changes should be made to the templates amd/or generator to generate this file with changes.

package org.odpi.openmetadata.accessservices.subjectarea.properties.relationships;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.odpi.openmetadata.accessservices.subjectarea.properties.enums.TermRelationshipStatus;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.graph.Relationship;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.graph.RelationshipEnd;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.graph.RelationshipType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.RelationshipEndCardinality;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Defines an inheritance relationship between two spine objects.
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsATypeOf extends Relationship {
    private String description = "Defines an inheritance relationship between two spine objects.";

    /*
     * Set up end 1.
     */
     private static final String END_1_NODE_TYPE = "Term";
     private static final String END_1_ATTRIBUTE_NAME = "inherited";
     private static final String END_1_ATTRIBUTE_DESCRIPTION = "Inherited (Subtypes) for this object.";
     private static final RelationshipEndCardinality END_1_CARDINALITY = RelationshipEndCardinality.ANY_NUMBER;
     private static final RelationshipEnd RELATIONSHIP_END_1 = new RelationshipEnd(END_1_NODE_TYPE,
                                                                           END_1_ATTRIBUTE_NAME, END_1_ATTRIBUTE_DESCRIPTION, END_1_CARDINALITY);

    /*
     * Set up end 2.
     */
     private static final String END_2_NODE_TYPE = "Term";
     private static final String END_2_ATTRIBUTE_NAME = "inheritedFrom";
     private static final String END_2_ATTRIBUTE_DESCRIPTION = "Inherited from (Supertypes) for this object.";

     private static final RelationshipEndCardinality END_2_CARDINALITY = RelationshipEndCardinality.ANY_NUMBER;
     private static final RelationshipEnd RELATIONSHIP_END_2 = new RelationshipEnd(END_2_NODE_TYPE,
                                                                           END_2_ATTRIBUTE_NAME, END_2_ATTRIBUTE_DESCRIPTION, END_2_CARDINALITY);

    private TermRelationshipStatus status;
    private String steward;
    private String source;

    public IsATypeOf() {
        super(RelationshipType.IsATypeOf.name(), RELATIONSHIP_END_1, RELATIONSHIP_END_2);
    }

    /**
     * {@literal Description of the relationship. }
     *
     * @return {@code String }
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * {@literal Set the description of the relationship. }
     * @param description {@code String }
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@literal The status of or confidence in the relationship. }
     *
     * @return {@code TermRelationshipStatus }
     */
    public TermRelationshipStatus getStatus() {
        return this.status;
    }

    public void setStatus(TermRelationshipStatus status) {
        this.status = status;
    }

    /**
     * {@literal Person responsible for the relationship. }
     *
     * @return {@code String }
     */
    public String getSteward() {
        return this.steward;
    }

    public void setSteward(String steward) {
        this.steward = steward;
    }

    /**
     * {@literal Person, organization or automated process that created the relationship. }
     *
     * @return {@code String }
     */
    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append(" isATypeOf=");
        sb.append(super.toString(sb));
        sb.append(" isATypeOf Attributes{");
        sb.append("description=").append(this.description).append(",");
        sb.append("steward=").append(this.steward).append(",");
        sb.append("source=").append(this.source).append(",");
        if (status != null) {
            sb.append("status=").append(status.name());
        }
        sb.append("}");
        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}