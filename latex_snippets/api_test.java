@Test
public void testDownloadPreviews() throws Exception {
    SatelliteDownloadRequest req = new SatelliteDownloadRequest();
    // [...] Eskaeraren datuak bete (GeoJson, datak)

    // Hornitzailearen portaera mock bidez simulatu
    Tile mockedTile = new CopernicusTile("123", "TILE_1", "md5", "2023-01-01", "2023-01-10", "POLYGON",
            "http://preview.link");
    when(provider.getTile(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Collections.singletonList(mockedTile));
    when(provider.getAccessToken()).thenReturn("fake-access-token");
    doNothing().when(provider).downloadPreviewImage(anyString(), anyString(), anyString());

    // API birtualerantz HTTP POST eskaera bidali bermatutako parametroekin
    Response response = resources.target("/downloadPreviews")
            .request()
            .post(Entity.entity(req, MediaType.APPLICATION_JSON));

    assertEquals(200, response.getStatus()); // Erantzun egokia dela ziurtatu
}
