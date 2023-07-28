package ch.asarix.areamarkets.deal;

import java.util.List;
import java.util.UUID;

public interface AreaDeal {

    boolean isExpired();

    double price();

    UUID customer();

    DealType dealType();

    List<UUID> access();

    void addAccess(UUID user);

    long expirationDate();
}
