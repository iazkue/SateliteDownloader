package tfg.satelitedownloader.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to convert GeoJSON to different area formats
 */
public class GeoJsonConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts GeoJSON to WKT (Well-Known Text) format for Copernicus
     * 
     * @param geoJson The GeoJSON string
     * @return WKT string in SRID=4326;POLYGON format
     */
    public static String convertToWKT(String geoJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(geoJson);

            if (jsonNode.has("geometry")) {
                jsonNode = jsonNode.get("geometry");
            }

            String geometryType = jsonNode.get("type").asText();
            JsonNode coordinates = jsonNode.get("coordinates");

            if ("Polygon".equals(geometryType)) {
                return convertPolygonToWKT(coordinates);
            } else if ("Point".equals(geometryType)) {
                return convertPointToWKT(coordinates);
            } else {
                throw new IllegalArgumentException("Unsupported geometry type: " + geometryType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error converting GeoJSON to WKT: " + e.getMessage(), e);
        }
    }

    /**
     * Converts GeoJSON to bounding box format for Landsat
     * 
     * @param geoJson The GeoJSON string
     * @return Comma-separated coordinates as string
     */
    public static String convertToBoundingBox(String geoJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(geoJson);

            if (jsonNode.has("geometry")) {
                jsonNode = jsonNode.get("geometry");
            }

            String geometryType = jsonNode.get("type").asText();
            JsonNode coordinates = jsonNode.get("coordinates");

            if ("Polygon".equals(geometryType)) {
                return extractBoundingBox(coordinates);
            } else if ("Point".equals(geometryType)) {
                double lon = coordinates.get(0).asDouble();
                double lat = coordinates.get(1).asDouble();
                return lat + "," + lon + "," + lat + "," + lon;
            } else {
                throw new IllegalArgumentException("Unsupported geometry type: " + geometryType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error converting GeoJSON to bounding box: " + e.getMessage(), e);
        }
    }

    private static String convertPolygonToWKT(JsonNode coordinates) {
        StringBuilder wkt = new StringBuilder("SRID=4326;POLYGON((");
        JsonNode outerRing = coordinates.get(0);

        for (int i = 0; i < outerRing.size(); i++) {
            JsonNode point = outerRing.get(i);
            double lon = point.get(0).asDouble();
            double lat = point.get(1).asDouble();

            if (i > 0) {
                wkt.append(",");
            }
            wkt.append(lon).append(" ").append(lat);
        }

        wkt.append("))");
        return wkt.toString();
    }

    private static String convertPointToWKT(JsonNode coordinates) {
        double lon = coordinates.get(0).asDouble();
        double lat = coordinates.get(1).asDouble();
        // Create a small polygon around the point
        double offset = 0.001; // Small offset for creating a polygon around the point
        return String.format("SRID=4326;POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
                lon - offset, lat - offset,
                lon + offset, lat - offset,
                lon + offset, lat + offset,
                lon - offset, lat + offset,
                lon - offset, lat - offset);
    }

    private static String extractBoundingBox(JsonNode coordinates) {
        JsonNode outerRing = coordinates.get(0);

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (int i = 0; i < outerRing.size(); i++) {
            JsonNode point = outerRing.get(i);
            double lon = point.get(0).asDouble();
            double lat = point.get(1).asDouble();

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
        }

        return minLat + "," + minLon + "," + maxLat + "," + maxLon;
    }
}
