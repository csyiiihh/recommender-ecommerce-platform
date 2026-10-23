package org.example.springboot.mapper;
import org.apache.ibatis.annotations.*;
import org.example.springboot.entity.InventoryReservation;
@Mapper
public interface InventoryReservationMapper {
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'inventory_reservation'")
    int tableExists();
    @Select("SELECT COUNT(*) FROM inventory_reservation WHERE state = 1")
    long countHeld();
    @Select("SELECT order_id, product_id, quantity, state FROM inventory_reservation WHERE order_id = #{id}")
    InventoryReservation find(@Param("id") Long id);
    @Insert("INSERT INTO inventory_reservation(order_id, product_id, quantity, state) VALUES(#{orderId}, #{productId}, #{quantity}, 1)")
    int insert(@Param("orderId") Long orderId, @Param("productId") Long productId, @Param("quantity") int quantity);
    @Update("UPDATE inventory_reservation SET state = #{state}, updated_at = CURRENT_TIMESTAMP WHERE order_id = #{id} AND state = 1")
    int transition(@Param("id") Long id, @Param("state") int state);
    @Select("SELECT COUNT(*) FROM inventory_reservation r JOIN `order` o ON o.id = r.order_id WHERE o.user_id = #{userId} AND r.state = 1")
    long countBuyerHeldOrders(@Param("userId") Long userId);
    @Select("SELECT COALESCE(SUM(r.quantity), 0) FROM inventory_reservation r JOIN `order` o ON o.id = r.order_id WHERE o.user_id = #{userId} AND r.state = 1")
    long countBuyerHeldUnits(@Param("userId") Long userId);
}
