package rs.raf.pds.elections.domain;

public class Controllor {private int id;
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


