package ch.asarix.areamarkets;

import ch.asarix.areamarkets.deal.AreaDeal;
import ch.asarix.areamarkets.deal.DealSign;
import ch.asarix.areamarkets.deal.DealType;
import ch.asarix.areamarkets.deal.RentDeal;
import ch.asarix.areamarkets.util.UUIDManager;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import java.text.SimpleDateFormat;

public class Area {
    private final String name;
    private final Location from;
    private final Location to;
    private final String ownerFactionId;
    private AreaDeal currentDeal = null;

    private DealSign dealSign = null;

    public Area(String name, Location from, Location to, String ownerFactionId) {
        this.name = name;
        this.from = from;
        this.to = to;
        this.ownerFactionId = ownerFactionId;
    }

    public String getName() {
        return name;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public Faction getOwnerFaction() {
        return Factions.getInstance().getFactionById(ownerFactionId);
    }

    public String getOwnerFactionId() {
        return ownerFactionId;
    }

    public boolean contains(Location location) {
        double minX = Math.min(from.getX(), to.getX());
        double minY = Math.min(from.getY(), to.getY());
        double minZ = Math.min(from.getZ(), to.getZ());

        double maxX = Math.max(from.getX(), to.getX());
        double maxY = Math.max(from.getY(), to.getY());
        double maxZ = Math.max(from.getZ(), to.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public AreaDeal getCurrentDeal() {
        if (currentDeal != null && currentDeal.isExpired()) {
            System.out.println("Deal expired");
            setCurrentDeal(null);
        }
        return currentDeal;
    }

    public void setCurrentDeal(AreaDeal currentDeal) {
        this.currentDeal = currentDeal;
        updateDealSign();
        AreaMarkets.getInstance().getAreaManager().saveArea(this);
    }

    public DealSign getDealSign() {
        return dealSign;
    }

    public void setDealSign(DealSign dealSign) {
        this.dealSign = dealSign;
        AreaMarkets.getInstance().getAreaManager().saveArea(this);
    }

    public void updateDealSign() {
        if (this.dealSign == null) return;
        System.out.println("Editing sign...");
        Location signLocation = this.dealSign.getLocation();
        Block block = signLocation.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            System.err.println("dealsign is not a sign");
            return;
        }
        if (currentDeal != null) {
            sign.setLine(0, "§7[§eZone§7] - " + getName());
            sign.setLine(1, currentDeal.dealType() == DealType.PURCHASE ? "Acheté" : "Loué - " + currentDeal.price() + "$");
            sign.setLine(2, UUIDManager.get().getName(currentDeal.customer()));
            if (currentDeal.dealType() == DealType.RENT) {
                sign.setLine(3, new SimpleDateFormat("dd.MM.yy HH:mm").format(currentDeal.expirationDate()));
            } else {
                sign.setLine(3, "");
            }
        } else {
            sign.setLine(0, "§7[§eZone§7]");
            sign.setLine(1, dealSign.getDealType() == DealType.PURCHASE ? "Achat" : "Location");
            sign.setLine(2, dealSign.getPrice() + "$");
            sign.setLine(3, getName());
        }
        sign.update();
    }

    public void broadcastToOwners(String message) {
        getOwnerFaction().getOnlinePlayers().stream()
                .filter(
                        player -> {
                            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                            return getOwnerFaction().getAccess(fPlayer, PermissableAction.TERRITORY) == Access.ALLOW;
                        }
                )
                .forEach(player -> player.sendMessage(message));
    }
}
