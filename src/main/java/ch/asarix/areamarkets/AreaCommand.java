package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.DealType;
import ch.asarix.areamarkets.util.StringUtil;
import ch.asarix.areamarkets.util.UUIDManager;
import com.massivecraft.factions.*;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AreaCommand implements CommandExecutor {

    private final AreaMarkets plugin;

    public AreaCommand(AreaMarkets plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        String cmdName = strings.length == 0 ? "" : strings[0];

        switch (cmdName.toLowerCase()) {
            case "remove":
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur !");
                    return true;
                }
                if (strings.length < 2) {
                    commandSender.sendMessage("§cVeuillez spécifier un nom de zone !");
                    return true;
                }
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                Faction targetFaction = null;
                String areaName = strings[1];
                if (commandSender.hasPermission("zone.everyzone") && strings.length > 2) {
                    targetFaction = Factions.getInstance().getBestTagMatch(strings[1]);
                    if (targetFaction == null) {
                        commandSender.sendMessage("§cVeuillez spécifier une faction valable !");
                        return true;
                    }
                    areaName = strings[2];
                }
                if (targetFaction == null) {
                    if (fPlayer.hasFaction()) {
                        targetFaction = fPlayer.getFaction();
                    } else {
                        commandSender.sendMessage("§cVous n'êtes dans aucune faction !");
                        return true;
                    }
                }
                if (targetFaction.getAccess(fPlayer, PermissableAction.TERRITORY) != Access.ALLOW && !commandSender.hasPermission("zone.everyzone")) {
                    commandSender.sendMessage("§cVous n'avez pas les permissions de faire cela !");
                    return true;
                }
                Area areaToRemove = plugin.getAreaManager().getAreaByName(targetFaction, areaName);
                if (areaToRemove == null) {
                    commandSender.sendMessage("§cLa zone spécifiée (" + areaName + ") n'existe pas dans la faction " + targetFaction.getTag() + " !");
                    return true;
                }
                plugin.getAreaManager().removeArea(areaToRemove);
                commandSender.sendMessage("Zone " + areaName + " de la faction " + targetFaction.getTag() + " supprimée avec succès.");
                areaToRemove.broadcastToOwners("§eLa zone " + areaName + " a été supprimée par " + player.getName() + " !");
                break;
            case "list":
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur !");
                    return true;
                }
                fPlayer = FPlayers.getInstance().getByPlayer(player);
                targetFaction = null;
                List<Area> areas = plugin.getAreaManager().getAreas();
                if (commandSender.hasPermission("zone.everyzone")) {
                    if (strings.length > 1) {
                        if (!strings[1].equalsIgnoreCase("all")) {
                            targetFaction = Factions.getInstance().getBestTagMatch(strings[1]);
                            if (targetFaction == null) {
                                commandSender.sendMessage("§cAucune faction n'a pu être trouvée avec un nom semblable à " + strings[1] + ".");
                                return true;
                            }
                        }
                    }
                } else if (!fPlayer.hasFaction()) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur qui appartient à une faction !");
                    return true;
                }
                final Faction finalTargetFaction = targetFaction;
                if (finalTargetFaction != null && !finalTargetFaction.isWilderness()) {
                    areas = areas.stream().filter(area -> area.getOwnerFaction() == finalTargetFaction).toList();
                }
                commandSender.sendMessage("Liste des zones : " + (finalTargetFaction == null ? "Toutes" : finalTargetFaction.getTag()));
                for (Area area : areas) {
                    String line = area.getName() + (area.getDealSign() != null ? StringUtil.locToString(area.getDealSign().getLocation()) : "");
                    if (finalTargetFaction == null) {
                        line += " - " + area.getOwnerFaction().getTag();
                    }
                    if (area.getCurrentDeal() != null) {
                        line += " - " + (area.getCurrentDeal().dealType() == DealType.RENT ? "Loué" : "Acheté")
                                + " par " + UUIDManager.get().getName(area.getCurrentDeal().customer());
                    }
                    commandSender.sendMessage(line);
                }
                break;
            case "give":
                Material material = Material.matchMaterial("woncore_delimitation_flag");
                if (material == null) {
                    System.err.println("Failed to get modded area item");
                    material = Material.NAME_TAG;
                }
                ItemStack areaItem = new ItemStack(material);
                ItemMeta meta = areaItem.getItemMeta();
                if (meta == null) {
                    commandSender.sendMessage("§cQuelque chose s'est mal passé ! Veuillez réessayer, ou contactez les développeurs !");
                    return true;
                }
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(plugin.getNamespacedKey(), PersistentDataType.BYTE, (byte) 1);
                areaItem.setItemMeta(meta);
                Player targetPlayer;
                if (!(commandSender instanceof Player)) {
                    if (strings.length < 2) {
                        commandSender.sendMessage("§cVeuillez spécifier un joueur !");
                        return true;
                    }
                    targetPlayer = Bukkit.getPlayer(strings[1]);
                    if (targetPlayer == null) {
                        commandSender.sendMessage("§c" + strings[1] + " : Ce joueur n'existe pas ou n'est pas en ligne !");
                        return true;
                    }
                } else {
                    targetPlayer = (Player) commandSender;
                }

                targetPlayer.getInventory().addItem(areaItem);
                targetPlayer.sendMessage("Vous avez reçu l'item de zone.");
                break;
            case "save":
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur !");
                    return true;
                }
                fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (!fPlayer.hasFaction()) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur qui appartient à une faction !");
                    return true;
                }
                Faction faction = fPlayer.getFaction();
                if (faction.getAccess(fPlayer, PermissableAction.TERRITORY) != Access.ALLOW) {
                    commandSender.sendMessage("§cVous n'avez pas les permissions de faire cela !");
                    return true;
                }
                AreaSession session = plugin.getSession(player.getUniqueId());
                if (session == null || !session.isComplete()) {
                    commandSender.sendMessage("§cVous n'avez pas encore sélectionné de zone !");
                    return true;
                }
                if (strings.length < 2) {
                    commandSender.sendMessage("§cVeuillez spécifier un nom à votre zone ! \n" +
                            "/zone save <nom_de_votre_zone>");
                    return true;
                }
                areaName = strings[1];
                if (areaName.length() > 6) {
                    commandSender.sendMessage("§cNom de zone trop long : longueur maximale = 6");
                    return true;
                }
                boolean fullyClaimed = true;
                for (Chunk chunk : session.getChunksInArea()) {
                    if (Board.getInstance().getFactionAt(FLocation.wrap(chunk)) != faction) {
                        fullyClaimed = false;
                        break;
                    }
                }
                if (!fullyClaimed) {
                    commandSender.sendMessage("§cLa zone sélectionnée contient des parties qui " +
                            "n'appartiennent pas à votre faction !");
                    return true;
                }
                boolean noOverlap = true;
                for (Area area : plugin.getAreaManager().getAreas()) {
                    if (doAreasOverlap(session.getFrom(), session.getTo(), area.getFrom(), area.getTo())) {
                        noOverlap = false;
                        break;
                    }
                }
                if (!noOverlap) {
                    commandSender.sendMessage("§cUne autre zone est au moins partiellement contenue dans la zone sélectionnée !");
                    return true;
                }
                plugin.getAreaManager().saveArea(faction, areaName, session);
                commandSender.sendMessage("Zone " + areaName + " sauvegardée !");
                plugin.removeSession(player.getUniqueId());
                break;
            case "expand":
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage("§cCette commande ne peut être exécutée " +
                            "qu'uniquement par un joueur !");
                    return true;
                }
                session = plugin.getSession(player.getUniqueId());
                if (session == null || !session.isComplete()) {
                    commandSender.sendMessage("§cVous n'avez pas encore sélectionné de zone !");
                    return true;
                }
                Location from = session.getFrom();
                session.setFrom(new Location(from.getWorld(), from.getX(), 0, from.getZ()));
                Location to = session.getTo();
                session.setTo(new Location(to.getWorld(), to.getX(), 256, to.getZ()));
                commandSender.sendMessage("Zone étendue sur toute la hauteur disponible !");
                break;
            case "help":
            default:
                List<String> lines = List.of(
                        "/zone remove [faction] <nom>",
                        "/zone list [faction] : " + (commandSender.hasPermission("zone.everyzone") ? "Voire toutes les zones" : "Voire les zones de votre faction"),
                        "/zone give [pseudo] : Obtenir l'outil à zone (commande sûrement temporaire)",
                        "/zone save <nom> : Sauvegarder votre zone",
                        "/zone expand : Etendre votre zone sélectionnée à toute la hauteur possible",
                        "/zone help : Afficher ceci"
                );
                for (String line : lines) {
                    commandSender.sendMessage(line);
                }
                //Help
        }
        return false;
    }


    private boolean doAreasOverlap(Location corner1A, Location corner2A, Location corner1B, Location corner2B) {
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
}
