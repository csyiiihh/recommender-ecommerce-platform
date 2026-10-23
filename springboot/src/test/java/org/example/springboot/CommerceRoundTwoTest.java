package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.service.impl.GroupBuyServiceImpl;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommerceRoundTwoTest {
    @Mock OrderMapper orderMapper;
    @Mock ProductMapper productMapper;
    @Mock StockInMapper stockInMapper;
    @Mock StockOutMapper stockOutMapper;
    @Mock AccessService access;
    @Mock AlipayService alipay;
    @Mock LogisticsMapper logisticsMapper;
    @Mock GroupBuyMemberMapper groupBuyMemberMapper;
    @Mock GroupBuyTeamMapper groupBuyTeamMapper;
    @Mock GroupBuyActivityMapper groupBuyActivityMapper;
    @Mock AutomaticRefundService automaticRefunds;
    @InjectMocks OrderService service;
    @InjectMocks StockService stocks;
    @InjectMocks GroupBuyServiceImpl groups;

    @BeforeAll static void initializeMybatisMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), Order.class);
    }
    Order pending(String reason) {
        Order o=new Order();o.setId(1L);o.setProductId(2L);o.setQuantity(2);o.setStatus(5);o.setLastStatus(1);o.setPayMethod("alipay");o.setTradeNo("paid");o.setOutTradeNo("checkout");o.setTotalPrice(new BigDecimal("50.00"));o.setRefundReason(reason);o.setRefundStatus(reason.startsWith(AutomaticRefundService.TEAM_FAILED) || reason.equals(AutomaticRefundService.UNFULFILLABLE) ? AutomaticRefundService.AUTO_PENDING : 1);return o;
    }
    @Test void compensationRefundNeverRestoresUndeductedInventoryAndCannotRunTwice() throws Exception {
        Order o=pending(AutomaticRefundService.UNFULFILLABLE);
        when(orderMapper.selectForUpdate(1L)).thenReturn(o);when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        assertEquals("0",service.handleRefund(1L,6,"自动退款").getCode());
        assertNotEquals("0",service.handleRefund(1L,6,"重复执行").getCode());
        assertEquals(6,o.getStatus());verify(alipay,times(1)).refund("checkout","50.00","REFUND_ORDER_1");verifyNoInteractions(productMapper);
    }
    @Test void remoteRefundFailurePreservesRetryIntent() throws Exception {
        Order o=pending(AutomaticRefundService.TEAM_FAILED);when(orderMapper.selectForUpdate(1L)).thenReturn(o);
        doThrow(new com.alipay.api.AlipayApiException("timeout")).when(alipay).refund(anyString(),anyString(),anyString());
        assertThrows(BusinessException.class,()->service.handleRefund(1L,6,"重试"));
        assertEquals(5,o.getStatus());assertEquals(AutomaticRefundService.TEAM_FAILED,o.getRefundReason());verify(orderMapper,never()).updateById(any(Order.class));verifyNoInteractions(productMapper);
    }
    @Test void userSuppliedRefundReasonCannotBecomeAnAutomaticRefund() {
        Order o=pending(AutomaticRefundService.UNFULFILLABLE);o.setRefundStatus(1);
        assertFalse(AutomaticRefundService.isAutomatic(o));
    }
    @Test void automaticCompensationCannotBeRejectedByOperator() {
        Order o=pending(AutomaticRefundService.UNFULFILLABLE);when(orderMapper.selectForUpdate(1L)).thenReturn(o);
        assertThrows(BusinessException.class,()->service.handleRefund(1L,7,"拒绝"));verifyNoInteractions(alipay);
    }
    @Test void completedGroupSurvivesAnIndividualRefund() {
        Order o=pending("售后");o.setOrderType(1);o.setActivityId(3L);
        Product p=new Product();p.setId(2L);p.setStock(4);p.setSalesCount(10);
        GroupBuyMember m=new GroupBuyMember();m.setId(4L);m.setTeamId(5L);m.setStatus(1);m.setIsLeader(1);
        GroupBuyTeam t=new GroupBuyTeam();t.setId(5L);t.setStatus(1);t.setCurrentPeople(3);
        GroupBuyActivity a=new GroupBuyActivity();a.setStock(4);a.setSalesCount(10);
        when(orderMapper.selectForUpdate(1L)).thenReturn(o);when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        when(productMapper.selectForUpdate(2L)).thenReturn(p);when(groupBuyMemberMapper.selectOne(any())).thenReturn(m);
        when(groupBuyTeamMapper.selectForUpdate(5L)).thenReturn(t);when(groupBuyActivityMapper.selectForUpdate(3L)).thenReturn(a);
        assertEquals("0",service.handleRefund(1L,6,"售后退款").getCode());
        assertEquals(1,t.getStatus());assertEquals(6,p.getStock());assertEquals(8,p.getSalesCount());assertEquals(8,a.getSalesCount());verifyNoInteractions(automaticRefunds);
    }
    @Test void activeLeaderRefundQueuesOtherMembers() {
        Order o=pending("取消参团");o.setOrderType(1);o.setActivityId(3L);
        Product p=new Product();p.setId(2L);p.setStock(4);p.setSalesCount(0);
        GroupBuyMember m=new GroupBuyMember();m.setTeamId(5L);m.setIsLeader(1);
        GroupBuyTeam t=new GroupBuyTeam();t.setId(5L);t.setStatus(0);t.setCurrentPeople(2);
        GroupBuyActivity a=new GroupBuyActivity();a.setStock(4);a.setSalesCount(0);
        when(orderMapper.selectForUpdate(1L)).thenReturn(o);when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        when(productMapper.selectForUpdate(2L)).thenReturn(p);when(groupBuyMemberMapper.selectOne(any())).thenReturn(m);
        when(groupBuyTeamMapper.selectForUpdate(5L)).thenReturn(t);when(groupBuyActivityMapper.selectForUpdate(3L)).thenReturn(a);
        service.handleRefund(1L,6,"退款");assertEquals(2,t.getStatus());verify(automaticRefunds).queueTeam(5L);
    }
    @Test void shippedRefundDoesNotCreateStockBeforeGoodsAreReturned() {
        Order o=pending("售后退款");o.setLastStatus(2);o.setOrderType(0);
        Product p=new Product();p.setId(2L);p.setStock(4);p.setSalesCount(10);
        when(orderMapper.selectForUpdate(1L)).thenReturn(o);when(orderMapper.updateById(any(Order.class))).thenReturn(1);when(productMapper.selectForUpdate(2L)).thenReturn(p);
        assertEquals("0",service.handleRefund(1L,6,"退款批准，实物需单独验收入库").getCode());
        assertEquals(4,p.getStock());assertEquals(8,p.getSalesCount());
    }
    @Test void stockReversalCannotApplyTwice() {
        StockIn in=new StockIn();in.setId(1L);in.setProductId(2L);in.setQuantity(2);in.setStatus(0);
        when(stockInMapper.selectForUpdate(1L)).thenReturn(in);
        assertNotEquals("0",stocks.invalidateStockIn(1L).getCode());verifyNoInteractions(productMapper);
    }
    @Test void stockInUsesAuthenticatedOperatorAndLocksBeforeWriting() {
        Product p=new Product();p.setStock(4);when(productMapper.selectForUpdate(2L)).thenReturn(p);
        User user=new User();user.setId(7L);when(access.current()).thenReturn(user);
        StockIn in=new StockIn();in.setId(99L);in.setProductId(2L);in.setOperatorId(999L);in.setQuantity(2);in.setStatus(0);in.setUnitPrice(BigDecimal.TEN);
        when(stockInMapper.insert(any(StockIn.class))).thenReturn(1);
        assertEquals("0",stocks.createStockIn(in).getCode());assertEquals(7L,in.getOperatorId());assertEquals(1,in.getStatus());assertNull(in.getId());assertEquals(6,p.getStock());
        InOrder sequence=inOrder(productMapper,stockInMapper);sequence.verify(productMapper).selectForUpdate(2L);sequence.verify(productMapper).updateById(p);sequence.verify(stockInMapper).insert(in);
    }
    @Test void failedTeamQueuesRefundsWithoutCallingPaymentNetwork() {
        GroupBuyTeam t=new GroupBuyTeam();t.setId(5L);t.setStatus(0);
        when(groupBuyTeamMapper.selectForUpdate(5L)).thenReturn(t);
        groups.failTeam(5L,"timeout");assertEquals(2,t.getStatus());verify(automaticRefunds).queueTeam(5L);verifyNoInteractions(alipay);
    }
    @Test void retrySchedulerContinuesAfterOneFailedRefund() {
        Order one=pending(AutomaticRefundService.TEAM_FAILED),two=pending(AutomaticRefundService.UNFULFILLABLE);two.setId(2L);
        OrderService worker=mock(OrderService.class);when(orderMapper.selectList(any())).thenReturn(List.of(one,two));
        doThrow(new BusinessException("temporary error")).when(worker).handleRefund(1L,6,"系统自动退款");
        new org.example.springboot.task.AutomaticRefundTask(orderMapper,worker).retryPendingRefunds();
        verify(worker).handleRefund(2L,6,"系统自动退款");verify(orderMapper).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
