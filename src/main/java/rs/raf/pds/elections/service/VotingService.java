package rs.raf.pds.elections.service;


import rs.raf.pds.elections.domain.Election;
import rs.raf.pds.elections.domain.VoteResult;
import rs.raf.pds.elections.domain.VotingUnit;

import java.util.*;
import java.util.stream.Collectors;

public class VotingService {

    public void setElections(Map<Integer, Election> elections) {
        this.elections = elections;
    }

    private Map<Integer, Election> elections;

    public VotingService(Map<Integer, Election> elections) {
        this.elections = elections;
    }
    public List<String> getUnitsToRetry() {
        List<String> unitsToRetry = new ArrayList<>();
        for (Election election : elections.values()) {
            for (VotingUnit vu : election.getVotingUnits()) {
                if (vu.isNeedsReentry()) {
                    unitsToRetry.add(vu.getName());
                }
            }
        }
        return unitsToRetry;
    }

    public Map<String, Map<String, List<VoteResult>>> getAllResults() {
        Map<String, Map<String, List<VoteResult>>> allResults = new HashMap<>();
        for (Election e : elections.values()) {
            allResults.put(e.getId() + "", e.getResults());
        }
        return allResults;
    }

    public void addVoteResult(Integer electionId, String votingStationId, VoteResult result) {
        Election election = elections.get(electionId);
        if (election == null) return;

        election.getResults().computeIfAbsent(votingStationId, k -> new ArrayList<>()).add(result);

        List<VoteResult> allResults = election.getResults().get(votingStationId).stream()
                .filter(VoteResult::isValid)
                .collect(Collectors.toList());

        if (allResults.size() >= 2) {
            Optional<VoteResult> majority = findMajority(allResults);
            if (majority.isPresent()) {
                election.getVotingUnits().stream()
                        .filter(vu -> vu.getName().equals(votingStationId))
                        .forEach(vu -> vu.setNeedsReentry(false));
            } else {
                election.getVotingUnits().stream()
                        .filter(vu -> vu.getName().equals(votingStationId))
                        .forEach(vu -> vu.setNeedsReentry(true));
            }
        }
    }

    private Optional<VoteResult> findMajority(List<VoteResult> results) {
        Map<VoteResult, Integer> countMap = new HashMap<>();
        for (VoteResult vr : results) {
            countMap.put(vr, countMap.getOrDefault(vr, 0) + 1);
        }
        for (Map.Entry<VoteResult, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > results.size() / 2) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public int getFilledVotingUnits() {
        int filled = 0;
        for (Election e : elections.values()) {
            for (List<VoteResult> vrList : e.getResults().values()) {
                if (!vrList.isEmpty()) filled++;
            }
        }
        return filled;
    }

    public void setAllResults(Map<String, Map<String, List<VoteResult>>> allResults) {
        for (Map.Entry<String, Map<String, List<VoteResult>>> entry : allResults.entrySet()) {
            int electionId = Integer.parseInt(entry.getKey());
            Election e = elections.get(electionId);
            if (e != null) {
                e.setResults(entry.getValue());
            }
        }
    }

    public int getTotalVotingUnits() {
        int total = 0;
        for (Election e : elections.values()) {
            total += e.getResults().size();
        }
        return total;
    }

    public int getVotingUnitsToRetry() {
        int retry = 0;
        for (Election e : elections.values()) {
            for (VotingUnit vu : e.getVotingUnits()) {
                if (vu.isNeedsReentry()) retry++;
            }
        }
        return retry;
    }

    public List<VoteResult> getResults(String electionId, String pollingStationId) {
        Election e = elections.get(electionId);
        if (e == null) return Collections.emptyList();
        return e.getResults().getOrDefault(pollingStationId, Collections.emptyList());
    }

    public void applyVoteResult(int electionId, String votingUnitName, VoteResult vr) {
        Election election = elections.get(electionId);
        if (election == null) return;

        List<VoteResult> resultsForUnit = election.getResults().computeIfAbsent(votingUnitName, k -> new ArrayList<>());
        resultsForUnit.add(vr);

        if (resultsForUnit.size() >= 2) {
            Map<VoteResult, Integer> countMap = new HashMap<>();
            for (VoteResult r : resultsForUnit) countMap.put(r, countMap.getOrDefault(r, 0) + 1);
            Optional<VoteResult> majority = countMap.entrySet().stream()
                    .filter(e -> e.getValue() > resultsForUnit.size() / 2)
                    .map(Map.Entry::getKey).findFirst();
            election.getVotingUnits().stream()
                    .filter(vu -> vu.getName().equals(votingUnitName))
                    .forEach(vu -> vu.setNeedsReentry(majority.isEmpty()));
        }
    }


}