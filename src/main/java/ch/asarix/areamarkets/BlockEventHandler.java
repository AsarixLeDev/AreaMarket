package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.AreaDeal;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class BlockEventHandler {

    private final AreaManager areaManager;

    public BlockEventHandler(AreaMarkets plugin) {
        this.areaManager = plugin.getAreaManager();
    }

    private boolean shouldByPassClaim(Location location, Player player) {
        if (location == null) return false;
        if (player == null) return false;
        Area area = areaManager.getAreaByLocation(location);
        if (area == null) return false;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer.getFaction() != area.getOwnerFaction()) return false;
        AreaDeal deal = area.getCurrentDeal();
        if (deal == null) return false;
        return deal.access().contains(player.getUniqueId());
    }

    private boolean shouldByPassClaim(Block block, Player player) {
        return shouldByPassClaim(block.getLocation(), player);
    }

    public boolean shouldByPassClaim(BlockBreakEvent event) {
        return shouldByPassClaim(event.getBlock(), event.getPlayer());
    }

    public boolean shouldByPassClaim(BlockPlaceEvent event) {
        return shouldByPassClaim(event.getBlock(), event.getPlayer());
    }

    public boolean shouldByPassClaim(BlockDamageEvent event) {
        return shouldByPassClaim(event.getBlock(), event.getPlayer());
    }

    public boolean shouldByPassClaim(EntityBlockFormEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.PLAYER || event.getBlock() == null)
            return false;
        return shouldByPassClaim(event.getBlock(), (Player) event.getEntity());

    }

//    public boolean shouldByPassClaim(EntityChangeBlockEvent event) {
//        if (!shouldByPassClaim0(event)) return false;
//        if (FactionsPlugin.getInstance().getConfig().getBoolean("Falling-Block-Fix.Enabled")) {
//            Faction faction = Board.getInstance().getFactionAt(FLocation.wrap(event.getBlock()));
//            if (faction.isWarZone() || faction.isSafeZone()) {
//                event.getBlock().setType(Material.AIR);
//                event.setCancelled(true);
//            }
//
//        }
//    }

    public boolean shouldByPassClaim(HangingBreakByEntityEvent event) {
        if (event.getRemover() == null) return false;
        if (!(event.getRemover() instanceof Player player) || !event.getEntity().getType().name().contains("ITEM_FRAME"))
            return false;
        return shouldByPassClaim(event.getEntity().getLocation(), player);
    }

    public boolean shouldByPassClaim(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) return false;
        return shouldByPassClaim(event.getBlock(), player);
    }
}
