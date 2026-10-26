package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.example.springboot.service.GroupBuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 拼团控制器
 */
@RestController
@RequestMapping("/group-buy")
@Validated
public class GroupBuyController {
    @Autowired private org.example.springboot.service.AccessService access;
    

    
    @Autowired
    private GroupBuyService groupBuyService;
    
    // ========== 活动相关 ==========
    
    /**
     * 获取拼团活动列表（管理后台）
     * 返回所有活动（包括未开始、进行中、已结束、已下架）
     */
    @GetMapping("/activities")
    public Result<Page<GroupBuyActivity>> getActivities(
            @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(1) @jakarta.validation.constraints.Max(100) Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String activityName) {
        access.staff();
        Long scopedMerchant = access.isAdmin() ? merchantId : access.current().getId();
        return Result.success(groupBuyService.getActivityList(currentPage, size, status, scopedMerchant, activityName));
    }
    
    /**
     * 获取进行中的拼团活动列表（用户前台）
     * 只返回当前时间在开始时间和结束时间之间，且状态为"进行中"的活动
     */
    @GetMapping("/activities/active")
    public Result<Page<GroupBuyActivity>> getActiveActivities(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        Page<GroupBuyActivity> page = groupBuyService.getActiveActivityList(currentPage, size);
        return Result.success(page);
    }
    
    /**
     * 获取活动详情
     */
    @GetMapping("/activity/{activityId}")
    public Result<GroupBuyActivity> getActivityDetail(@PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long activityId) {
        GroupBuyActivity activity = groupBuyService.getActivityDetail(activityId);
        return Result.success(activity);
    }
    
    /**
     * 创建拼团活动（商家）
     */
    @PostMapping("/activity")
    public Result<GroupBuyActivity> createActivity(@Valid @RequestBody GroupActivityRequest input) {
        access.product(input.productId());
        GroupBuyActivity activity = input.toEntity(); activity.setId(null);
        return Result.success(groupBuyService.createActivity(activity));
    }
    
    /**
     * 更新拼团活动（商家）
     */
    @PutMapping("/activity")
    public Result<?> updateActivity(@Valid @RequestBody GroupActivityRequest input) {
        if (input.id() == null || input.id() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动ID不能为空");
        access.activity(input.id()); access.product(input.productId());
        return groupBuyService.updateActivity(input.toEntity()) ? Result.success() : Result.error("-1", "更新失败");
    }
    
    /**
     * 更新活动状态（商家）
     */
    @PutMapping("/activity/status/{id}")
    public Result<?> updateActivityStatus(
            @PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long id,
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        access.activity(id);
        boolean success = groupBuyService.updateActivityStatus(id, status);
        return success ? Result.success("更新状态成功") : Result.error("-1", "更新失败");
    }
    
    /**
     * 删除拼团活动（商家）
     */
    @DeleteMapping("/activity/{id}")
    public Result<?> deleteActivity(@PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long id) {
        access.activity(id);
        groupBuyService.deleteActivity(id);
        return Result.success("删除成功");
    }
    
    // ========== 拼团操作 ==========
    
    /**
     * 发起拼团（开团）
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startTeam(@Valid @RequestBody GroupOrderRequest input,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey) {
        if (input.activityId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少活动或团队ID");
        access.owner(input.userId()); access.address(input.addressId());
        return Result.success(groupBuyService.startTeam(input.activityId(), input.userId(), input.addressId(), input.quantity(), requestKey));
    }
    
    /**
     * 参与拼团（参团）
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> joinTeam(@Valid @RequestBody GroupOrderRequest input,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey) {
        if (input.teamId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少活动或团队ID");
        access.owner(input.userId()); access.address(input.addressId());
        return Result.success(groupBuyService.joinTeam(input.teamId(), input.userId(), input.addressId(), input.quantity(), requestKey));
    }
    
    /**
     * 获取正在进行的团列表
     */
    @GetMapping("/teams")
    public Result<List<PublicGroupTeam>> getActiveTeams(@RequestParam @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long activityId) {
        List<GroupBuyTeam> teams = groupBuyService.getActiveTeams(activityId);
        return Result.success(teams.stream().map(PublicGroupTeam::from).toList());
    }
    
    /**
     * 获取团详情
     */
    @GetMapping("/team/{teamId}")
    public Result<PublicGroupTeam> getTeamDetail(@PathVariable @NotNull(message = "团ID不能为空") @Min(value = 1, message = "团ID必须大于0") Long teamId) {
        access.current();
        GroupBuyTeam team = groupBuyService.getTeamDetail(teamId);
        return Result.success(PublicGroupTeam.from(team));
    }
    
    /**
     * 获取我的拼团列表
     */
    @GetMapping("/my-teams")
    public Result<List<PublicGroupTeam>> getUserTeams(
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Integer status) {
        access.owner(userId);
        List<GroupBuyTeam> teams = groupBuyService.getUserTeams(userId, status);
        return Result.success(teams.stream().map(PublicGroupTeam::from).toList());
    }
}

