package com.castsoftware.artemis.interactions.famililes;

import com.castsoftware.artemis.config.Configuration;
import org.apache.shiro.crypto.hash.Hash;
import org.neo4j.graphdb.Node;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class FamilyTree {

    // Imaging
    private static final String IMAGING_OBJECT_NAME = Configuration.get("imaging.node.object.name");
    private static final Double MIN_PERCENTAGE = 0.10;

    public class FamilyLeaf {
        public String name;
        public Integer depth;
        public List<FamilyLeaf> children;
        public List<Node> items;

        public FamilyLeaf(String name, Integer depth, List<FamilyLeaf> children, List<Node> items) {
            this.name = name;
            this.depth = depth;
            this.children = children;
            this.items = items;
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
        int numNodes = toTreatNodes.size();

        List<String> toRemove = new ArrayList<>();
        Iterator<Map.Entry<String, List<Node>>> itMap = families.entrySet().iterator();
        while (itMap.hasNext()) {
            Map.Entry<String, List<Node>>  en = itMap.next();

            String actualKey = en.getKey();
            double ratio = (double) en.getValue().size() / (double) numNodes;

            // If community to small, merge back
            if(ratio < MIN_PERCENTAGE) {
                String bestCandidate  = null;
                Integer score = Integer.MAX_VALUE;
                for(String name : families.keySet()) {
                    if(name.equals(actualKey)) continue;
                    if(toRemove.contains(name)) continue;
                    // get score
                    int lScore = this.computeLevenshtein(name, actualKey);
                    if(score > lScore) {
                        bestCandidate = name;
                    }
                }

                // Merge the community back in the best candidate
                families.get(bestCandidate).addAll(en.getValue());

                toRemove.add(bestCandidate);
            }
        }

        // remove old record ( avoid concurrent access)
        toRemove.stream().map((Function<String, Object>) families::remove);

        return families;
    }

    /**
     * Increase by 1 the depth of the tree
     * @return the number of created nodes
     */
    public int increaseDepth() {
        int createdLeaves = 0;
        FamilyLeaf start = this.root;
        List<FamilyLeaf> toVisit = new CopyOnWriteArrayList();
        toVisit.add(start);

        for(Iterator<FamilyLeaf> itTree = toVisit.listIterator(); itTree.hasNext();) {
            FamilyLeaf actualFl = itTree.next();
            int depth = actualFl.depth + 1;

            // Check if node have children
            if(!actualFl.children.isEmpty()) {
                toVisit.addAll(actualFl.children);
            } else {
                // Ignore empty leaf
                if(actualFl.items.isEmpty()) continue;
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
     * Get end leaves representing the Families founds during the process
     * @return List of Families
     */
    public List<FamilyLeaf> getEndLeaves() {
        List<FamilyLeaf> returnList = new ArrayList<>();
        List<FamilyLeaf> toVisit = Arrays.asList(root);

        FamilyLeaf fl;
        Iterator<FamilyLeaf> itTree = toVisit.listIterator();
        while(itTree.hasNext()) {
            fl = itTree.next();
            if(fl.children.isEmpty()) {
                returnList.add(fl);
            } else {
                toVisit.addAll(fl.children);
            }
        }

        return returnList;
    }

    /**
     * Build a tree and increase the depth  N times
     * @param maxDepth Maximum depth
     * @return
     */
    public FamilyLeaf buildTree(int maxDepth) {
        for(int i = 0; i < maxDepth -1; i++) increaseDepth();
        return root;
    }

    public FamilyTree(FamilyLeaf root) {
        this.root = root;
    }

    public FamilyTree(List<Node> nodes) {
        this.root = new FamilyLeaf("", 0, new ArrayList<>(), nodes);
    }
}
