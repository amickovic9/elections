package rs.raf.pds.elections.domain;

import java.io.Serializable;

public class Candidate implements Serializable {
    private int id;
    private String name;

    public Candidate() { }

    public Candidate(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Candidate(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
