package tfg.satelitedownloader.stress;

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
import tfg.satelitedownloader.worker.DownloadWorker;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class QueueLoadTest {

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
    public void testHighLoadConcurrentQueueProcessing() throws Exception {
        // En este test vamos a simular 100 peticiones simultáneas (stress testing) para
        // probar que
        // la cola y el entorno multi-hilo sobreviven sin caerse ni perder memoria.
        int NUM_REQUESTS = 100;
        CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);
        AtomicInteger processedCounter = new AtomicInteger(0);

        // Mocking provider to just increment our processed counter and act quickly
        Tile mockedTile = new CopernicusTile("123", "TILE_1", "md5", "2023-01-01", "2023-01-10", "POLYGON",
                "http://preview.link");
        when(provider.getTile(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(mockedTile));
        doAnswer(invocation -> {
            processedCounter.incrementAndGet();
            latch.countDown();
            return null;
        }).when(provider).downloadTile(any(Tile.class));

        // Start worker
        worker.start();

        // Use 10 threads to furiously add elements to the queue
        ExecutorService producerService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < NUM_REQUESTS; i++) {
            producerService.submit(() -> {
                SatelliteDownloadRequest req = new SatelliteDownloadRequest();
                req.setInitialDay("2023-01-01T00:00:00.000Z");
                req.setFinalDay("2023-01-10T00:00:00.000Z");
                req.setGeoJson("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]}");
                try {
                    queue.put(req);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        producerService.shutdown();
        assertTrue(producerService.awaitTermination(5, TimeUnit.SECONDS), "Producers timed out");

        // Now we wait for the worker to process all elements up to 10 seconds timeout
        boolean finished = latch.await(10, TimeUnit.SECONDS);

        // Verification
        assertTrue(finished, "The asynchronous system has not been able to process the entire load ("s
                + processedCounter.get() + "/" + NUM_REQUESTS + ")");
        verify(provider, times(NUM_REQUESTS)).downloadTile(any(Tile.class));
    }
}
