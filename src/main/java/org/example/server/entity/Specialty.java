package org.example.server.entity;

/**
 * Représente une spécialité médicale (ex: Cardiologie, Neurologie).
 * Correspond à la table 'speciality' en base de données.
 */
public class Specialty implements Entity {

    private Integer id;
    private String name;

    public Specialty() {
    }

    public Specialty(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Specialty(String name) {
        this.name = name;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Specialty{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
