package tfg.satelitedownloader.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class CopernicusTileDAO extends AbstractDAO<CopernicusTileEntity> {

    public CopernicusTileDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CopernicusTileEntity save(CopernicusTileEntity entity) {
        return persist(entity);
    }

    public List<CopernicusTileEntity> findAll() {
        return currentSession()
                .createQuery("FROM CopernicusTileEntity ORDER BY downloadedAt DESC", CopernicusTileEntity.class)
                .list();
    }

    public Optional<CopernicusTileEntity> findByProductId(String productId) {
        CopernicusTileEntity entity = uniqueResult(
                currentSession()
                        .createQuery("FROM CopernicusTileEntity WHERE productId = :productId",
                                CopernicusTileEntity.class)
                        .setParameter("productId", productId));
        return Optional.ofNullable(entity);
    }
}
