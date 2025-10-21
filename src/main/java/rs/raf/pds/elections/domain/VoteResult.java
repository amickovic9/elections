package rs.raf.pds.elections.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class VoteResult implements Serializable {
    private int controllorId;
    private int totalVoters;
    private int invalidVotes;
    private Map<String, Integer> candidateVotes;

    public VoteResult(int controllorId, int totalVoters, int invalidVotes, Map<String, Integer> candidateVotes) {
        this.controllorId = controllorId;
        this.totalVoters = totalVoters;
        this.invalidVotes = invalidVotes;
        this.candidateVotes = candidateVotes;
    }

    public int getControllorId() { return controllorId; }
    public int getTotalVoters() { return totalVoters; }
    public int getInvalidVotes() { return invalidVotes; }
    public Map<String, Integer> getCandidateVotes() { return candidateVotes; }

    public int getValidVotes() {
        return totalVoters - invalidVotes;
    }

    public boolean isValid() {
        int sum = candidateVotes.values().stream().mapToInt(Integer::intValue).sum();
        return sum == getValidVotes();
    }

    @Override
    public String toString() {
        return "VoteResult{" + "controllorId='" + controllorId + '\'' +
                ", totalVoters=" + totalVoters +
                ", invalidVotes=" + invalidVotes +
                ", candidateVotes=" + candidateVotes + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoteResult)) return false;
        VoteResult other = (VoteResult) o;
        return totalVoters == other.totalVoters &&
                invalidVotes == other.invalidVotes &&
                Objects.equals(candidateVotes, other.candidateVotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalVoters, invalidVotes, candidateVotes);
    }

}
