package tfg.satelitedownloader.model;

import tfg.satelitedownloader.core.Tile;

public class LandsatTile implements Tile {
    private final String tileName;
    private String entityId;

    public LandsatTile(String tileName, String entityId) {

        this.tileName = tileName;
        this.entityId = entityId;
    }

    @Override
    public String[] getParametersForDownload() {
        return new String[] { this.tileName, this.entityId };
    }

    public String getEntityId() {
        return this.entityId;
    }
}
