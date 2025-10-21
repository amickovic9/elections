package rs.raf.pds.elections.domain;

import java.io.Serializable;
import java.util.List;

public class VotingUnit implements Serializable {
    private int id;
    private String name;
    private List<Controllor> controllors;
    private boolean needsReentry;

    public VotingUnit() { }

    public VotingUnit(int id, String name, List<Controllor> controllors) {
        this.id = id;
        this.name = name;
        this.controllors = controllors;
        this.needsReentry = false;
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

    public List<Controllor> getControllors() {
        return controllors;
    }

    public void setControllors(List<Controllor> controllors) {
        this.controllors = controllors;
    }

    public boolean isNeedsReentry() {
        return needsReentry;
    }

    public void setNeedsReentry(boolean needsReentry) {
        this.needsReentry = needsReentry;
    }
}

