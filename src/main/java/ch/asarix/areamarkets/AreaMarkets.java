package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.*;
import ch.asarix.areamarkets.util.StringUtil;
import ch.asarix.areamarkets.util.UUIDManager;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class AreaMarkets extends JavaPlugin implements Listener {

    private static AreaMarkets instance;
    private final Map<UUID, AreaSession> sessionMap = new HashMap<>();
    private NamespacedKey namespacedKey;
    private AreaManager areaManager;
    private BlockEventHandler blockEventHandler;
    private Economy econ = null;

    public static AreaMarkets getInstance() {
        return instance;
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public BlockEventHandler getBlockEventHandler() {
        return blockEventHandler;
    }

    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public AreaSession getSession(UUID uuid) {
        return sessionMap.get(uuid);
    }

    public void removeSession(UUID uuid) {
        sessionMap.remove(uuid);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        namespacedKey = new NamespacedKey(this, "area_item");
        areaManager = new AreaManager(this);
        areaManager.init();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new AreaBlockListener(), this);
        this.blockEventHandler = new BlockEventHandler(this);
        getCommand("zone").setExecutor(new AreaCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        areaManager.disable();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDealSignBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (event.isCancelled()) return;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer.hasFaction()) {
            Faction faction = fPlayer.getFaction();
            Area area = areaManager.getAreaBySignLocation(event.getBlock().getLocation());
            if (area != null) {
                if (area.getOwnerFaction() != faction) {
                    player.sendMessage("Ce panneau est protégé pour la zone de la faction " + area.getOwnerFaction().getTag() + " !");
                    event.setCancelled(true);
                    return;
                }
                if (faction.getAccess(fPlayer, PermissableAction.TERRITORY) != Access.ALLOW) {
                    player.sendMessage("Vous n'avez pas les permissions nécessaires pour faire cela !");
                    return;
                }
                player.sendMessage("Panneau de vente/location de la zone " + area.getName() + " supprimé.");
                area.setDealSign(null);
            }
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemMeta meta = heldItem.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(namespacedKey, PersistentDataType.BYTE)) return;
        AreaSession session;
        if (sessionMap.containsKey(player.getUniqueId())) {
            session = sessionMap.get(player.getUniqueId());
        } else {
            session = new AreaSession();
            sessionMap.put(player.getUniqueId(), session);
        }
        session.setFrom(event.getBlock().getLocation());
        player.sendMessage("La sélection de départ a été assignée au bloc de position "
                + StringUtil.locToString(event.getBlock().getLocation()));
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemMeta meta = heldItem.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(namespacedKey, PersistentDataType.BYTE)) return;
        AreaSession session;
        if (sessionMap.containsKey(player.getUniqueId())) {
            session = sessionMap.get(player.getUniqueId());
        } else {
            session = new AreaSession();
            sessionMap.put(player.getUniqueId(), session);
        }
        session.setTo(block.getLocation());
        player.sendMessage("La sélection d'arrivée a été assignée au bloc de position "
                + StringUtil.locToString(block.getLocation()));
        event.setCancelled(true);
    }

    @EventHandler
    public void onDealSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Area area = areaManager.getAreaBySignLocation(block.getLocation());
        if (area == null) return;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction ownerFaction = area.getOwnerFaction();
        if (!fPlayer.hasFaction() || fPlayer.getFaction() != ownerFaction) return;
        DealSign dealSign = area.getDealSign();
        DealType dealType = dealSign.getDealType();
        if (area.getCurrentDeal() != null) {
            if (area.getCurrentDeal().dealType() == DealType.PURCHASE) {
                if (area.getCurrentDeal().customer() == player.getUniqueId()) {
                    player.sendMessage("Vous possédez déjà cette zone !");
                    return;
                }
                player.sendMessage(UUIDManager.get().getName(area.getCurrentDeal().customer()) + " possède déjà cette zone !");
                return;
            }
            if (area.getCurrentDeal().customer() != player.getUniqueId()) {
                player.sendMessage(UUIDManager.get().getName(area.getCurrentDeal().customer()) + " loue déjà cette zone !");
            } else {
                double playerPurse = econ.getBalance(player);
                if (playerPurse < dealSign.getPrice()) {
                    player.sendMessage("Vous n'avez pas de quoi prolonger votre location de cette zone !");
                    return;
                }
                econ.withdrawPlayer(player, dealSign.getPrice());
                ownerFaction.setFactionBalance(ownerFaction.getFactionBalance() + dealSign.getPrice());
                area.broadcastToOwners("Le joueur " + player.getName() + " a loué, pour une journée de plus, la zone "
                        + area.getName() + " pour " + dealSign.getPrice() + "$.");
                ((RentDeal) area.getCurrentDeal()).addDuration(TimeUnit.DAYS.toMillis(1));
                area.updateDealSign();
                player.sendMessage("Vous avez loué la zone " + area.getName() + " à votre faction pour une journée de plus !");
            }
            return;
        }

        double playerPurse = econ.getBalance(player);
        if (dealType == DealType.PURCHASE) {
            if (playerPurse < dealSign.getPrice()) {
                player.sendMessage("Vous n'avez pas de quoi acheter cette zone !");
                return;
            }
            econ.withdrawPlayer(player, dealSign.getPrice());
            ownerFaction.setFactionBalance(ownerFaction.getFactionBalance() + dealSign.getPrice());
            area.broadcastToOwners("Le joueur " + player.getName() + " a acheté la zone "
                    + area.getName() + " pour " + dealSign.getPrice() + "$.");
            AreaDeal areaDeal = new PurchaseDeal(player.getUniqueId(), dealSign.getPrice());
            area.setCurrentDeal(areaDeal);
            player.sendMessage("Vous avez acheté la zone " + area.getName() + " à votre faction !");
            //Purchase
        } else {
            if (playerPurse < dealSign.getPrice()) {
                player.sendMessage("Vous n'avez pas de quoi louer cette zone !");
                return;
            }
            econ.withdrawPlayer(player, dealSign.getPrice());
            ownerFaction.setFactionBalance(ownerFaction.getFactionBalance() + dealSign.getPrice());
            area.broadcastToOwners("Le joueur " + player.getName() + " a loué, pour une journée, la zone "
                    + area.getName() + " pour " + dealSign.getPrice() + "$.");
            AreaDeal areaDeal = new RentDeal(player.getUniqueId(), dealSign.getPrice(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
            area.setCurrentDeal(areaDeal);
            player.sendMessage("Vous avez loué la zone " + area.getName() + " à votre faction pour une journée !");
            //Rent
        }
    }

    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (!fPlayer.hasFaction()) return;
        Faction faction = fPlayer.getFaction();
        String[] lines = event.getLines();
        if (lines.length != 4) return;
        if (!lines[0].equalsIgnoreCase("[zone]")) return;
        boolean buy = true;
        if (lines[1].equalsIgnoreCase("location") || lines[1].equalsIgnoreCase("rent")) {
            buy = false;
        } else if (!lines[1].equalsIgnoreCase("vente") && !lines[1].equalsIgnoreCase("sell")) {
            return;
        }
        double price;
        try {
            price = Double.parseDouble(lines[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("Le prix de votre zone n'est pas valable !");
            return;
        }
        String areaName = lines[3];
        if (!areaManager.exists(faction, areaName)) {
            player.sendMessage(areaName + " : Vous ne possédez pas de zone nommée ainsi !");
            return;
        }
        Area area = areaManager.getAreaByName(faction, areaName);
        //Ne devrait jamais arriver
        if (area.getCurrentDeal() != null) {
            System.err.println("Sign about already sold area exists");
            player.sendMessage("Cette zone est déjà concernée par un contrat !");
            return;
        }
        if (area.getDealSign() != null) {
            player.sendMessage("Cette zone a déjà un panneau qui lui est assigné " + StringUtil.locToString(area.getDealSign().getLocation()));
            return;
        }
        DealSign dealSign = new DealSign(event.getBlock().getLocation(), buy ? DealType.PURCHASE : DealType.RENT, price);
        area.setDealSign(dealSign);
        player.sendMessage("Panneau de vente/location pour la zone " + areaName + " de la faction " + faction.getTag() + " détecté !");
        event.setLine(0, "§7[§eZone§7]");
        event.setLine(1, buy ? "Achat" : "Location");
        event.setLine(2, price + "$");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDisband(FactionDisbandEvent event) {
        areaManager.removeAreas(event.getFaction());
    }
}
