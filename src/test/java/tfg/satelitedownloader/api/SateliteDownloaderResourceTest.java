package tfg.satelitedownloader.api;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.db.CopernicusTileDAO;
import tfg.satelitedownloader.model.CopernicusTile;
import tfg.satelitedownloader.service.CopernicusProvider;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class SateliteDownloaderResourceTest {

    private final CopernicusProvider provider = mock(CopernicusProvider.class);
    private final CopernicusTileDAO dao = mock(CopernicusTileDAO.class);
    private final LinkedBlockingQueue<SatelliteDownloadRequest> queue = new LinkedBlockingQueue<>();

    public final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new SateliteDownloaderResource(provider, dao, queue))
            .build();

    @BeforeEach
    public void setup() {
        queue.clear();
        when(dao.findAll()).thenReturn(Collections.emptyList());
    }

    @AfterEach
    public void tearDown() {
        reset(provider, dao);
    }

    @Test
    public void testDownloadImages_SuccessfulQueue() {
        SatelliteDownloadRequest req = new SatelliteDownloadRequest();
        req.setInitialDay("2023-01-01T00:00:00.000Z");
        req.setFinalDay("2023-01-10T00:00:00.000Z");
        req.setGeoJson("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]}");

        Response response = resources.target("/downloadImages")
                .request()
                .post(Entity.entity(req, MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        assertEquals(1, queue.size()); // Item should be in the queue
        tfg.satelitedownloader.model.SatelliteDownloadTask task = response.readEntity(tfg.satelitedownloader.model.SatelliteDownloadTask.class);
        assertEquals("QUEUED", task.getStatus());
    }

    @Test
    public void testDownloadImages_BadRequest() {
        SatelliteDownloadRequest req = new SatelliteDownloadRequest();
        // Missing fields

        Response response = resources.target("/downloadImages")
                .request()
                .post(Entity.entity(req, MediaType.APPLICATION_JSON));

        assertEquals(400, response.getStatus());
        assertEquals(0, queue.size()); // Nothing added
    }

    @Test
    public void testDownloadPreviews() throws Exception {
        SatelliteDownloadRequest req = new SatelliteDownloadRequest();
        req.setInitialDay("2023-01-01T00:00:00.000Z");
        req.setFinalDay("2023-01-10T00:00:00.000Z");
        req.setGeoJson("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]}");

        // Mock providers behaviour
        Tile mockedTile = new CopernicusTile("123", "TILE_1", "md5", "2023-01-01", "2023-01-10", "POLYGON",
                "http://preview.link");
        when(provider.getTile(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(mockedTile));
        when(provider.getAccessToken()).thenReturn("fake-access-token");
        doNothing().when(provider).downloadPreviewImage(anyString(), anyString(), anyString());

        Response response = resources.target("/downloadPreviews")
                .request()
                .post(Entity.entity(req, MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        String streamOutput = response.readEntity(String.class);
        org.junit.jupiter.api.Assertions.assertTrue(streamOutput.contains("\"status\":\"completed\""));
        org.junit.jupiter.api.Assertions.assertTrue(streamOutput.contains("TILE_1_preview.png"));
        verify(provider, times(1)).downloadPreviewImage(eq("http://preview.link"), eq("fake-access-token"),
                anyString());
    }
}
