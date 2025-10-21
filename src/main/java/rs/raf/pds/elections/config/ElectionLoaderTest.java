package rs.raf.pds.elections.config;

import rs.raf.pds.elections.domain.Candidate;
import rs.raf.pds.elections.domain.Election;
import rs.raf.pds.elections.domain.VotingUnit;

import java.util.List;


public class ElectionLoaderTest {

    public static void main(String[] args) {
        try {
            String path = "/home/aleksandar/Aleksandar/faks/distribuirane/elections/elections/src/main/java/rs/raf/pds/elections/config/config.json";

            List<Election> elections = ElectionLoader.loadElections(path);

            elections.forEach(
                    election -> {
                                System.out.println("Election ID: " + election.getId());
                                System.out.println("Election Name: " + election.getName());
                                System.out.println("Election Type: " + election.getType());

                                System.out.println("Candidates:");
                                for (Candidate c : election.getCandidates()) {
                                    System.out.println(" - " + c.getName());
                                }

                                System.out.println("Voting Units:");
                                for (VotingUnit vu : election.getVotingUnits()) {
                                    System.out.println(" - " + vu.getId() + ": " + vu.getName());
                                    System.out.println("Controllors: ");
                                    vu.getControllors().forEach(c-> {System.out.println(c.getName());});
                                }
                    }
                    );
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}