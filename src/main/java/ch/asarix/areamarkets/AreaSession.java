package ch.asarix.areamarkets;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class AreaSession {

    //Left clicked block
    private Location from = null;
    //Right clicked block
    private Location to = null;

    public AreaSession(Location from, Location to) {
        this.from = from;
        this.to = to;
    }

    public AreaSession() {
    }

    public Location getFrom() {
        return from;
    }

    public void setFrom(Location from) {
        this.from = from;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        this.to = to;
    }

    public boolean isComplete() {
        return from != null
                && to != null
                && from.getWorld() != null
                && from.getWorld().equals(to.getWorld());
    }

    public List<Chunk> getChunksInArea() {
        if (!isComplete()) return new ArrayList<>();
        World world = from.getWorld();
        if (world == null) return new ArrayList<>();

        int minChunkX = Math.min(from.getBlockX(), to.getBlockX()) >> 4;
        int minChunkZ = Math.min(from.getBlockZ(), to.getBlockZ()) >> 4;

        int maxChunkX = Math.max(from.getBlockX(), to.getBlockX()) >> 4;
        int maxChunkZ = Math.max(from.getBlockZ(), to.getBlockZ()) >> 4;

        List<Chunk> chunks = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}
