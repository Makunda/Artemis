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

package com.castsoftware.artemis.sof.famililes;

import com.castsoftware.artemis.config.Configuration;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import java.util.*;
import java.util.stream.Collectors;

public class FamilyTree {

    // Imaging
    private static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
    private static final int MIN_NODE = 3;
    private static final double MIN_PREFIX = 2.2;

    private Log log;

    public class FamilyLeaf {
        public String name;
        public Integer depth;
        public Boolean breakpoint;
        public List<FamilyLeaf> children;
        public List<Node> items;

        public FamilyLeaf(String name, Integer depth, List<FamilyLeaf> children, List<Node> items) {
            this.name = name;
            this.depth = depth;
            this.children = children;
            this.items = items;
            this.breakpoint = false;
        }
    }

    private FamilyLeaf root;

    private int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    /**
     * Compute the Levenshtein distance between two strings/
     * @param x Word 1
     * @param y Word 2
     * @return the Levenshtein modification score
     */
    private int computeLevenshtein(String x, String y) {
        int[][]dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j]= j;
                }
                else if (j == 0) {
                    dp[i][j]= i;
                }
                else {
                    dp[i][j]= min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j]+ 1,
                            dp[i][j - 1]+ 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    /**
     * Explode the name in multiple leafs. If the leafs are too small , they will be merged back in the most similar one.
     * @param prefixLength Length of the prefix
     * @param toTreatNodes Node to inspect
     * @return The list of families found under the nodes to treat
     */
    public Map<String, List<Node>> explodeName(Integer prefixLength, List<Node> toTreatNodes) {
        Map<String, List<Node>> families  = new HashMap<>();

        Node n;
        String nodeName;
        Iterator<Node> itNode = toTreatNodes.listIterator();
        while (itNode.hasNext()) {
            n = itNode.next();

            // Ignore node without name
            if(!n.hasProperty(IMAGING_OBJECT_NAME)) continue;
            nodeName = (String) n.getProperty(IMAGING_OBJECT_NAME);

            // Ignore not relevant names
            if(nodeName.length() <= prefixLength) continue;
            String prefix = nodeName.substring(0, prefixLength);

            // Check if it's a known prefix
            List<Node> list = families.getOrDefault(prefix, new ArrayList<>());
            list.add(n);
            families.put(prefix, list);
        }

        // If not enough items in the list merge back with the most similar one
        List<String> toRemove = new ArrayList<>();
        Iterator<Map.Entry<String, List<Node>>> itMap = families.entrySet().iterator();

        while (itMap.hasNext()) {
            Map.Entry<String, List<Node>>  en = itMap.next();

            String actualKey = en.getKey();

            // If community to small, merge back
            if(en.getValue().size() < MIN_NODE) {
                String bestCandidate  = null;

                int score = Integer.MAX_VALUE;
                for(String name : families.keySet()) { // Find the most similar family
                    if(name.equals(actualKey)) continue;
                    if(toRemove.contains(name)) continue;
                    // get score
                    int lScore = this.computeLevenshtein(name, actualKey);
                    if(score > lScore) {
                        bestCandidate = name;
                        score = lScore;
                    }
                }

                if(bestCandidate != null) {
                    // Merge the community back in the best candidate
                    families.get(bestCandidate).addAll(en.getValue());
                }

                // remove old record ( avoid concurrent access)
                itMap.remove();

            }
        }

        return families;
    }

    /**
     * Increase by 1 the depth of the tree
     * @return the number of created nodes
     */
    public int increaseDepth() {
        int createdLeaves = 0;
        FamilyLeaf start = this.root;
        Stack<FamilyLeaf> toVisit = new Stack<>();
        toVisit.push(start);

        while(!toVisit.isEmpty()) {
            FamilyLeaf actualFl = toVisit.pop();
            int depth = actualFl.depth + 1;

            // Check if node have children
            if(!actualFl.children.isEmpty()) {
                for(FamilyLeaf fl : actualFl.children) {
                    toVisit.push(fl);
                }
            } else {
                // Ignore empty leaf
                if(actualFl.items.isEmpty()) continue;
                log.info(String.format("KeySet for level for level %d : %s", actualFl.depth, actualFl.items.stream().map(x ->  (String) x.getProperty("Name")).collect(Collectors.joining(" ,"))));
                Map<String, List<Node>> newFamilies = explodeName(depth, actualFl.items); // Explode the names

                // Create new leaves
                for(Map.Entry<String, List<Node>> en : newFamilies.entrySet()) {
                    FamilyLeaf familyLeaf = new FamilyLeaf(en.getKey(), depth, new ArrayList<>(), en.getValue());
                    actualFl.children.add(familyLeaf);
                    createdLeaves++;
                }
            }
        }

        return createdLeaves;
    }

    /**
     * Print the tree built, in the logs
     * @param fl The leaf used as a starting point
     */
    public void displayFinalTree(FamilyLeaf fl) {
        String mes = String.format("%s : Prefix %s : Size : %s", "-".repeat(fl.depth*2), fl.name, fl.children.size());
        if (fl.breakpoint) {
            mes += " -- IS BREAKPOINT --";
        }
        log.info(mes);

        for(FamilyLeaf cl : fl.children) {
            displayFinalTree(cl);
        }
    }

    /**
     * Check if leaf is a breakPoint. A break point is a leaf having a better distribution than its children
     * @param fl Leaf to inspect
     */
    public void applyBreakpoints(FamilyLeaf fl) {
        int numChildren = fl.children.size();

        double sumItemChildren = .0;
        for (FamilyLeaf cfl : fl.children) {
            sumItemChildren += cfl.children.size();
            applyBreakpoints(cfl);
        }
        double meanItems = sumItemChildren / numChildren;
        fl.breakpoint =  numChildren > meanItems && sumItemChildren != 0;
    }

    /**
     * Check if a breakpoint is present under a branch
     * @param fl Id of the leaf to start the investigation
     * @return
     */
    public boolean breakpointUnderBranch(FamilyLeaf fl) {

        for(FamilyLeaf cfl : fl.children) {
            if(cfl.breakpoint) return true;
            breakpointUnderBranch(cfl);
        }

        return false;
    }

    /**
     * Get end leaves representing the Families founds during the process
     * @return List of Families
     */
    public List<FamilyLeaf> getEndLeaves() {

        List<FamilyLeaf> returnList = new ArrayList<>();
        Stack<FamilyLeaf> toVisit = new Stack<>();

        toVisit.push(this.root);

        FamilyLeaf fl;

        while(!toVisit.isEmpty()) {
            fl = toVisit.pop();
            if(fl.children.isEmpty() || ( !breakpointUnderBranch(fl) && fl.depth > MIN_PREFIX) ) {
                returnList.add(fl);
            } else {
                for(FamilyLeaf child : fl.children)
                toVisit.push(child);
            }
        }

        displayFinalTree(this.root);

        return returnList;
    }

    /**
     * Build a tree and increase the depth  N times
     * @param maxDepth Maximum depth
     * @return
     */
    public FamilyLeaf buildTree(int maxDepth) {
        // Build the tree
        for(int i = 0; i < maxDepth -1; i++) increaseDepth();

        // Apply breakpoints in the tree
        applyBreakpoints(this.root);
        return root;
    }

    public FamilyTree(Log log, FamilyLeaf root) {
        this.log = log;
        this.root = root;
    }

    public FamilyTree(Log log, List<Node> nodes) {
        this.log  = log;
        this.root = new FamilyLeaf("", 0, new ArrayList<>(), nodes);
    }
}
