package tfg.satelitedownloader;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.db.DataSourceFactory;
import tfg.satelitedownloader.db.CopernicusTileEntity;
import tfg.satelitedownloader.db.CopernicusTileDAO;
import tfg.satelitedownloader.service.CopernicusProvider;
import tfg.satelitedownloader.api.SateliteDownloaderResource;

public class SateliteDownloaderApplication extends Application<DropWizardConfiguration> {

    private final HibernateBundle<DropWizardConfiguration> hibernateBundle = new HibernateBundle<>(
            CopernicusTileEntity.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(DropWizardConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    public static void main(String[] args) throws Exception {
        new SateliteDownloaderApplication().run(args);
    }

    @Override
    public String getName() {
        return "satelite-downloader";
    }

    @Override
    public void initialize(Bootstrap<DropWizardConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/pagina_web", "/", "index.html"));
        bootstrap.addBundle(hibernateBundle);
    }

    @Override
    public void run(DropWizardConfiguration configuration, Environment environment) {
        environment.jersey().setUrlPattern("/api/*");
        CopernicusTileDAO tileDAO = new CopernicusTileDAO(hibernateBundle.getSessionFactory());
        CopernicusProvider copernicusProvider = new CopernicusProvider(tileDAO);

        java.util.concurrent.LinkedBlockingQueue<tfg.satelitedownloader.api.SatelliteDownloadRequest> queue = new java.util.concurrent.LinkedBlockingQueue<>();
        tfg.satelitedownloader.worker.DownloadWorker worker = new tfg.satelitedownloader.worker.DownloadWorker(
                queue,
                copernicusProvider,
                hibernateBundle.getSessionFactory());
        environment.lifecycle().manage(worker);

        environment.jersey().register(new SateliteDownloaderResource(copernicusProvider, tileDAO, queue));
    }
}
