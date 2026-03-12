package org.roda.core.data.v2.ip;

import java.io.Serializable;

public class ParentAncestorMap implements Serializable {
    private String ancestorId;
    private String title;
    public ParentAncestorMap() {}

    public ParentAncestorMap(String ancestorId, String title) {
        this.ancestorId = ancestorId;
        this.title = title;
    }

    public String getAncestorId() { return ancestorId; }
    public void setAncestorId(String id) { this.ancestorId = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
