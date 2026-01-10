package org.example.server.entity;

import java.io.Serializable;


public interface Entity extends Serializable {


    Integer getId();


    void setId(Integer id);


    default boolean isPersisted() {
        return getId() != null && getId() > 0;
    }


    default boolean isNew() {
        return !isPersisted();
    }
}
