package rs.raf.pds.elections.domain;

import rs.raf.pds.elections.enums.ElectionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Election implements Serializable {
    private int id;
    private String name;
    private ElectionType type;
    private List<Candidate> candidates = new ArrayList<>();
    private List<VotingUnit> votingUnits = new ArrayList<>();
    private Map<String, List<VoteResult>> results = new HashMap<>();

    public Election() { }

    public Election(String name, int id, List<Candidate> candidates, ElectionType type, List<VotingUnit> votingUnits) {
        this.name = name;
        this.id = id;
        this.candidates = candidates;
        this.type = type;
        this.votingUnits = votingUnits;
        this.results = new HashMap<>();
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

    public ElectionType getType() {
        return type;
    }

    public void setType(ElectionType type) {
        this.type = type;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public List<VotingUnit> getVotingUnits() {
        return votingUnits;
    }

    public void setVotingUnits(List<VotingUnit> votingUnits) {
        this.votingUnits = votingUnits;
    }

    public Map<String, List<VoteResult>> getResults() {
        return results;
    }

    public void setResults(Map<String, List<VoteResult>> results) {
        this.results = results;
    }
}
