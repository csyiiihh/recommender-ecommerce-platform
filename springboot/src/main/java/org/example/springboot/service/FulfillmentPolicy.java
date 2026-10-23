package org.example.springboot.service;

import org.example.springboot.entity.Order;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.mapper.GroupBuyTeamMapper;
import org.example.springboot.exception.BusinessException;
import org.springframework.stereotype.Component;

/** Shared guards for all order and logistics entry points. Call after locking the order. */
@Component
public class FulfillmentPolicy {
    private final GroupBuyTeamMapper teams;
    public FulfillmentPolicy(GroupBuyTeamMapper teams) { this.teams = teams; }
    public int fulfillmentStatus(Order order) {
        if (order == null || order.getStatus() == null) return -1;
        return order.getStatus() == 7 && order.getLastStatus() != null ? order.getLastStatus() : order.getStatus();
    }
    public void requireShipping(Order order) {
        if (fulfillmentStatus(order) != 1) throw new BusinessException("当前订单状态不允许发货");
        requireCompletedGroup(order);
    }
    public void requireReceipt(Order order) {
        if (fulfillmentStatus(order) != 2) throw new BusinessException("当前订单状态不允许签收");
        requireCompletedGroup(order);
    }
    private void requireCompletedGroup(Order order) {
        if (!Integer.valueOf(1).equals(order.getOrderType())) return;
        GroupBuyTeam team = order.getGroupBuyTeamId() == null ? null : teams.selectById(order.getGroupBuyTeamId());
        if (team == null || !Integer.valueOf(1).equals(team.getStatus())) throw new BusinessException("拼团尚未成功，不能发货或签收");
    }
}
