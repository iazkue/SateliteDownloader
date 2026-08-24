package tfg.satelitedownloader.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class propsReader {
    private static final Properties props = new Properties();

    static {
        // 1. Load config.properties from classpath
        try (InputStream input = propsReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // 2. Load .env file if available (checking current dir and parent dirs)
        Path[] possibleEnvPaths = new Path[] {
            Paths.get(".env"),
            Paths.get("../.env"),
            Paths.get("../../.env")
        };

        for (Path envPath : possibleEnvPaths) {
            if (Files.exists(envPath)) {
                try {
                    List<String> lines = Files.readAllLines(envPath);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String val = line.substring(eqIdx + 1).trim();
                            // Strip quotes if present
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if ("USERNAME_COPERNICUS_S".equalsIgnoreCase(key)) {
                                props.setProperty("COPERNICUS_USERNAME", val);
                            } else if ("PASSWORD_COPERNICUS_S".equalsIgnoreCase(key)) {
                                props.setProperty("COPERNICUS_PASSWORD", val);
                            } else {
                                props.setProperty(key, val);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Could not load .env file from " + envPath + ": " + e.getMessage());
                }
                break; // Found and loaded
            }
        }
    }

    public static String get(String key) {
        String val = props.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        // Fallback to System.getenv
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        if ("COPERNICUS_USERNAME".equalsIgnoreCase(key)) {
            envVal = System.getenv("USERNAME_COPERNICUS_S");
        } else if ("COPERNICUS_PASSWORD".equalsIgnoreCase(key)) {
            envVal = System.getenv("PASSWORD_COPERNICUS_S");
        }
        return envVal != null ? envVal.trim() : "";
    }
}
