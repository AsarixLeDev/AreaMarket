package ch.asarix.areamarkets.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;

public class JsonUtil {
    public static JsonNode readFile(File file) {
        return readFile(file, false);
    }

    public static JsonNode readFile(File file, boolean createNodeIfEmpty) {
        try {
            return new ObjectMapper().readTree(file);
        } catch (JsonMappingException e) {
            if (createNodeIfEmpty) {
                try {
                    new ObjectMapper().writeValue(file, JsonNodeFactory.instance.objectNode());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode getObjectNode(File file, boolean writeDefaultObjectNode) {
        JsonNode node = readFile(file);
        if (node == null || node instanceof MissingNode) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            if (writeDefaultObjectNode)
                write(file, objectNode);
            return objectNode;
        } else if (node instanceof ObjectNode) {
            return (ObjectNode) node;
        } else {
            throw new RuntimeException("Could not read node properly");
        }
    }

    public static ObjectNode getObjectNode(File file) {
        return getObjectNode(file, false);
    }

    public static Location readLocation(JsonNode node) {
        double x = node.get("x").asDouble();
        double y = node.get("y").asDouble();
        double z = node.get("z").asDouble();
        float pitch = node.get("pitch").floatValue();
        float yaw = node.get("yaw").floatValue();
        String worldName = node.get("world_name").asText();
        return new Location(Bukkit.getWorld(worldName), x, y, z, pitch, yaw);
    }

    public static ObjectNode writeLocation(Location location) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("x", location.getX());
        objectNode.put("y", location.getY());
        objectNode.put("z", location.getZ());
        objectNode.put("pitch", location.getPitch());
        objectNode.put("yaw", location.getYaw());
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();
        objectNode.put("world_name", worldName);
        return objectNode;
    }

    public static void write(File file, JsonNode node) {
        try {
            writeTrow(file, node);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void writeTrow(File file, JsonNode node) throws RuntimeException {
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
