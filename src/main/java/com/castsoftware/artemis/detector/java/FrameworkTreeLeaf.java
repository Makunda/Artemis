package com.castsoftware.artemis.detector.java;

import java.util.ArrayList;
import java.util.List;

public class FrameworkTreeLeaf {

    private String name;
    private List<FrameworkTreeLeaf> children;
    private boolean framework;

    public boolean isFramework() {
        return framework;
    }

    public void setFramework(boolean framework) {
        this.framework = framework;
    }

    public String getName() {
        return name;
    }

    public List<FrameworkTreeLeaf> getChildren() {
        return children;
    }

    public void addLeaf(FrameworkTreeLeaf leaf) {
        this.children.add(leaf);
    }

    public FrameworkTreeLeaf(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.framework = false;
    }
}
