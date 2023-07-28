package ch.asarix.areamarkets.deal;

import org.bukkit.Location;
import org.checkerframework.checker.units.qual.A;

import java.util.*;

public class PurchaseDeal implements AreaDeal {

    private final UUID customer;

    private final double price;
    private Location signLocation = null;

    private final Set<UUID> access;

    public PurchaseDeal(UUID customer, double price) {
        this(customer, price, new ArrayList<>());
    }

    public PurchaseDeal(UUID customer, double price, List<UUID> access) {
        this.customer = customer;
        this.price = price;
        this.access = new HashSet<>(access);
        this.access.add(customer);
    }

    @Override
    public List<UUID> access() {
        return access.stream().toList();
    }

    @Override
    public void addAccess(UUID uuid) {
        access.add(uuid);
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public void setSignLocation(Location location) {
        this.signLocation = location;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public long expirationDate() {
        return -1;
    }

    @Override
    public double price() {
        return price;
    }

    @Override
    public UUID customer() {
        return customer;
    }

    @Override
    public DealType dealType() {
        return DealType.PURCHASE;
    }
}
