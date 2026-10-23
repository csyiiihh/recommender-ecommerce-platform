package org.example.springboot.service;

import jakarta.annotation.PostConstruct;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.InventoryReservation;
import org.example.springboot.mapper.InventoryReservationMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Call under the order transaction, with its order row locked (or newly inserted). */
@Service
public class InventoryReservationService {
    private final InventoryReservationMapper reservations;
    private final ProductMapper products;
    private final boolean enabled;
    public InventoryReservationService(InventoryReservationMapper reservations, ProductMapper products,
            @Value("${inventory.reservation.enabled:false}") boolean enabled) {
        this.reservations = reservations; this.products = products; this.enabled = enabled;
    }
    @PostConstruct
    public void validateSchemaAndSwitch() {
        boolean exists = reservations.tableExists() == 1;
        if (enabled && !exists) throw new IllegalStateException("Apply the inventory_reservation migration before enabling inventory.reservation.enabled");
        if (!enabled && exists && reservations.countHeld() > 0) {
            throw new IllegalStateException("Active inventory reservations exist; keep inventory.reservation.enabled=true until they are settled or released");
        }
    }
    public boolean isEnabled() { return enabled; }
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Order order) {
        if (!enabled || !Integer.valueOf(0).equals(order.getOrderType())) return;
        if (order.getId() == null || order.getQuantity() == null || order.getQuantity() < 1) throw new BusinessException("库存预占参数无效");
        InventoryReservation existing = reservations.find(order.getId());
        if (existing != null) {
            validate(order, existing);
            if (existing.getState() == 1) return;
            throw new BusinessException("库存预占已结束");
        }
        if (products.reserveStock(order.getProductId(), order.getQuantity()) != 1) throw new BusinessException("可售库存不足");
        if (reservations.insert(order.getId(), order.getProductId(), order.getQuantity()) != 1) throw new BusinessException("保存库存预占失败");
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasHeld(Order order) {
        if (!enabled || !Integer.valueOf(0).equals(order.getOrderType())) return false;
        InventoryReservation row = reservations.find(order.getId());
        if (row == null) return false; // Legacy order created before enabling reservations.
        validate(order, row);
        return row.getState() == 1;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean consume(Order order) {
        if (!enabled || !Integer.valueOf(0).equals(order.getOrderType())) return false;
        InventoryReservation row = reservations.find(order.getId());
        if (row == null) return false;
        validate(order, row);
        if (row.getState() == 2) return true;
        if (row.getState() != 1) throw new BusinessException("库存预占已释放");
        if (reservations.transition(order.getId(), 2) != 1 || products.recordReservedSale(order.getProductId(), order.getQuantity()) != 1) throw new BusinessException("结算库存预占失败");
        return true;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Order order) {
        if (!enabled || !Integer.valueOf(0).equals(order.getOrderType())) return;
        InventoryReservation row = reservations.find(order.getId());
        if (row == null) return;
        validate(order, row);
        if (row.getState() != 1) return;
        if (reservations.transition(order.getId(), 3) != 1 || products.releaseReservedStock(order.getProductId(), order.getQuantity()) != 1) throw new BusinessException("释放库存预占失败");
    }
    private void validate(Order order, InventoryReservation row) {
        if (!java.util.Objects.equals(order.getProductId(), row.getProductId()) || !java.util.Objects.equals(order.getQuantity(), row.getQuantity())) throw new BusinessException("库存预占与订单不一致");
    }
}
