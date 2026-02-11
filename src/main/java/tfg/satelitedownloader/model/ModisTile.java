package tfg.satelitedownloader.model;

import tfg.satelitedownloader.core.Tile;

public class ModisTile implements Tile {
    private static final String BASE_URL = "https://ladsweb.modaps.eosdis.nasa.gov/archive/allData/";
    private String targetDirectory;

    public ModisTile(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public String[] getParametersForDownload() {
        return new String[] { this.targetDirectory };
    }
}
