package tfg.satelitedownloader.core;

import java.io.IOException;
import java.util.List;

public interface Provider {
    List<Tile> getTile(String name, String dateStart, String dateEnd, String area)
            throws IOException, InterruptedException;

    void downloadTile(Tile tile) throws IOException, InterruptedException;
}
