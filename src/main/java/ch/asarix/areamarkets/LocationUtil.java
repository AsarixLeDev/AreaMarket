package ch.asarix.areamarkets;

import com.massivecraft.factions.shade.apache.tuple.Pair;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static boolean doAreasOverlap(Pair<Location, Location> area1, Pair<Location, Location> area2) {
        return doAreasOverlap(area1.getLeft(), area1.getRight(), area2.getLeft(), area2.getRight());
    }

    public static boolean doAreasOverlap(Location corner1A, Location corner2A, Location corner1B, Location corner2B) {
        World worldA = corner1A.getWorld();
        World worldB = corner1B.getWorld();

        if (worldA == null || !worldA.equals(corner2A.getWorld()) ||
                worldB == null || !worldB.equals(corner2B.getWorld())) {
            // The locations are in different worlds
            return false;
        }

        int minX1 = Math.min(corner1A.getBlockX(), corner2A.getBlockX());
        int minY1 = Math.min(corner1A.getBlockY(), corner2A.getBlockY());
        int minZ1 = Math.min(corner1A.getBlockZ(), corner2A.getBlockZ());

        int maxX1 = Math.max(corner1A.getBlockX(), corner2A.getBlockX());
        int maxY1 = Math.max(corner1A.getBlockY(), corner2A.getBlockY());
        int maxZ1 = Math.max(corner1A.getBlockZ(), corner2A.getBlockZ());

        int minX2 = Math.min(corner1B.getBlockX(), corner2B.getBlockX());
        int minY2 = Math.min(corner1B.getBlockY(), corner2B.getBlockY());
        int minZ2 = Math.min(corner1B.getBlockZ(), corner2B.getBlockZ());

        int maxX2 = Math.max(corner1B.getBlockX(), corner2B.getBlockX());
        int maxY2 = Math.max(corner1B.getBlockY(), corner2B.getBlockY());
        int maxZ2 = Math.max(corner1B.getBlockZ(), corner2B.getBlockZ());

        boolean overlapX = (minX1 <= maxX2) && (maxX1 >= minX2);
        boolean overlapY = (minY1 <= maxY2) && (maxY1 >= minY2);
        boolean overlapZ = (minZ1 <= maxZ2) && (maxZ1 >= minZ2);

        return overlapX && overlapY && overlapZ;
    }

    public static boolean isLocationInArea(Location location, Location areaFrom, Location areaTo) {
        double minX = Math.min(areaFrom.getX(), areaTo.getX());
        double minY = Math.min(areaFrom.getY(), areaTo.getY());
        double minZ = Math.min(areaFrom.getZ(), areaTo.getZ());

        double maxX = Math.max(areaFrom.getX(), areaTo.getX());
        double maxY = Math.max(areaFrom.getY(), areaTo.getY());
        double maxZ = Math.max(areaFrom.getZ(), areaTo.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public static Pair<Location, Location> getChunkCorners(Chunk chunk) {
        World world = chunk.getWorld();

        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Corner 1: Lower X and Z values
        Location corner1 = new Location(world, chunkX * 16, 256, chunkZ * 16);

        // Corner 2: Higher X and Z values
        Location corner2 = new Location(world, chunkX * 16 + 15, -64, chunkZ * 16 + 15);

        return Pair.of(corner1, corner2);
    }
}
