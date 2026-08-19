@Test
public void testHighLoadConcurrentQueueProcessing() throws Exception {
    int NUM_REQUESTS = 100;
    CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);
    AtomicInteger processedCounter = new AtomicInteger(0);

    // Hornitzailearen exekuzioan kontagailuak sinkronizatu
    doAnswer(invocation -> {
        processedCounter.incrementAndGet();
        latch.countDown();
        return null;
    }).when(provider).downloadTile(any(Tile.class));

    worker.start();
    ExecutorService producerService = Executors.newFixedThreadPool(10);
    // [...] 100 eskaera ilaran sartzearen logika paraleloa

    // Prozesuak amaitu arte (gehienez 10 segundu) itxaron
    boolean finished = latch.await(10, TimeUnit.SECONDS);

    assertTrue(finished, "El sistema asíncrono no ha sido capaz de procesar toda la carga.");
    verify(provider, times(NUM_REQUESTS)).downloadTile(any(Tile.class));
}
