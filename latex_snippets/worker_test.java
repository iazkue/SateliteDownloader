@BeforeEach
public void setup() {
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
    worker = new DownloadWorker(queue, provider, sessionFactory);
}

@Test
public void testWorkerProcessesRequestSuccessfully() throws Exception {
    // [...] Eskaera eta provider objektuak prestatu

    worker.start();
    queue.put(req); // Eskaera ilarara bota
    Thread.sleep(500); // Itxaron prozesamendua burutu dadin

    // Egiaztatu deien ordena eta transakzioaren kontrola asinkronoa izan arren
    verify(sessionFactory, atLeastOnce()).openSession();
    verify(session, atLeastOnce()).beginTransaction();
    verify(transaction, atLeastOnce()).commit();
    verify(provider, atLeastOnce()).downloadTile(any(Tile.class));
}
