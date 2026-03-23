package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.service.GroupBuyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger log = LoggerFactory.getLogger(GroupBuyController.class);
    
    @Autowired
    private GroupBuyService groupBuyService;
    
    // ========== 活动相关 ==========
    
    /**
     * 获取拼团活动列表（管理后台）
     * 返回所有活动（包括未开始、进行中、已结束、已下架）
     */
    @GetMapping("/activities")
    public Result<Page<GroupBuyActivity>> getActivities(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size,
            @RequestParam(required = false) Integer status) {
        try {
            Page<GroupBuyActivity> page = groupBuyService.getActivityList(currentPage, size, status);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取拼团活动列表失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 获取进行中的拼团活动列表（用户前台）
     * 只返回当前时间在开始时间和结束时间之间，且状态为"进行中"的活动
     */
    @GetMapping("/activities/active")
    public Result<Page<GroupBuyActivity>> getActiveActivities(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        try {
            Page<GroupBuyActivity> page = groupBuyService.getActiveActivityList(currentPage, size);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取进行中的拼团活动列表失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 获取活动详情
     */
    @GetMapping("/activity/{activityId}")
    public Result<GroupBuyActivity> getActivityDetail(@PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long activityId) {
        try {
            GroupBuyActivity activity = groupBuyService.getActivityDetail(activityId);
            return Result.success(activity);
        } catch (Exception e) {
            log.error("获取活动详情失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 创建拼团活动（商家）
     */
    @PostMapping("/activity")
    public Result<GroupBuyActivity> createActivity(@Valid @RequestBody GroupBuyActivity activity) {
        try {
            log.info("收到创建拼团活动请求");
            log.info("活动数据: {}", activity);
            log.info("活动名称: {}", activity.getActivityName());
            log.info("商品ID: {}", activity.getProductId());
            log.info("拼团价: {}", activity.getGroupPrice());
            log.info("成团人数: {}", activity.getRequiredMembers());
            log.info("有效期: {}", activity.getValidityHours());
            log.info("商家ID: {}", activity.getMerchantId());
            
            GroupBuyActivity created = groupBuyService.createActivity(activity);
            log.info("拼团活动创建成功，ID: {}", created.getId());
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建拼团活动失败", e);
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 更新拼团活动（商家）
     */
    @PutMapping("/activity")
    public Result<?> updateActivity(@Valid @RequestBody GroupBuyActivity activity) {
        try {
            boolean success = groupBuyService.updateActivity(activity);
            return success ? Result.success() : Result.error("-1", "更新失败");
        } catch (Exception e) {
            log.error("更新拼团活动失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 更新活动状态（商家）
     */
    @PutMapping("/activity/status/{id}")
    public Result<?> updateActivityStatus(
            @PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long id,
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        try {
            boolean success = groupBuyService.updateActivityStatus(id, status);
            return success ? Result.success("更新状态成功") : Result.error("-1", "更新失败");
        } catch (Exception e) {
            log.error("更新活动状态失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 删除拼团活动（商家）
     */
    @DeleteMapping("/activity/{id}")
    public Result<?> deleteActivity(@PathVariable @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long id) {
        try {
            groupBuyService.deleteActivity(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除拼团活动失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    // ========== 拼团操作 ==========
    
    /**
     * 发起拼团（开团）
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startTeam(@RequestBody Map<String, Object> params) {
        try {
            Long activityId = Long.parseLong(params.get("activityId").toString());
            Long userId = Long.parseLong(params.get("userId").toString());
            Long addressId = Long.parseLong(params.get("addressId").toString());
            Integer quantity = Integer.parseInt(params.getOrDefault("quantity", 1).toString());
            
            Map<String, Object> result = groupBuyService.startTeam(activityId, userId, addressId, quantity);
            return Result.success(result);
        } catch (Exception e) {
            log.error("发起拼团失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 参与拼团（参团）
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> joinTeam(@RequestBody Map<String, Object> params) {
        try {
            Long teamId = Long.parseLong(params.get("teamId").toString());
            Long userId = Long.parseLong(params.get("userId").toString());
            Long addressId = Long.parseLong(params.get("addressId").toString());
            Integer quantity = Integer.parseInt(params.getOrDefault("quantity", 1).toString());
            
            Map<String, Object> result = groupBuyService.joinTeam(teamId, userId, addressId, quantity);
            return Result.success(result);
        } catch (Exception e) {
            log.error("参与拼团失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 获取正在进行的团列表
     */
    @GetMapping("/teams")
    public Result<List<GroupBuyTeam>> getActiveTeams(@RequestParam @NotNull(message = "活动ID不能为空") @Min(value = 1, message = "活动ID必须大于0") Long activityId) {
        try {
            List<GroupBuyTeam> teams = groupBuyService.getActiveTeams(activityId);
            return Result.success(teams);
        } catch (Exception e) {
            log.error("获取拼团列表失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 获取团详情
     */
    @GetMapping("/team/{teamId}")
    public Result<GroupBuyTeam> getTeamDetail(@PathVariable @NotNull(message = "团ID不能为空") @Min(value = 1, message = "团ID必须大于0") Long teamId) {
        try {
            GroupBuyTeam team = groupBuyService.getTeamDetail(teamId);
            return Result.success(team);
        } catch (Exception e) {
            log.error("获取团详情失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
    
    /**
     * 获取我的拼团列表
     */
    @GetMapping("/my-teams")
    public Result<List<GroupBuyTeam>> getUserTeams(
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Integer status) {
        try {
            List<GroupBuyTeam> teams = groupBuyService.getUserTeams(userId, status);
            return Result.success(teams);
        } catch (Exception e) {
            log.error("获取我的拼团列表失败", e);
            return Result.error("-1", e.getMessage());
        }
    }
}

