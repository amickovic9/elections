package rs.raf.pds.elections.domain;

import java.util.Map;

public class VoteResult {
    private String controllerId;
    private int totalVotes;
    private int invalidVotes;
    private Map<String, Integer> candidateVotes;

    public VoteResult(String controllerId, Map<String, Integer> candidateVotes, int invalidVotes, int totalVotes) {
        this.controllerId = controllerId;
        this.candidateVotes = candidateVotes;
        this.invalidVotes = invalidVotes;
        this.totalVotes = totalVotes;
    }

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public int getInvalidVotes() {
        return invalidVotes;
    }

    public void setInvalidVotes(int invalidVotes) {
        this.invalidVotes = invalidVotes;
    }

    public Map<String, Integer> getCandidateVotes() {
        return candidateVotes;
    }

    public void setCandidateVotes(Map<String, Integer> candidateVotes) {
        this.candidateVotes = candidateVotes;
    }
}
