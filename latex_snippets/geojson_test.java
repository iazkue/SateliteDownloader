@Test
public void testConvertToWKTPolygon() {
    String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-4.0, 43.0], [-3.0, 43.0], [-3.0, 44.0], [-4.0, 44.0], [-4.0, 43.0]]]}";

    String result = GeoJsonConverter.convertToWKT(geoJson);
    String expected = "SRID=4326;POLYGON((-4.0 43.0,-3.0 43.0,-3.0 44.0,-4.0 44.0,-4.0 43.0))";

    assertEquals(expected, result, "WKT Polygon parsing failed");
}
