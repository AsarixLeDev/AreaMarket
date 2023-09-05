package ch.asarix.areamarkets;

import com.massivecraft.factions.*;
import com.massivecraft.factions.event.LandClaimEvent;
import com.massivecraft.factions.event.LandUnclaimEvent;
import com.massivecraft.factions.shade.xseries.XMaterial;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.CC;
import com.massivecraft.factions.util.ItemBuilder;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AreaBlockListener implements Listener {

    private static final AreaManager areaManager = AreaMarkets.getInstance().getAreaManager();

    public static boolean playerCanBuildDestroyBlock(Player player, Location location) {
        if (Conf.playersWhoBypassAllProtection.contains(player.getName())) return true;

        FPlayer me = FPlayers.getInstance().getById(player.getUniqueId().toString());
        if (me.isAdminBypassing()) return true;

        Area area = areaManager.getAreaByLocation(location);
        if (area == null) return true;
        if (!area.hasProtection()) return true;
        FLocation loc = FLocation.wrap(location);
        Faction otherFaction = Board.getInstance().getFactionAt(loc);
        return area.getCurrentDeal().access().contains(player.getUniqueId()) || ((otherFaction == area.getOwnerFaction()) && (me.getFaction() == area.getOwnerFaction()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.canBuild()) return;
        if (event.getBlockPlaced().getType() == Material.FIRE) return;
        boolean isSpawner = event.getBlock().getType().equals(XMaterial.SPAWNER.parseMaterial());
        if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (isSpawner) {
            if (Conf.spawnerLock) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(CC.translate(TL.COMMAND_SPAWNER_LOCK_CANNOT_PLACE.toString()));
            }
        }
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!Conf.handleExploitLiquidFlow) return;
        if (event.getBlock().isLiquid()) {
            if (event.getToBlock().isEmpty()) {
                Faction from = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));
                Faction to = Board.getInstance().getFactionAt(FLocation.wrap(event.getToBlock()));
                if (from == to || to.isWilderness()) return;
                // from faction != to faction
                if (to.isSystemFaction()) {
                    event.setCancelled(true);
                    return;
                }

                if (to.isNormal()) {
                    if (from.isNormal() && from.getRelationTo(to).isAlly()) {
                        return;
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getInstaBreak() && !playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {

        if (!Conf.pistonProtectionThroughDenyBuild) return;
        Faction pistonFaction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));

        // target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
        Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

        // if potentially pushing into air/water/lava in another territory, we need to check it out
        if ((targetBlock.isEmpty() || targetBlock.isLiquid()) && !canPistonMoveBlock(pistonFaction, targetBlock.getLocation()))
            event.setCancelled(true);
    }


    @EventHandler
    public void onVaultPlace(BlockPlaceEvent e) {

        if (e.getItemInHand().getType() == Material.CHEST) {

            ItemStack vault = new ItemBuilder(Material.CHEST)
                    .amount(1).name(FactionsPlugin.instance.getConfig().getString("fvault.Item.Name"))
                    .lore(FactionsPlugin.instance.getConfig().getStringList("fvault.Item.Lore"))
                    .build();

            if (e.getItemInHand().isSimilar(vault)) {
                FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());
                if (fme.getFaction().getVault() != null) {
                    fme.msg(TL.COMMAND_GETVAULT_ALREADYSET);
                    e.setCancelled(true);
                    return;
                }
                FLocation flocation = FLocation.wrap(e.getBlockPlaced().getLocation());
                if (Board.getInstance().getFactionAt(flocation) != fme.getFaction()) {
                    fme.msg(TL.COMMAND_GETVAULT_INVALIDLOCATION);
                    e.setCancelled(true);
                    return;
                }
                Block start = e.getBlockPlaced();
                int radius = 1;
                for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
                    for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
                        for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
                            Location blockLoc = new Location(e.getPlayer().getWorld(), x, y, z);
                            if (blockLoc.getX() == start.getLocation().getX() && blockLoc.getY() == start.getLocation().getY() && blockLoc.getZ() == start.getLocation().getZ()) {
                                continue;
                            }
                            Material blockMaterial = blockLoc.getBlock().getType();
                            if (blockMaterial == Material.CHEST || (FactionsPlugin.instance.getConfig().getBoolean("fvault.No-Hoppers-near-vault") && blockMaterial == Material.HOPPER)) {
                                e.setCancelled(true);
                                fme.msg(TL.COMMAND_GETVAULT_CHESTNEAR);
                                return;
                            }
                        }
                    }
                }
                fme.msg(TL.COMMAND_GETVAULT_SUCCESS);
                fme.getFaction().setVault(e.getBlockPlaced().getLocation());

            }
        }
    }

    @EventHandler
    public void onHopperPlace(BlockPlaceEvent e) {

        if (e.getItemInHand().getType() != Material.HOPPER && !FactionsPlugin.instance.getConfig().getBoolean("fvault.No-Hoppers-near-vault"))
            return;
        Faction factionAt = Board.getInstance().getFactionAt(FLocation.wrap(e.getBlockPlaced().getLocation()));
        if (factionAt.isWilderness() || factionAt.getVault() == null) return;
        FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());
        Block start = e.getBlockPlaced();
        int radius = 1;
        for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
            for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
                for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
                    Location blockLoc = new Location(e.getPlayer().getWorld(), x, y, z);
                    if (blockLoc.getX() == start.getLocation().getX() && blockLoc.getY() == start.getLocation().getY() && blockLoc.getZ() == start.getLocation().getZ()) {
                        continue;
                    }

                    if (blockLoc.getBlock().getType() == XMaterial.CHEST.parseMaterial()) {
                        if (factionAt.getVault().equals(blockLoc)) {
                            e.setCancelled(true);
                            fme.msg(TL.COMMAND_VAULT_NO_HOPPER);
                            return;
                        }
                    }
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {


        // if not a sticky piston, retraction should be fine
        if (!event.isSticky() || !Conf.pistonProtectionThroughDenyBuild) return;

        Location targetLoc = event.getRetractLocation();
        Faction otherFaction = Board.getInstance().getFactionAt(FLocation.wrap(targetLoc));

        // Check if the piston is moving in a faction's territory. This disables pistons entirely in faction territory.
        if (otherFaction.isNormal() && FactionsPlugin.instance.getConfig().getBoolean("disable-pistons-in-territory", false)) {
            event.setCancelled(true);
            return;
        }

        // if potentially retracted block is just air/water/lava, no worries
        if (targetLoc.getBlock().isEmpty() || targetLoc.getBlock().isLiquid()) return;
        Faction pistonFaction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));
        if (!canPistonMoveBlock(pistonFaction, targetLoc)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFrostWalker(EntityBlockFormEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.PLAYER || event.getBlock() == null)
            return;
        Player player = (Player) event.getEntity();
        Location location = event.getBlock().getLocation();

        // only notify every 10 seconds
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        boolean justCheck = fPlayer.getLastFrostwalkerMessage() + 10000 > System.currentTimeMillis();
        if (!justCheck) fPlayer.setLastFrostwalkerMessage();

        // Check if they have build permissions here. If not, block this from happening.
        if (!playerCanBuildDestroyBlock(player, location))
            event.setCancelled(true);
    }

    @EventHandler
    public void onFallingBlock(EntityChangeBlockEvent event) {


        if (!FactionsPlugin.getInstance().getConfig().getBoolean("Falling-Block-Fix.Enabled"))
            return;
        Faction faction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));
        if (faction.isWarZone() || faction.isSafeZone()) {
            event.getBlock().setType(Material.AIR);
            event.setCancelled(true);
        }
    }

    private boolean canPistonMoveBlock(Faction pistonFaction, Location target) {
        Area area = areaManager.getAreaByLocation(target);
        if (area == null) return true;
        if (!area.hasProtection()) return true;

        if (pistonFaction == area.getOwnerFaction()) return true;

        Relation rel = pistonFaction.getRelationTo(area.getOwnerFaction());
        return !rel.confDenyBuild(area.getOwnerFaction().hasPlayersOnline());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        //If there is an error its much safer to not allow the block to be broken
        try {
            Block block = event.getBlock();

            Faction at = Board.getInstance().getFactionAt(FLocation.wrap(block));
            boolean isSpawner = event.getBlock().getType().equals(XMaterial.matchXMaterial("MOB_SPAWNER").get().parseMaterial());
            if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }

            FPlayer fme = FPlayers.getInstance().getByPlayer(event.getPlayer());
            if (fme == null || !fme.hasFaction()) return;

            if (isSpawner) {
                Access access = fme.getFaction().getAccess(fme, PermissableAction.SPAWNER);
                if (access != Access.ALLOW && fme.getRole() != Role.LEADER) {
                    fme.msg(TL.GENERIC_FPERM_NOPERMISSION, "mine spawners");
                }
            }
        } catch (Exception e) {
            event.setCancelled(true);
            e.printStackTrace();
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void FrameRemove(HangingBreakByEntityEvent event) {
        if (event.getRemover() == null) return;
        if ((event.getRemover() instanceof Player)) {
            if (event.getEntity().getType().name().contains("ITEM_FRAME")) {
                Player p = (Player) event.getRemover();
                if (!playerCanBuildDestroyBlock(p, event.getEntity().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onFarmLandDamage(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!playerCanBuildDestroyBlock(player, event.getBlock().getLocation())) {
                FPlayer me = FPlayers.getInstance().getByPlayer(player);
                Faction otherFaction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock().getLocation()));
                me.msg(TL.ACTION_DENIED_OTHER, otherFaction.getTag(), "trample crops");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClaim(LandClaimEvent event) {
        List<Area> areas = areaManager.getAreasInChunk(event.getLocation().getChunk());
        for (Area area : areas) {
            if (event.getFaction() != area.getOwnerFaction()) {
                event.getFaction().getFPlayers().stream()
                        .filter(fPlayer -> event.getFaction().getAccess(fPlayer, PermissableAction.TERRITORY) == Access.ALLOW)
                        .forEach(fPlayer -> fPlayer.sendMessage("§cCould not claim some chunk because an area that does not belong to your faction was in it."));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onUnClaim(LandUnclaimEvent event) {
        areaManager.getAreasInChunk(event.getLocation().getChunk()).forEach(Area::activateProtection);
    }
}