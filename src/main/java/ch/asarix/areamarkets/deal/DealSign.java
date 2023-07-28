package ch.asarix.areamarkets.deal;

import org.bukkit.Location;

public class DealSign {
    private final Location location;
    private final DealType dealType;
    private final double price;

    public DealSign(Location location, DealType dealType, double price) {
        this.dealType = dealType;
        this.location = location;
        this.price = price;
    }

    public DealType getDealType() {
        return dealType;
    }

    public Location getLocation() {
        return location;
    }

    public double getPrice() {
        return price;
    }
}
