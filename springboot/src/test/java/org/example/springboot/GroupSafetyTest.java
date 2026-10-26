package org.example.springboot;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.dto.PublicGroupTeam;
import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;
import org.example.springboot.service.CheckoutIdempotencyService;
import org.example.springboot.service.impl.GroupBuyServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupSafetyTest {
    GroupBuyServiceImpl service;
    GroupBuyActivityMapper activities;
    GroupBuyTeamMapper teams;
    GroupBuyMemberMapper members;
    OrderMapper orders;
    ProductMapper products;
    UserMapper users;
    AddressMapper addresses;
    CheckoutIdempotencyService requests;

    @BeforeEach void setup() {
        service = new GroupBuyServiceImpl();
        activities = mock(GroupBuyActivityMapper.class); teams = mock(GroupBuyTeamMapper.class);
        members = mock(GroupBuyMemberMapper.class); orders = mock(OrderMapper.class);
        products = mock(ProductMapper.class); users = mock(UserMapper.class); addresses = mock(AddressMapper.class);
        requests = mock(CheckoutIdempotencyService.class);
        ReflectionTestUtils.setField(service, "activityMapper", activities); ReflectionTestUtils.setField(service, "teamMapper", teams);
        ReflectionTestUtils.setField(service, "memberMapper", members); ReflectionTestUtils.setField(service, "orderMapper", orders);
        ReflectionTestUtils.setField(service, "productMapper", products); ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "addressMapper", addresses); ReflectionTestUtils.setField(service, "checkoutRequests", requests);
    }
    GroupBuyActivity activity() {
        GroupBuyActivity a = new GroupBuyActivity(); a.setId(2L); a.setProductId(9L); a.setMerchantId(7L);
        a.setActivityName("Test activity"); a.setGroupPrice(new BigDecimal("20.00")); a.setRequiredMembers(3);
        a.setValidityHours(24); a.setStatus(1); a.setStartTime(new Timestamp(System.currentTimeMillis()-3600000));
        a.setEndTime(new Timestamp(System.currentTimeMillis()+86400000)); return a;
    }
    Product product() {
        Product p = new Product(); p.setId(9L); p.setMerchantId(7L); p.setPrice(new BigDecimal("30.00")); p.setStock(100); p.setStatus(1); return p;
    }
    void orderFixtures() {
        GroupBuyActivity a = activity(); when(activities.selectById(2L)).thenReturn(a); when(activities.selectForUpdate(2L)).thenReturn(a);
        when(products.selectForUpdate(9L)).thenReturn(product());
        User u = new User(); u.setId(42L); u.setStatus(1); when(users.selectForUpdate(42L)).thenReturn(u);
        Address address = new Address(); address.setId(8L); address.setUserId(42L); address.setReceiver("Fixture"); address.setPhone("123"); address.setAddress("Test address"); when(addresses.selectById(8L)).thenReturn(address);
        when(orders.insert(any(Order.class))).thenAnswer(inv -> { ((Order)inv.getArgument(0)).setId(88L); return 1; });
    }
    Order pending() {
        Order o = new Order(); o.setId(80L); o.setActivityId(2L); o.setUserId(42L); o.setOrderType(1); o.setStatus(0); o.setQuantity(1);
        o.setRecvName("Fixture"); o.setRecvPhone("123"); o.setRecvAddress("Test address"); return o;
    }
    @Test void publicTeamIsAnExplicitAllowlist() throws Exception {
        User u = new User(); u.setId(42L); u.setName("Reader"); u.setUsername("private-login"); u.setEmail("fixture@example.invalid"); u.setBusinessLicense("license-secret"); u.setPassword("password-secret"); u.setToken("token-secret");
        GroupBuyTeam t = new GroupBuyTeam(); t.setId(2L); t.setLeader(u); t.setLeaderOrderId(123456L);
        GroupBuyMember m = new GroupBuyMember(); m.setUser(u); m.setOrderId(123456L); t.setMembers(List.of(m));
        String json = new ObjectMapper().writeValueAsString(PublicGroupTeam.from(t));
        for (String forbidden : List.of("email", "businessLicense", "password", "token", "orderId", "leaderOrderId", "private-login")) assertFalse(json.contains(forbidden), json);
        assertTrue(json.contains("Reader"));
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void anyTeamOrOrderHistoryPreventsPhysicalDeletion(boolean teamHistory) {
        when(activities.selectForUpdate(2L)).thenReturn(activity());
        if (teamHistory) when(teams.selectCount(any())).thenReturn(1L); else when(orders.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.deleteActivity(2L));
        verify(activities, never()).deleteById(anyLong()); verify(teams, never()).delete(any(Wrapper.class)); verify(members, never()).delete(any(Wrapper.class));
    }
    @Test void emptyActivityCanBeDeletedWithoutTouchingOtherRecords() {
        when(activities.selectForUpdate(2L)).thenReturn(activity()); when(activities.deleteById(2L)).thenReturn(1);
        service.deleteActivity(2L); verify(activities).deleteById(2L); verifyNoInteractions(members);
    }
    @ParameterizedTest @ValueSource(ints = {0,1,2,3,4})
    void updateUsesCreationRulesForInvalidTradingValues(int invalid) {
        GroupBuyActivity input = activity(); when(activities.selectForUpdate(2L)).thenReturn(activity()); when(products.selectForUpdate(9L)).thenReturn(product());
        switch(invalid) {
            case 0 -> input.setGroupPrice(BigDecimal.ZERO);
            case 1 -> input.setGroupPrice(new BigDecimal("30.00"));
            case 2 -> input.setRequiredMembers(1);
            case 3 -> input.setValidityHours(73);
            case 4 -> input.setEndTime(input.getStartTime());
        }
        assertThrows(BusinessException.class, () -> service.updateActivity(input)); verify(activities, never()).updateById(any(GroupBuyActivity.class));
    }
    @Test void updateIgnoresClientCountersAndOwnership() {
        GroupBuyActivity input = activity(); input.setSalesCount(9999); input.setStock(9999); input.setStatus(3); input.setMerchantId(999L);
        when(activities.selectForUpdate(2L)).thenReturn(activity()); when(products.selectForUpdate(9L)).thenReturn(product()); when(activities.updateById(any(GroupBuyActivity.class))).thenReturn(1);
        assertTrue(service.updateActivity(input)); ArgumentCaptor<GroupBuyActivity> captor = ArgumentCaptor.forClass(GroupBuyActivity.class); verify(activities).updateById(captor.capture());
        GroupBuyActivity patch = captor.getValue(); assertNull(patch.getSalesCount()); assertNull(patch.getStock()); assertNull(patch.getStatus()); assertEquals(7L, patch.getMerchantId());
    }
    @Test void historyLocksTradingRulesButAllowsRenaming() {
        GroupBuyActivity old = activity(); GroupBuyActivity input = activity(); input.setStartTime(old.getStartTime()); input.setEndTime(old.getEndTime());
        when(activities.selectForUpdate(2L)).thenReturn(old); when(products.selectForUpdate(9L)).thenReturn(product()); when(orders.selectCount(any())).thenReturn(1L);
        input.setGroupPrice(new BigDecimal("19.00")); assertThrows(BusinessException.class, () -> service.updateActivity(input));
        input.setGroupPrice(old.getGroupPrice()); input.setActivityName("New name"); when(activities.updateById(any(GroupBuyActivity.class))).thenReturn(1); assertTrue(service.updateActivity(input));
    }
    @Test void creationDerivesOwnershipPriceStockAndCounters() {
        GroupBuyActivity input = activity(); input.setMerchantId(999L); input.setStock(9999); input.setSalesCount(9999); input.setOriginalPrice(BigDecimal.ONE); input.setStatus(3);
        when(products.selectForUpdate(9L)).thenReturn(product()); when(activities.insert(any(GroupBuyActivity.class))).thenReturn(1);
        GroupBuyActivity saved = service.createActivity(input); assertEquals(7L,saved.getMerchantId()); assertEquals(100,saved.getStock()); assertEquals(0,saved.getSalesCount()); assertEquals(new BigDecimal("30.00"),saved.getOriginalPrice()); assertEquals(1,saved.getStatus());
    }
    @Test void scopedActivityPageBatchesProductsAndCapsPageSize() {
        GroupBuyActivity a = activity(), b = activity(); b.setId(3L); Page<GroupBuyActivity> page = new Page<>(); page.setRecords(List.of(a,b));
        when(activities.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page); when(products.selectBatchIds(Set.of(9L))).thenReturn(List.of(product()));
        service.getActivityList(1,500,1,7L,"book");
        ArgumentCaptor<Page> paging = ArgumentCaptor.forClass(Page.class); ArgumentCaptor<Wrapper> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(activities).selectPage(paging.capture(),query.capture()); assertEquals(100,paging.getValue().getSize()); assertTrue(query.getValue().getSqlSegment().contains("merchant_id")); assertTrue(query.getValue().getSqlSegment().contains("activity_name"));
        verify(products,times(1)).selectBatchIds(any()); verify(products,never()).selectById(anyLong()); assertSame(a.getProduct(),b.getProduct());
    }
    @Test void teamPageBatchesMembersAndDistinctPeople() {
        GroupBuyTeam a = new GroupBuyTeam(); a.setId(2L); a.setLeaderUserId(42L); GroupBuyMember m = new GroupBuyMember(); m.setTeamId(2L); m.setUserId(42L);
        User u = new User(); u.setId(42L); when(teams.selectList(any())).thenReturn(List.of(a)); when(members.selectList(any())).thenReturn(List.of(m)); when(users.selectBatchIds(Set.of(42L))).thenReturn(List.of(u));
        service.getActiveTeams(2L); assertSame(u,a.getLeader()); assertSame(u,a.getMembers().get(0).getUser()); verify(users,times(1)).selectBatchIds(any()); verify(users,never()).selectById(anyLong());
    }
    @Test void historicalActivityCannotBypassOfflineTransitionViaDraft() {
        when(activities.selectForUpdate(2L)).thenReturn(activity()); when(orders.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.updateActivityStatus(2L,0)); verify(activities,never()).updateById(any(GroupBuyActivity.class));
    }
    @Test void startScopesPendingLookupToActualActivityAndLeaderRole() {
        orderFixtures(); Map<String,Object> result = service.startTeam(2L,42L,8L,3);
        assertEquals(2L,result.get("activityId")); assertEquals(88L,result.get("orderId"));
        ArgumentCaptor<Wrapper> q = ArgumentCaptor.forClass(Wrapper.class); verify(orders).selectOne(q.capture()); String sql=q.getValue().getSqlSegment(); assertTrue(sql.contains("activity_id")); assertTrue(sql.contains("group_buy_team_id IS NULL"));
        ArgumentCaptor<Order> saved=ArgumentCaptor.forClass(Order.class); verify(orders).insert(saved.capture()); assertEquals(3,saved.getValue().getQuantity()); assertEquals(new BigDecimal("60.00"),saved.getValue().getTotalPrice());
        var sequence=inOrder(users,products,activities); sequence.verify(users).selectForUpdate(42L); sequence.verify(activities).selectById(2L); sequence.verify(products).selectForUpdate(9L); sequence.verify(activities).selectForUpdate(2L);
    }
    @Test void exactLegacyPendingReplayPreservesOrder() {
        orderFixtures(); when(orders.selectOne(any(Wrapper.class))).thenReturn(pending());
        assertEquals(80L,service.startTeam(2L,42L,8L,1).get("orderId")); verify(orders,never()).insert(any(Order.class));
    }
    @Test void changedLegacyPendingIntentReturnsConflict() {
        orderFixtures(); when(orders.selectOne(any(Wrapper.class))).thenReturn(pending());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.startTeam(2L,42L,8L,3)).getStatusCode().value()); verify(orders,never()).insert(any(Order.class));
    }
    @Test void keyedReplayDoesNotReadLiveActivityOrCreateOrder() {
        Order order = pending(); order.setStatus(1);
        when(requests.prepare(eq(42L),eq("group-start"),eq("a".repeat(32)),anyList())).thenReturn(new CheckoutIdempotencyService.Token("trade",List.of(order)));
        Map<String,Object> result=service.startTeam(2L,42L,8L,1,"a".repeat(32)); assertEquals(80L,result.get("orderId")); assertEquals(false,result.get("paymentAllowed")); verifyNoInteractions(activities,teams,members,orders,products,users,addresses);
    }
    @Test void keyConflictPropagatesBeforeBusinessReads() {
        when(requests.prepare(anyLong(),anyString(),anyString(),anyList())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.startTeam(2L,42L,8L,1,"a".repeat(32))).getStatusCode().value()); verifyNoInteractions(activities,orders,products);
    }
    @Test void anotherBuyersAddressCannotBeUsed() {
        orderFixtures(); Address address=new Address(); address.setUserId(99L); when(addresses.selectById(8L)).thenReturn(address);
        assertThrows(BusinessException.class,()->service.startTeam(2L,42L,8L,1)); verify(orders,never()).insert(any(Order.class));
    }
    @ParameterizedTest @ValueSource(ints={0,-1,1001})
    void invalidQuantityNeverAcquiresLocks(int quantity) {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.startTeam(2L,42L,8L,quantity)).getStatusCode().value()); verifyNoInteractions(users,activities,orders,products);
    }
    @Test void joinScopesReplayToTheSelectedTeam() {
        orderFixtures(); GroupBuyTeam t=new GroupBuyTeam(); t.setId(5L); t.setActivityId(2L); t.setStatus(0); t.setCurrentPeople(1); t.setRequiredPeople(3); t.setExpireTime(new Timestamp(System.currentTimeMillis()+3600000));
        when(teams.selectById(5L)).thenReturn(t); when(teams.selectForUpdate(5L)).thenReturn(t);
        service.joinTeam(5L,42L,8L,1); ArgumentCaptor<Wrapper> q=ArgumentCaptor.forClass(Wrapper.class); verify(orders).selectOne(q.capture()); assertTrue(q.getValue().getSqlSegment().contains("group_buy_team_id ="));
        ArgumentCaptor<Order> saved=ArgumentCaptor.forClass(Order.class); verify(orders).insert(saved.capture()); assertEquals(5L,saved.getValue().getGroupBuyTeamId());
    }
    @Test void unpublishedProductCannotCreateAnOrder() {
        orderFixtures(); Product p=product(); p.setStatus(0); when(products.selectForUpdate(9L)).thenReturn(p);
        assertThrows(BusinessException.class,()->service.startTeam(2L,42L,8L,1)); verify(orders,never()).insert(any(Order.class));
    }
}
