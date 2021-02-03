/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.datasets;

import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jBadNodeFormatException;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RegexNode {
    private static final String LABEL = "ArtemisRegexFramework";
    private static final String NAME_PROPERTY = "Name"; // Mandatory
    private static final String REGEXES_PROPERTY = "Regexes"; // List of String // Mandatory
    private static final String INTERNAL_TYPE_PROPERTY = "InternalTypes";
    private static final String FRAMEWORK_PROPERTY = "Framework";
    private static final String CATEGORY_PROPERTY = "Category";

    private Node node;
    private String name;
    private List<String> regexes;
    private List<String> internalTypes;
    private String framework;
    private String category;

    public Node getNode() {
        return node;
    }

    public String getName() {
        return name;
    }

    public List<String> getRegexes() {
        return regexes;
    }

    public String getFramework() {
        return framework;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getInternalTypes() {
        return internalTypes;
    }

    public RegexNode(String name, List<String> regexes, List<String> internalTypes, String framework, String category) {
        this.name = name;
        this.regexes = regexes;
        this.framework = framework;
        this.category = category;
        this.internalTypes = internalTypes;
        this.node = null;
    }

    private Node initNode(Node n) {
        assert(n != null) : "Cannot initialize a null node";
        this.node = n;
        node.setProperty(NAME_PROPERTY, name);
        node.setProperty(REGEXES_PROPERTY, regexes);
        node.setProperty(FRAMEWORK_PROPERTY, framework);
        node.setProperty(CATEGORY_PROPERTY, category);
        node.setProperty(INTERNAL_TYPE_PROPERTY, internalTypes);
        return n;
    }

    /**
     * Get the ID of the parent node
     * @return
     * @throws Neo4jQueryException
     */
    public Long getParentId() throws Neo4jQueryException {
        if(this.node == null) return null;
        Iterator<Relationship> itRel =  this.node.getRelationships(Direction.INCOMING, RelationshipType.withName("INCLUDES")).iterator();
        if(!itRel.hasNext()) return null;
        return itRel.next().getStartNodeId();
    }

    /**
     * Get the list of roots ( not not having parent)
     * @param neo4jAL Neo4j Access Layer
     * @return
     * @throws Neo4jQueryException
     */
    public static List<RegexNode> getRootRegexNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
        String req = String.format("MATCH (n:%1$s) WHERE NOT (n)<-[r:INCLUDES]-(m:%1$s) RETURN n as root", LABEL);

        Result res = neo4jAL.executeQuery(req);
        List<RegexNode> regexNodes = new ArrayList<>();
        while(res.hasNext()) {
            try{
                RegexNode rn = new RegexNode((Node) res.next().get("root"));
                regexNodes.add(rn);
            } catch (Neo4jBadNodeFormatException e) {
                neo4jAL.logError("Failed to instantiate a Regex node", e);
            }

        }

        return regexNodes;
    }

    /**
     * Get the children of this node
     * @return
     */
    public List<RegexNode> getChildren() {
        if(this.node == null) return new ArrayList<>();
        Iterator<Relationship> itRel =  this.node.getRelationships(Direction.OUTGOING, RelationshipType.withName("INCLUDES")).iterator();

        List<RegexNode> returnList = new ArrayList<>();
        while(itRel.hasNext()) {
            Node n = itRel.next().getEndNode();
            try {
                returnList.add(new RegexNode(n));
            } catch (Neo4jBadNodeFormatException e) {
                e.printStackTrace();
            }
        }
        return returnList;
    }


    public RegexNode(Node node) throws Neo4jBadNodeFormatException {
        this.node = node;

        try {

            this.name = (String) node.getProperty(NAME_PROPERTY);
            String[] tempRegexes = (String[]) node.getProperty(REGEXES_PROPERTY);
            this.regexes = List.of(tempRegexes);

            if(!node.hasProperty(FRAMEWORK_PROPERTY)){
                node.setProperty(FRAMEWORK_PROPERTY, "");
            }

            if(!node.hasProperty(CATEGORY_PROPERTY)){
                node.setProperty(CATEGORY_PROPERTY, "");
            }
            this.category = (String) node.getProperty(CATEGORY_PROPERTY);

        } catch (Exception e) {
            throw new Neo4jBadNodeFormatException(String.format("Regex Node with id '%d' is not correctly formatted : %s", node.getId(), e.getLocalizedMessage()), "REGEXxCONS1");
        }
    }

    /**
     * Create a new Regex Node
     * @param neo4jAL Neo4J Acces Layer
     * @param name Name of the regex node
     * @param regexes List of matching regex
     * @param framework Framework associated with the regex
     * @param category Category of the Frameworks
     * @return
     */
    public static RegexNode createRegexNode(Neo4jAL neo4jAL, String name, List<String> regexes, List<String> internalTypes, String framework, String category) {
        RegexNode rn = new RegexNode(name, regexes, internalTypes, framework, category);
        Node node = neo4jAL.getTransaction().createNode(Label.label(LABEL));
        rn.initNode(node);

        return rn;
    }

    /**
     * Get the Id of the parent node (return -1 if no parent was found)
     * @return
     */
    public static Long getParentId(Neo4jAL neo4jAL, RegexNode rn) throws Neo4jQueryException {
        if(rn.node == null) return null;
        String req = String.format("MATCH (n:%1$s)<-[r:INCLUDES]-(m:%1$s) WHERE ID(n)=$idNode RETURN ID(m) as idParent", LABEL);
        Map<String, Object> params = Map.of("idNode", rn.node.getId());

        Result res = neo4jAL.executeQuery(req, params);
        if(!res.hasNext()) return null;

        return (Long) res.next().get("idParent");
    }

    /**
     * Get a regex node by its Id
     * @param neo4jAL Neo4j Access Layer
     * @param idNode Id of the node
     * @return
     * @throws Neo4jQueryException
     */
    public static  RegexNode getRegexNodeById(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException, Neo4jBadNodeFormatException {
        String req = String.format("MATCH (n:%1$s) WHERE ID(n)=$idNode RETURN n as node", LABEL);
        Map<String, Object> params = Map.of("idNode", idNode);

        Result res = neo4jAL.executeQuery(req, params);
        if(!res.hasNext()) return null;

        return new RegexNode((Node) res.next().get("node"));
    }

    /**
     * Get the Id of the node or null of the node doesn't exist
     * @return
     */
    public Long getId() {
        if(node == null) return null;
        return node.getId();
    }

    /**
     * Create a link between two regex nodes ( or merge it, if it already exists)
     * @param neo4jAL Neo4j Access Layer
     * @param idChild Id of the children
     * @param idParent Id of the parent
     * @return The relationship or null if it failed to create it
     * @throws Neo4jQueryException
     */
    public static Relationship linkToParent(Neo4jAL neo4jAL, Long idChild, Long idParent) throws Neo4jQueryException {
        String req = String.format("MATCH (n:%1$s), (m:%1$s) WHERE ID(n)=$idChild AND ID(m)=$idParent MERGE (m)-[r:INCLUDES]->(n) RETURN r as rel", LABEL);
        Map<String, Object> params = Map.of("idChild", idChild, "idParent", idParent);

        Result res = neo4jAL.executeQuery(req, params);

        if(!res.hasNext()) return null;

        return (Relationship) res.next().get("rel");
    }

    /**
     * Remove a Regex node using its Id
     * @param neo4jAL
     * @param idNode
     * @return
     * @throws Neo4jQueryException
     */
    public static boolean removeRegexNode(Neo4jAL neo4jAL, Long idNode) throws Neo4jQueryException {
        String req = String.format("MATCH (n:%1$s) WHERE ID(n)=$idNode DETACH DELETE n RETURN n as node", LABEL);
        Map<String, Object> params = Map.of("idNode", idNode);

        Result res = neo4jAL.executeQuery(req, params);
        return !res.hasNext();
    }

    /**
     * Get all the Regex nodes
     * @param neo4jAL Neo4j Access Layer
     * @return
     * @throws Neo4jQueryException
     */
    public static List<RegexNode> getAllNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
        String req = String.format("MATCH (n:%1$s) RETURN n as node", LABEL);
        Result res = neo4jAL.executeQuery(req);

        List<RegexNode> regexNodes = new ArrayList<>();
        while(res.hasNext()) {
            try{
                RegexNode rn = new RegexNode((Node) res.next().get("node"));
                regexNodes.add(rn);
            } catch (Neo4jBadNodeFormatException e) {
                neo4jAL.logError("Failed to instantiate a Regex node", e);
            }

        }

        return regexNodes;
    }
}
