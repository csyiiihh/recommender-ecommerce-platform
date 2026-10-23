package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentAndDeliveryTest {
    @Mock OrderMapper orders;
    @Mock LogisticsMapper logistics;
    @Mock GroupBuyTeamMapper teams;
    @Mock AccessService access;
    @InjectMocks LogisticsService shipping;
    @InjectMocks OrderService orderService;
    @BeforeEach void policy() {
        var policy = new FulfillmentPolicy(teams);
        ReflectionTestUtils.setField(shipping,"fulfillment",policy);
        ReflectionTestUtils.setField(orderService,"fulfillment",policy);
    }
    Order order(int state) { Order o=new Order();o.setId(1L);o.setUserId(9L);o.setStatus(state);o.setOrderType(0);return o; }
    Logistics shipment(int state) { Logistics l=new Logistics();l.setId(2L);l.setOrderId(1L);l.setStatus(state);l.setCompanyName("物流");l.setReceiverName("买家");l.setReceiverPhone("13800000000");l.setReceiverAddress("测试地址");return l; }
    void lock(Order o,Logistics l) {
        when(logistics.selectById(2L)).thenReturn(l);when(orders.selectForUpdate(1L)).thenReturn(o);when(logistics.selectForUpdate(2L)).thenReturn(l);
    }
    @Test void creatingShipmentCannotBypassGroupCompletion() {
        Order o=order(1);o.setOrderType(1);o.setGroupBuyTeamId(3L);GroupBuyTeam team=new GroupBuyTeam();team.setStatus(0);
        when(orders.selectForUpdate(1L)).thenReturn(o);when(teams.selectById(3L)).thenReturn(team);
        assertThrows(BusinessException.class,()->shipping.createLogistics(shipment(0)));verifyNoInteractions(logistics);assertEquals(1,o.getStatus());
    }
    @Test void orderStatusEndpointUsesTheSameGroupGuard() {
        Order o=order(1);o.setOrderType(1);o.setGroupBuyTeamId(3L);GroupBuyTeam team=new GroupBuyTeam();team.setStatus(0);
        when(orders.selectForUpdate(1L)).thenReturn(o);when(teams.selectById(3L)).thenReturn(team);when(access.isMerchant()).thenReturn(true);
        assertThrows(BusinessException.class,()->orderService.updateOrderStatus(1L,2));verify(orders,never()).updateById(any(Order.class));
    }
    @Test void completedGroupCanShipAndCannotBeInsertedTwice() {
        Order o=order(1);o.setOrderType(1);o.setGroupBuyTeamId(3L);GroupBuyTeam team=new GroupBuyTeam();team.setStatus(1);
        when(orders.selectForUpdate(1L)).thenReturn(o);when(teams.selectById(3L)).thenReturn(team);when(logistics.selectCount(any())).thenReturn(0L);
        when(logistics.insert(any(Logistics.class))).thenReturn(1);when(orders.updateById(any(Order.class))).thenReturn(1);
        assertEquals("0",shipping.createLogistics(shipment(0)).getCode());assertEquals(2,o.getStatus());assertEquals(1,o.getLastStatus());
        assertThrows(BusinessException.class,()->shipping.createLogistics(shipment(0)));verify(logistics,times(1)).insert(any(Logistics.class));
    }
    @Test void existingShipmentIsRejectedBeforeAnotherInsert() {
        when(orders.selectForUpdate(1L)).thenReturn(order(1));when(logistics.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class,()->shipping.createLogistics(shipment(0)));verify(logistics,never()).insert(any(Logistics.class));
    }
    @ParameterizedTest @ValueSource(ints={0,3,4,5,6}) void disallowedOrdersCannotCreateLogistics(int state) {
        when(orders.selectForUpdate(1L)).thenReturn(order(state));
        assertThrows(BusinessException.class,()->shipping.createLogistics(shipment(0)));verifyNoInteractions(logistics);
    }
    @ParameterizedTest @ValueSource(ints={4,5,6}) void logisticsUpdateAndSignCannotOverwriteClosedOrRefundingOrder(int state) {
        Order o=order(state);lock(o,shipment(1));
        assertThrows(BusinessException.class,()->shipping.updateLogisticsStatus(2L,2));assertThrows(BusinessException.class,()->shipping.signLogistics(2L));
        assertEquals(state,o.getStatus());verify(logistics,never()).updateById(any(Logistics.class));verify(orders,never()).updateById(any(Order.class));
    }
    @ParameterizedTest @ValueSource(ints={0,3,-1,99}) void cannotRollBackShipmentOrCancelTransactionThroughLogistics(int target) {
        if (target >= 0 && target <= 3) lock(order(2),shipment(1));assertThrows(BusinessException.class,()->shipping.updateLogisticsStatus(2L,target));verify(logistics,never()).updateById(any(Logistics.class));
    }
    @Test void receiptUsesOrderThenLogisticsLockAndIsIdempotent() {
        Order o=order(2);Logistics l=shipment(1);lock(o,l);when(logistics.updateById(l)).thenReturn(1);when(orders.updateById(o)).thenReturn(1);
        shipping.signLogistics(2L);shipping.signLogistics(2L);assertEquals(3,o.getStatus());assertEquals(2,l.getStatus());
        verify(orders,times(1)).updateById(o);verify(logistics,times(1)).updateById(l);
        InOrder sequence=inOrder(orders,logistics);sequence.verify(logistics).selectById(2L);sequence.verify(orders).selectForUpdate(1L);sequence.verify(logistics).selectForUpdate(2L);sequence.verify(logistics).updateById(l);
    }
    @Test void failedOrderWriteDoesNotReportSuccessfulShipping() {
        when(orders.selectForUpdate(1L)).thenReturn(order(1));when(logistics.selectCount(any())).thenReturn(0L);when(logistics.insert(any(Logistics.class))).thenReturn(1);
        assertThrows(BusinessException.class,()->shipping.createLogistics(shipment(0)));
    }
    @Test void rejectedRefundRetainsAuthorizedFulfillment() {
        Order o=order(7);o.setLastStatus(2);Logistics l=shipment(1);lock(o,l);when(logistics.updateById(l)).thenReturn(1);when(orders.updateById(o)).thenReturn(1);
        assertEquals("0",shipping.signLogistics(2L).getCode());assertEquals(3,o.getStatus());
    }
    @Test void addressUpdateWritesOnlyDeliveryFieldsAndLeavesPaymentStateUntouched() {
        Order o=order(1);o.setTradeNo("paid-trade");when(orders.selectForUpdate(1L)).thenReturn(o);when(orders.updateDeliveryIfUnshipped(1L,"收件人","新地址","13800000000")).thenReturn(1);
        assertEquals("0",orderService.updateOrderAddress(" 收件人 ",1L," 新地址 ","13800000000").getCode());
        assertEquals(1,o.getStatus());assertEquals("paid-trade",o.getTradeNo());verify(orders,never()).updateById(any(Order.class));
        InOrder sequence=inOrder(orders);sequence.verify(orders).selectForUpdate(1L);sequence.verify(orders).updateDeliveryIfUnshipped(1L,"收件人","新地址","13800000000");
    }
    @ParameterizedTest @ValueSource(ints={2,3,4,5,6,7}) void forbiddenStatesCannotChangeAddress(int state) {
        when(orders.selectForUpdate(1L)).thenReturn(order(state));assertNotEquals("0",orderService.updateOrderAddress("买家",1L,"地址","电话").getCode());
        verify(orders,never()).updateDeliveryIfUnshipped(anyLong(),anyString(),anyString(),anyString());verify(orders,never()).updateById(any(Order.class));
    }
    @Test void lostConditionalAddressUpdateReturnsConflictWithoutEntityOverwrite() {
        when(orders.selectForUpdate(1L)).thenReturn(order(0));assertNotEquals("0",orderService.updateOrderAddress("买家",1L,"地址","电话").getCode());verify(orders,never()).updateById(any(Order.class));
    }
}
