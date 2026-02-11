package tfg.satelitedownloader;

import tfg.satelitedownloader.core.Provider;
import tfg.satelitedownloader.core.Tile;
import tfg.satelitedownloader.service.CopernicusProvider;
import tfg.satelitedownloader.service.LandsatProvider;
import tfg.satelitedownloader.service.ModisProvider;

import java.io.IOException;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        int option = 1;
        Provider tileProvider;

        String name = "";
        String dateStart = "";
        String dateEnd = "";
        String area = "";

        switch (option) {
            case 1 -> {
                tileProvider = new CopernicusProvider();
                name = "SENTINEL-2";
                dateStart = "2024-05-20T00:00:00.000Z";
                dateEnd = "2024-07-21T00:00:00.000Z";
                area = "SRID=4326;POLYGON((12.655118166047592 47.44667197521409,21.39065656328509 48.347694733853245,28.334291357162826 41.877123516783655,17.47086198383573 40.35854475076158,12.655118166047592 47.44667197521409))";
            }
            case 2 -> {
                tileProvider = new LandsatProvider();
                name = "Global Land Survey";
                dateStart = "2012-01-01";
                dateEnd = "2012-12-01";
                area = "44.60847,-99.69639,44.60847,-99.69639";
            }
            case 3 -> tileProvider = new ModisProvider();
            default -> throw new IllegalArgumentException("Invalid option");
        }

        List<Tile> tiles = tileProvider.getTile(name, dateStart, dateEnd, area);
        // for (Tile tile : tiles) {
        // tileProvider.downloadTile(tile);
        // }

        if (!tiles.isEmpty()) {
            tileProvider.downloadTile(tiles.get(0));
        }
    }
}