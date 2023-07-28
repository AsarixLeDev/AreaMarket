package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.AreaDeal;
import com.massivecraft.factions.*;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class AreaProtectionListener implements Listener {

    private final AreaManager areaManager;

    public AreaProtectionListener(AreaMarkets plugin) {
        this.areaManager = plugin.getAreaManager();
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onAreaBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Area area = areaManager.getAreaByLocation(event.getBlock().getLocation());
        if (area == null) return;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer.getFaction() != area.getOwnerFaction()) return;
        AreaDeal deal = area.getCurrentDeal();
        if (deal == null) return;
        if (!deal.access().contains(player.getUniqueId())) return;
        event.setCancelled(false);
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Area area = areaManager.getAreaByLocation(event.getBlock().getLocation());
        if (area == null) return;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer.getFaction() != area.getOwnerFaction()) return;
        AreaDeal deal = area.getCurrentDeal();
        if (deal == null) return;
        if (!deal.access().contains(player.getUniqueId())) return;
        event.setCancelled(false);
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Area area = areaManager.getAreaByLocation(event.getBlock().getLocation());
        if (area == null) return;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer.getFaction() != area.getOwnerFaction()) return;
        AreaDeal deal = area.getCurrentDeal();
        if (deal == null) return;
        if (!deal.access().contains(player.getUniqueId())) return;
        event.setCancelled(false);
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onFrostWalker(EntityBlockFormEvent event) {
        if (event.getEntity() != null && event.getEntity().getType() == EntityType.PLAYER && event.getBlock() != null) {
            Player player = (Player) event.getEntity();
            Area area = areaManager.getAreaByLocation(event.getBlock().getLocation());
            if (area == null) return;
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            if (fPlayer.getFaction() != area.getOwnerFaction()) return;
            AreaDeal deal = area.getCurrentDeal();
            if (deal == null) return;
            if (!deal.access().contains(player.getUniqueId())) return;
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onFallingBlock(EntityChangeBlockEvent event) {
        if (FactionsPlugin.getInstance().getConfig().getBoolean("Falling-Block-Fix.Enabled")) {
            Faction faction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));
            if (faction.isWarZone() || faction.isSafeZone()) {
                event.getBlock().setType(Material.AIR);
                event.setCancelled(true);
            }

        }
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void FrameRemove(HangingBreakByEntityEvent event) {
        if (event.getRemover() != null) {
            if (event.getRemover() instanceof Player player && event.getEntity().getType().name().contains("ITEM_FRAME")) {
                Area area = areaManager.getAreaByLocation(event.getEntity().getLocation());
                if (area == null) return;
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (fPlayer.getFaction() != area.getOwnerFaction()) return;
                AreaDeal deal = area.getCurrentDeal();
                if (deal == null) return;
                if (!deal.access().contains(player.getUniqueId())) return;
                event.setCancelled(false);
            }

        }
    }

    @EventHandler
    public void onFarmLandDamage(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player player) {
            Area area = areaManager.getAreaByLocation(event.getEntity().getLocation());
            if (area == null) return;
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            if (fPlayer.getFaction() != area.getOwnerFaction()) return;
            AreaDeal deal = area.getCurrentDeal();
            if (deal == null) return;
            if (!deal.access().contains(player.getUniqueId())) return;
            event.setCancelled(false);
        }
    }
}
