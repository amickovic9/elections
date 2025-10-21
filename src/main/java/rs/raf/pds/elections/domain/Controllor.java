package rs.raf.pds.elections.domain;

import java.io.Serializable;

public class Controllor implements Serializable {
    private int id;
    private String name;

    public Controllor() { }

    public Controllor(int id, String name) {
        this.id = id;
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


