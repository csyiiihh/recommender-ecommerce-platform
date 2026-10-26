package org.example.springboot;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.entity.Order;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.service.impl.GroupBuyServiceImpl;
import org.example.springboot.task.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupTaskConsistencyTest {
    static final Instant CUTOFF=Instant.parse("2035-01-01T00:00:00Z");
    GroupBuyActivityMapper activities;
    GroupBuyTeamMapper teams;
    AutomaticRefundService refunds;
    GroupBuyServiceImpl service;
    @BeforeEach void setup() {
        activities=mock(GroupBuyActivityMapper.class); teams=mock(GroupBuyTeamMapper.class); refunds=mock(AutomaticRefundService.class);
        service=new GroupBuyServiceImpl(); ReflectionTestUtils.setField(service,"activityMapper",activities);
        ReflectionTestUtils.setField(service,"teamMapper",teams); ReflectionTestUtils.setField(service,"automaticRefunds",refunds);
    }
    GroupBuyActivity expired() {
        GroupBuyActivity a=new GroupBuyActivity(); a.setId(9L); a.setStatus(1); a.setEndTime(Timestamp.from(CUTOFF.minusSeconds(1))); a.setStock(77); a.setSalesCount(50); return a;
    }
    @Test void extensionAfterScanIsRecheckedUnderLock() {
        GroupBuyActivity latest=expired(); latest.setEndTime(Timestamp.from(CUTOFF.plusSeconds(600))); when(activities.selectForUpdate(9L)).thenReturn(latest);
        assertFalse(service.expireActivityIfDue(9L,CUTOFF)); verify(activities,never()).updateById(any(GroupBuyActivity.class)); verifyNoInteractions(teams,refunds);
    }
    @ParameterizedTest @ValueSource(ints={0,2,3})
    void alreadyTransitionedActivitiesAreSkipped(int status) {
        GroupBuyActivity latest=expired(); latest.setStatus(status); when(activities.selectForUpdate(9L)).thenReturn(latest);
        assertFalse(service.expireActivityIfDue(9L,CUTOFF)); verifyNoInteractions(teams,refunds);
    }
    @Test void missingActivityAndExactDeadlineAreNotWritten() {
        assertFalse(service.expireActivityIfDue(9L,CUTOFF));
        GroupBuyActivity exact=expired(); exact.setEndTime(Timestamp.from(CUTOFF)); when(activities.selectForUpdate(9L)).thenReturn(exact);
        assertFalse(service.expireActivityIfDue(9L,CUTOFF)); verify(activities,never()).updateById(any(GroupBuyActivity.class));
    }
    void closeFixtures() {
        when(activities.selectForUpdate(9L)).thenReturn(expired());
        GroupBuyTeam t=new GroupBuyTeam(); t.setId(5L); t.setStatus(0); when(teams.selectList(any())).thenReturn(List.of(t)); when(teams.selectForUpdate(5L)).thenReturn(t);
        when(teams.updateById(any(GroupBuyTeam.class))).thenReturn(1); when(activities.updateById(any(GroupBuyActivity.class))).thenReturn(1);
    }
    void assertPatch(int status) {
        ArgumentCaptor<GroupBuyActivity> patch=ArgumentCaptor.forClass(GroupBuyActivity.class); verify(activities).updateById(patch.capture());
        assertEquals(9L,patch.getValue().getId()); assertEquals(status,patch.getValue().getStatus()); assertNotNull(patch.getValue().getUpdatedAt());
        assertNull(patch.getValue().getStock()); assertNull(patch.getValue().getSalesCount()); assertNull(patch.getValue().getEndTime());
    }
    @Test void expiryUsesPartialWriteAndQueuesRefundsForOngoingTeams() {
        closeFixtures(); assertTrue(service.expireActivityIfDue(9L,CUTOFF)); assertPatch(2); verify(refunds).queueTeam(5L);
    }
    @ParameterizedTest @ValueSource(ints={2,3})
    void manualClosingUsesTheSameHistorySafeTransition(int status) {
        closeFixtures(); assertTrue(service.updateActivityStatus(9L,status)); assertPatch(status); verify(refunds).queueTeam(5L);
    }
    @Test void failedActivityWriteCannotReportSuccessAfterQueuingRefunds() {
        closeFixtures(); when(activities.updateById(any(GroupBuyActivity.class))).thenReturn(0);
        assertThrows(BusinessException.class,()->service.expireActivityIfDue(9L,CUTOFF));
    }
    @Test void failedTeamWriteDoesNotQueueRefunds() {
        GroupBuyTeam t=new GroupBuyTeam(); t.setId(5L); t.setStatus(0); when(teams.selectForUpdate(5L)).thenReturn(t);
        assertThrows(BusinessException.class,()->service.failTeam(5L,"timeout")); verifyNoInteractions(refunds);
    }
    @Test void activityScanIsBoundedAndOneFailureDoesNotSkipTheNextActivity() {
        GroupBuyService worker=mock(GroupBuyService.class); GroupBuyTask task=new GroupBuyTask(); ReflectionTestUtils.setField(task,"activityMapper",activities); ReflectionTestUtils.setField(task,"groupBuyService",worker);
        GroupBuyActivity a=expired(),b=expired(); b.setId(10L); when(activities.selectList(any())).thenReturn(List.of(a,b));
        when(worker.expireActivityIfDue(eq(9L),any())).thenThrow(new BusinessException("fixture write failure"));
        assertDoesNotThrow(task::checkExpiredActivities); verify(worker).expireActivityIfDue(eq(10L),any()); verify(activities,never()).updateById(any(GroupBuyActivity.class));
        ArgumentCaptor<Wrapper> q=ArgumentCaptor.forClass(Wrapper.class); verify(activities).selectList(q.capture()); assertTrue(q.getValue().getSqlSegment().contains("LIMIT 100")); assertTrue(q.getValue().getSqlSegment().contains("ORDER BY"));
    }
    @Test void failedTeamRetryDoesNotSkipOtherRetriesOrExpiredTeams() {
        GroupBuyService worker=mock(GroupBuyService.class); GroupBuyTask task=new GroupBuyTask(); ReflectionTestUtils.setField(task,"teamMapper",teams); ReflectionTestUtils.setField(task,"groupBuyService",worker);
        GroupBuyTeam expired=new GroupBuyTeam(); expired.setId(3L); GroupBuyTeam first=new GroupBuyTeam(); first.setId(1L); GroupBuyTeam next=new GroupBuyTeam(); next.setId(2L);
        when(teams.selectList(any())).thenReturn(List.of(expired),List.of(first,next)); doThrow(new BusinessException("fixture failure")).when(worker).failTeam(1L,null);
        assertDoesNotThrow(task::checkExpiredTeams); verify(worker).failTeam(2L,null); verify(worker).failTeam(3L);
        ArgumentCaptor<Wrapper> queries=ArgumentCaptor.forClass(Wrapper.class); verify(teams,times(2)).selectList(queries.capture()); for(Wrapper q:queries.getAllValues()) assertTrue(q.getSqlSegment().contains("LIMIT 100"));
    }
    @Test void failedRefundBackoffWriteDoesNotAbortLaterOrders() {
        OrderMapper orders=mock(OrderMapper.class); OrderService worker=mock(OrderService.class);
        Order first=new Order(); first.setId(1L); Order next=new Order(); next.setId(2L); when(orders.selectList(any())).thenReturn(List.of(first,next));
        when(worker.handleRefund(1L,6,"系统自动退款")).thenThrow(new BusinessException("fixture channel failure")); when(orders.update(isNull(),any(Wrapper.class))).thenThrow(new IllegalStateException("fixture backoff write failure"));
        assertDoesNotThrow(()->new AutomaticRefundTask(orders,worker).retryPendingRefunds()); verify(worker).handleRefund(2L,6,"系统自动退款");
    }
    ApplicationContextRunner taskContext() {
        return new ApplicationContextRunner().withUserConfiguration(GroupBuyTask.class,AutomaticRefundTask.class)
            .withBean(GroupBuyTeamMapper.class,()->teams).withBean(GroupBuyActivityMapper.class,()->activities)
            .withBean(GroupBuyService.class,()->mock(GroupBuyService.class))
            .withBean(OrderMapper.class,()->mock(OrderMapper.class))
            .withInitializer(ctx->ctx.getBeanFactory().registerSingleton("fixtureOrderService",mock(OrderService.class)));
    }
    @Test void tasksStayEnabledByDefault() {
        taskContext().run(ctx->{org.assertj.core.api.Assertions.assertThat(ctx).hasSingleBean(GroupBuyTask.class).hasSingleBean(AutomaticRefundTask.class);});
    }
    @Test void integrationCanDisableBothMutatingTasks() {
        taskContext().withPropertyValues("group-buy.tasks.enabled=false","refund.retry.enabled=false")
            .run(ctx->{org.assertj.core.api.Assertions.assertThat(ctx).doesNotHaveBean(GroupBuyTask.class).doesNotHaveBean(AutomaticRefundTask.class);});
    }
}
