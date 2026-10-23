package org.example.springboot.entity;
import lombok.Data;
@Data
public class InventoryReservation {
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Integer state;
}
