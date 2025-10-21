package rs.raf.pds.elections.service;

import rs.raf.pds.elections.domain.*;
import rs.raf.pds.elections.enums.ElectionType;

import java.util.*;

public class SimulateVoting {

    private Map<Integer, Election> elections;
    private boolean isLeader;
    private List<SimulateVoting> followers;

    public SimulateVoting(Map<Integer, Election> elections, boolean isLeader) {
        this.elections = elections;
        this.isLeader = isLeader;
        this.followers = new ArrayList<>();
    }

    public void addFollower(SimulateVoting follower) {
        followers.add(follower);
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        this.isLeader = leader;
    }

    public synchronized void addVoteResult(int electionId, VotingUnit unit, VoteResult result) {
        Election election = elections.get(electionId);
        if (election == null || !result.isValid()) return;

        List<VoteResult> resultsForUnit = election.getResults().get(unit.getName());
        if (resultsForUnit == null) {
            resultsForUnit = new ArrayList<>();
            election.getResults().put(unit.getName(), resultsForUnit);
        }
        resultsForUnit.add(result);

        Map<String, Integer> totalVotes = new HashMap<>();
        int totalValid = 0;
        for (VoteResult vr : resultsForUnit) {
            for (Map.Entry<String, Integer> e : vr.getCandidateVotes().entrySet()) {
                totalVotes.put(e.getKey(), totalVotes.getOrDefault(e.getKey(), 0) + e.getValue());
                totalValid += e.getValue();
            }
        }

        int finalTotalValid = totalValid;
        boolean clearWinner = totalVotes.values().stream().anyMatch(v -> v > finalTotalValid / 2);
        unit.setNeedsReentry(!clearWinner);

        if (isLeader) {
            for (SimulateVoting follower : followers) {
                follower.receiveReplication(electionId, unit, result);
            }
            createSnapshot(election);
        }
    }

    public synchronized void receiveReplication(int electionId, VotingUnit unit, VoteResult result) {
        Election election = elections.get(electionId);
        if (election == null) return;
        List<VoteResult> resultsForUnit = election.getResults().get(unit.getName());
        if (resultsForUnit == null) {
            resultsForUnit = new ArrayList<>();
            election.getResults().put(unit.getName(), resultsForUnit);
        }
        resultsForUnit.add(result);
    }

    public Election getElection(int id) {
        return elections.get(id);
    }

    public void createSnapshot(Election election) {
        System.out.println("[SNAPSHOT] Election '" + election.getName() + "' saved at  " + (isLeader ? "lider" : "replica"));
    }

    public static class VotingServer {
        private SimulateVoting leader;
        private SimulateVoting follower1;
        private SimulateVoting follower2;

        public VotingServer(SimulateVoting leader, SimulateVoting follower1, SimulateVoting follower2) {
            this.leader = leader;
            this.follower1 = follower1;
            this.follower2 = follower2;
        }

        public void simulateFailover() {
            System.out.println("\n*** Lider is down, failover started***");
            leader.setLeader(false);
            follower1.setLeader(true);
            leader = follower1;
            System.out.println("*** new leader ***" + leader);
        }

        public SimulateVoting getLeader() {
            return leader;
        }
    }

    public static void main(String[] args) {
        Random rnd = new Random();
        int numPollingStations = 3;
        int numCandidates = 3;

        Map<Integer, Election> electionsMap = new HashMap<>();
        int electionIdCounter = 1;

        for (ElectionType type : ElectionType.values()) {
            List<Candidate> candidates = new ArrayList<>();
            for (int i = 1; i <= numCandidates; i++) {
                candidates.add(new Candidate(i, type.name() + "_Candidate" + i));
            }

            List<VotingUnit> units = new ArrayList<>();
            for (int ps = 1; ps <= numPollingStations; ps++) {
                List<Controllor> controllors = Arrays.asList(
                        new Controllor(1, "Ctrl1"),
                        new Controllor(2, "Ctrl2"),
                        new Controllor(3, "Ctrl3")
                );
                units.add(new VotingUnit(ps, "PS" + ps, controllors));
            }

            Election election = new Election(type.name(), electionIdCounter++, candidates, type, units);
            electionsMap.put(election.getId(), election);
        }

        SimulateVoting leader = new SimulateVoting(electionsMap, true);
        SimulateVoting follower1 = new SimulateVoting(new HashMap<>(electionsMap), false);
        SimulateVoting follower2 = new SimulateVoting(new HashMap<>(electionsMap), false);

        leader.addFollower(follower1);
        leader.addFollower(follower2);

        VotingServer server = new VotingServer(leader, follower1, follower2);

        for (Election election : electionsMap.values()) {
            for (VotingUnit vu : election.getVotingUnits()) {
                for (Controllor ctrl : vu.getControllors()) {
                    Map<String, Integer> votes = new HashMap<>();
                    for (Candidate c : election.getCandidates()) {
                        votes.put(c.getName(), 30 + rnd.nextInt(20));
                    }
                    int invalid = rnd.nextInt(5);
                    int total = votes.values().stream().mapToInt(Integer::intValue).sum() + invalid;
                    VoteResult vr = new VoteResult(ctrl.getId(), total, invalid, votes);
                    server.getLeader().addVoteResult(election.getId(), vu, vr);
                }
            }
        }

        server.simulateFailover();

        for (Election election : electionsMap.values()) {
            System.out.println("\n===== Election: " + election.getName() + " =====");
            for (VotingUnit vu : election.getVotingUnits()) {
                List<VoteResult> results = election.getResults().get(vu.getName());
                if (results != null) {
                    for (VoteResult vr : results) System.out.println(vr);
                }
            }
        }
    }
}
