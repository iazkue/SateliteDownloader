package tfg.satelitedownloader.model;

import tfg.satelitedownloader.core.Tile;

public class CopernicusTile implements Tile {
    private final String productId;
    private final String name;
    private final String md5Hash;
    private final String dateStart;
    private final String dateEnd;
    private final String area;
    private final String previewLink;

    public CopernicusTile(String productId,
            String name,
            String md5Hash,
            String dateStart,
            String dateEnd,
            String area) {
        this(productId, name, md5Hash, dateStart, dateEnd, area, null);
    }

    public CopernicusTile(String productId,
            String name,
            String md5Hash,
            String dateStart,
            String dateEnd,
            String area,
            String previewLink) {
        this.productId = productId;
        this.name = name;
        this.md5Hash = md5Hash;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.area = area;
        this.previewLink = previewLink;
    }

    @Override
    public String[] getParametersForDownload() {
        return new String[] { this.productId };
    }

    public String getName() {
        return name;
    }

    public String getMd5Hash() {
        return md5Hash;
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
}
