package ch.asarix.areamarkets.util;

import org.bukkit.Location;

public class StringUtil {
    public static String locToString(Location location) {
        return "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
    }
}
