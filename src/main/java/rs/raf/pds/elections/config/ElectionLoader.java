package rs.raf.pds.elections.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import rs.raf.pds.elections.domain.Election;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ElectionLoader {

    public static List<Election> loadElections(String path) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(path), new TypeReference<List<Election>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error reading election JSON: " + e.getMessage(), e);
        }
    }
}
