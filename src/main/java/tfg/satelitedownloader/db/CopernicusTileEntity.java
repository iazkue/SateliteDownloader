package tfg.satelitedownloader.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "copernicus_tiles")
public class CopernicusTileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "md5_hash")
    private String md5Hash;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "date_start")
    private String dateStart;

    @Column(name = "date_end")
    private String dateEnd;

    @Column(name = "area", length = 2048)
    private String area;

    @Column(name = "preview_link")
    private String previewLink;

    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    protected CopernicusTileEntity() {
        // For Hibernate
    }

    public CopernicusTileEntity(String productId,
            String name,
            String md5Hash,
            String filePath,
            String dateStart,
            String dateEnd,
            String area,
            String previewLink,
            Instant downloadedAt,
            Long fileSizeBytes) {
        this.productId = productId;
        this.name = name;
        this.md5Hash = md5Hash;
        this.filePath = filePath;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.area = area;
        this.previewLink = previewLink;
        this.downloadedAt = downloadedAt;
        this.fileSizeBytes = fileSizeBytes;
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getDateStart() {
        return dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public String getArea() {
        return area;
    }

    public String getPreviewLink() {
        return previewLink;
    }

    public Instant getDownloadedAt() {
        return downloadedAt;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }
}
