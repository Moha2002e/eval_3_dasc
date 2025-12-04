package org.example.server.searchvm;

import java.io.Serializable;

public class SpecialtySearchVM implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public SpecialtySearchVM() {
    }

    public SpecialtySearchVM(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
