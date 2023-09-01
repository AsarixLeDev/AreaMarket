package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.*;
import ch.asarix.areamarkets.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class AreaManager {

    private final AreaMarkets plugin;
    private final List<Area> areas = new ArrayList<>();
    private File storageFolder;
    private int signUpdateTaskId;

    public AreaManager(AreaMarkets plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File folder = plugin.getDataFolder();
        if (!folder.exists())
            folder.mkdir();
        storageFolder = new File(folder, "areas");
        if (!storageFolder.exists()) {
            storageFolder.mkdir();
        } else {
            File[] files = storageFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    JsonNode node = JsonUtil.readFile(file);
                    Iterator<String> truc = node.fieldNames();
                    while (truc.hasNext()) {
                        String areaName = truc.next();
                        JsonNode areaNode = node.get(areaName);
                        Location from = JsonUtil.readLocation(areaNode.get("from"));
                        Location to = JsonUtil.readLocation(areaNode.get("to"));
                        String ownerFactionId = areaNode.get("faction_id").asText();
                        Area area = new Area(areaName, from, to, ownerFactionId);
                        JsonNode signNode = areaNode.get("deal_sign");

                        if (signNode.has("location") && signNode.has("price") && signNode.has("type")) {
                            Location signLocation = JsonUtil.readLocation(signNode.get("location"));
                            double signPrice = signNode.get("price").asDouble();
                            DealType dealType = DealType.valueOf(signNode.get("type").asText().toUpperCase());
                            area.setDealSign(new DealSign(signLocation, dealType, signPrice));
                        }

                        JsonNode dealNode = areaNode.get("current_deal");

                        if (dealNode.has("customer")
                                && dealNode.has("price")
                                && dealNode.has("type")
                                && dealNode.has("expiration_date")
                                && dealNode.has("access")) {
                            UUID customer = UUID.fromString(dealNode.get("customer").asText());
                            double dealPrice = dealNode.get("price").asDouble();
                            DealType dealType = DealType.valueOf(dealNode.get("type").asText().toUpperCase());
                            long expirationDate = dealNode.get("expiration_date").asLong();
                            Iterator<JsonNode> elements = dealNode.get("access_list").elements();
                            Iterable<JsonNode> iterable = () -> elements;
                            List<UUID> access = StreamSupport.stream(iterable.spliterator(), false)
                                    .map(accessNode -> UUID.fromString(accessNode.asText())).toList();
                            AreaDeal deal;
                            if (dealType == DealType.PURCHASE) {
                                deal = new PurchaseDeal(customer, dealPrice, access);
                            } else {
                                deal = new RentDeal(customer, dealPrice, expirationDate, access);
                            }
                            area.setCurrentDeal(deal);
                        }

                        areas.add(area);
                    }
                }
            }
        }
        signUpdateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Area area : plugin.getAreaManager().getAreas()) {
                //Should update sign if the return value is null
                area.getCurrentDeal();
            }
        }, 20 * 60L, 20 * 60L);
    }

    public void disable() {
        Bukkit.getScheduler().cancelTask(signUpdateTaskId);
        for (Area area : areas) saveArea(area);
    }

    public List<Area> getAreas(Faction faction) {
        return areas.stream().filter(area -> area.getOwnerFaction() == faction).toList();
    }

    public List<Area> getAreas() {
        return areas;
    }

    private File getFile(Faction faction) {
        File factionFile = new File(storageFolder, faction.getId() + ".json");
        if (!factionFile.exists()) {
            try {
                factionFile.createNewFile();
                return factionFile;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return factionFile;
    }

    private File getFile(String fileName) {
        File factionFile = new File(storageFolder, fileName + ".json");
        if (!factionFile.exists()) {
            try {
                factionFile.createNewFile();
                return factionFile;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return factionFile;
    }

    private ObjectNode getObjectNode(Faction faction) {
        File factionFile = getFile(faction);
        if (factionFile == null) return null;
        return JsonUtil.getObjectNode(factionFile, true);
    }

    private ObjectNode getObjectNode(String factionId) {
        File factionFile = getFile(factionId);
        if (factionFile == null) return null;
        return JsonUtil.getObjectNode(factionFile, true);
    }

    public void saveArea(Faction faction, String areaName, AreaSession session) {
        Area area = new Area(areaName, session.getFrom(), session.getTo(), faction.getId());
        saveArea(area);
    }

    public void saveArea(Area area) {
        Faction ownerFaction = area.getOwnerFaction();
        if (!exists(ownerFaction, area.getName())) {
            areas.add(area);
        }
        File factionFile = getFile(ownerFaction);
        ObjectNode objectNode = getObjectNode(ownerFaction);
        if (objectNode == null) return;
        ObjectNode areaNode = JsonNodeFactory.instance.objectNode();
        areaNode.put("name", area.getName());
        areaNode.set("from", JsonUtil.writeLocation(area.getFrom()));
        areaNode.set("to", JsonUtil.writeLocation(area.getTo()));
        areaNode.put("faction_id", ownerFaction.getId());
        DealSign dealSign = area.getDealSign();
        ObjectNode signNode = JsonNodeFactory.instance.objectNode();
        if (dealSign != null) {
            signNode.set("location", JsonUtil.writeLocation(dealSign.getLocation()));
            signNode.put("type", dealSign.getDealType().name());
            signNode.put("price", dealSign.getPrice());
        }
        areaNode.set("deal_sign", signNode);
        ObjectNode dealNode = JsonNodeFactory.instance.objectNode();
        AreaDeal deal = area.getCurrentDeal();
        if (deal != null) {
            dealNode.put("customer", deal.customer().toString());
            dealNode.put("price", deal.price());
            dealNode.put("type", deal.dealType().name());
            dealNode.put("expiration_date", deal.expirationDate());
            ArrayNode access = JsonNodeFactory.instance.arrayNode();
            for (UUID uuid : deal.access()) {
                access.add(uuid.toString());
            }
            dealNode.set("access_list", access);
        }
        areaNode.set("current_deal", dealNode);
        objectNode.set(area.getName(), areaNode);
        JsonUtil.write(factionFile, objectNode);
    }

    public boolean exists(Faction faction, String areaName) {
        return getAreaByName(faction, areaName) != null;
    }

    public Area getAreaByName(Faction faction, String areaName) {
        return areas.stream()
                .filter(area -> area.getOwnerFaction() == faction
                        && area.getName().equals(areaName))
                .findAny().orElse(null);
    }

    public Area getAreaBySignLocation(Location location) {
//        areas.forEach(area -> {
//            if (area.getDealSign() != null) {
//                System.out.println(StringUtil.locToString(area.getDealSign().getLocation()) + " " + StringUtil.locToString(location));
//                System.out.println(area.getDealSign().getLocation().getWorld().getBlockAt(area.getDealSign().getLocation()).getType().name());
//                System.out.println(area.getDealSign().getLocation().equals(location));
//            }
//        });
        return areas.stream()
                .filter(area -> (area.getDealSign() != null) && (area.getDealSign().getLocation().equals(location)))
                .findAny().orElse(null);
    }

    /**
     * Different from getAreaBySignLocation : finds Area if location is located in it
     */
    public Area getAreaByLocation(Location location) {
        return areas.stream()
                .filter(area -> area.contains(location))
                .findAny().orElse(null);
    }

    public void removeAreas(Faction faction) {
        for (Area area : getAreas(faction)) {
            removeArea(area);
        }
    }

    public void removeArea(Faction faction, String areaName) {
        Area area = getAreaByName(faction, areaName);
        if (area == null) return;
        removeArea(area);
    }

    public void removeArea(Area area) {
        if (area == null) return;
        ObjectNode objectNode = getObjectNode(area.getOwnerFactionId());
        if (objectNode != null) {
            if (objectNode.has(area.getName())) {
                objectNode.remove(area.getName());
            }
        }
        JsonUtil.write(getFile(area.getOwnerFaction()), objectNode);
        if (area.getDealSign() != null) {
            area.getDealSign().getLocation().getBlock().setType(Material.AIR);
        }
        areas.remove(area);
    }
}
