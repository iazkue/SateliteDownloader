package tfg.satelitedownloader.worker;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tfg.satelitedownloader.api.SatelliteDownloadRequest;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.model.CopernicusTile;
import tfg.satelitedownloader.service.CopernicusProvider;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DownloadWorkerTest {

    private final LinkedBlockingQueue<SatelliteDownloadRequest> queue = new LinkedBlockingQueue<>();

    @Mock
    private CopernicusProvider provider;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    private DownloadWorker worker;
    private AutoCloseable mocks;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        queue.clear();

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        worker = new DownloadWorker(queue, provider, sessionFactory);
    }

    @AfterEach
    public void teardown() throws Exception {
        worker.stop();
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testWorkerProcessesRequestSuccessfully() throws Exception {
        // Prepare request
        SatelliteDownloadRequest req = new SatelliteDownloadRequest();
        req.setInitialDay("2023-01-01T00:00:00.000Z");
        req.setFinalDay("2023-01-10T00:00:00.000Z");
        req.setGeoJson("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]}");

        // Prepare mocks for provider
        Tile mockedTile = new CopernicusTile("123", "TILE_1", "md5", "2023-01-01", "2023-01-10", "POLYGON",
                "http://preview.link");
        when(provider.getTile(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(mockedTile));
        doNothing().when(provider).downloadTile(any(Tile.class));

        // Start worker and provide work
        worker.start();
        queue.put(req);

        // Wait a short time for processing
        Thread.sleep(500);

        // Verifications
        verify(sessionFactory, atLeastOnce()).openSession();
        verify(session, atLeastOnce()).beginTransaction();
        verify(transaction, atLeastOnce()).commit();
        verify(provider, atLeastOnce()).downloadTile(any(Tile.class));
    }
}
