package tfg.satelitedownloader.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class propsReader {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = propsReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                System.out.println("Sorry, unable to find config.properties");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
