package ch.asarix.areamarkets.deal;

import java.util.*;

public class RentDeal implements AreaDeal {

    private final UUID customer;
    private final double price;
    private final long startTimeMillis;
    private long duration;
    private final Set<UUID> access;


    public RentDeal(UUID customer, double price, long expirationDate) {
        this(customer, price, expirationDate, new ArrayList<>());
    }

    public RentDeal(UUID customer, double price, long expirationDate, List<UUID> access) {
        this.customer = customer;
        this.price = price;
        this.duration = expirationDate - System.currentTimeMillis();
        this.startTimeMillis = System.currentTimeMillis();
        this.access = new HashSet<>(access);
        this.access.add(customer);
    }

    public List<UUID> access() {
        return access.stream().toList();
    }

    @Override
    public void addAccess(UUID user) {
        access.add(user);
    }

    public void addDuration(long duration) {
        this.duration += duration;
    }

    @Override
    public boolean isExpired() {
        return expirationDate() < System.currentTimeMillis();
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
        return DealType.RENT;
    }

    @Override
    public long expirationDate() {
        return startTimeMillis + duration;
    }
}
