package tfg.satelitedownloader.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeoJsonConverterTest {

    @Test
    public void testConvertToWKTPolygon() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-4.0, 43.0], [-3.0, 43.0], [-3.0, 44.0], [-4.0, 44.0], [-4.0, 43.0]]]}";

        String result = GeoJsonConverter.convertToWKT(geoJson);
        String expected = "SRID=4326;POLYGON((-4.0 43.0,-3.0 43.0,-3.0 44.0,-4.0 44.0,-4.0 43.0))";

        assertEquals(expected, result, "WKT Polygon parsing failed");
    }

    @Test
    public void testConvertToWKTPoint() {
        String geoJson = "{\"geometry\": {\"type\":\"Point\",\"coordinates\":[-4.0, 43.0]}}";

        String result = GeoJsonConverter.convertToWKT(geoJson);

        // Point is transformed into a small bounding box offset polygon (+-0.001)
        assertTrue(result.startsWith("SRID=4326;POLYGON(("));
        assertTrue(result.contains("-4.001000 42.999000")); // lon-offset lat-offset
        assertTrue(result.contains("-3.999000 43.001000")); // lon+offset lat+offset
    }

    @Test
    public void testConvertToBoundingBoxPolygon() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-4.0, 43.0], [-3.0, 43.0], [-3.0, 44.0], [-4.0, 44.0], [-4.0, 43.0]]]}";

        String result = GeoJsonConverter.convertToBoundingBox(geoJson);
        // Bounding box logic returns: minLat, minLon, maxLat, maxLon
        String expected = "43.0,-4.0,44.0,-3.0";

        assertEquals(expected, result, "BoundingBox Polygon parsing failed");
    }

    @Test
    public void testConvertToBoundingBoxPoint() {
        String geoJson = "{\"type\":\"Point\",\"coordinates\":[-4.0, 43.0]}";

        String result = GeoJsonConverter.convertToBoundingBox(geoJson);
        String expected = "43.0,-4.0,43.0,-4.0"; // lat,lon,lat,lon

        assertEquals(expected, result, "BoundingBox Point parsing failed");
    }

    @Test
    public void testUnsupportedGeometry() {
        String geoJson = "{\"type\":\"LineString\",\"coordinates\":[[-4.0, 43.0], [-3.0, 44.0]]}";

        Exception ex = assertThrows(RuntimeException.class, () -> {
            GeoJsonConverter.convertToWKT(geoJson);
        });

        assertTrue(ex.getMessage().contains("Unsupported geometry type"));
    }

    @Test
    public void testInvalidJson() {
        String badJson = "{type:Polygon,-]";

        assertThrows(RuntimeException.class, () -> {
            GeoJsonConverter.convertToBoundingBox(badJson);
        });
    }
}
