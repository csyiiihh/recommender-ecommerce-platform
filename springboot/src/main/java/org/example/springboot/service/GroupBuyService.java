package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.entity.GroupBuyTeam;

import java.util.List;
import java.util.Map;

/**
 * 拼团业务接口
 */
public interface GroupBuyService {
    
    // ========== 活动管理 ==========
    
    /**
     * 创建拼团活动
     * @param activity 活动信息
     * @return 创建的活动
     */
    GroupBuyActivity createActivity(GroupBuyActivity activity);
    
    /**
     * 获取活动列表（分页）- 管理后台使用
     * @param currentPage 当前页
     * @param size 每页大小
     * @param status 状态筛选（可选）
     * @return 分页结果
     */
    Page<GroupBuyActivity> getActivityList(Integer currentPage, Integer size, Integer status);
    
    /**
     * 获取进行中的活动列表（分页）- 用户前台使用
     * 只返回当前时间在开始时间和结束时间之间，且状态为"进行中"的活动
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    Page<GroupBuyActivity> getActiveActivityList(Integer currentPage, Integer size);
    
    /**
     * 获取活动详情
     * @param activityId 活动ID
     * @return 活动详情（包含商品信息）
     */
    GroupBuyActivity getActivityDetail(Long activityId);
    
    /**
     * 更新活动状态
     * @param activityId 活动ID
     * @param status 新状态
     * @return 是否成功
     */
    boolean updateActivityStatus(Long activityId, Integer status);
    
    /**
     * 更新活动（编辑）
     * @param activity 活动信息
     * @return 是否成功
     */
    boolean updateActivity(GroupBuyActivity activity);
    
    /**
     * 删除拼团活动
     * @param activityId 活动ID
     */
    void deleteActivity(Long activityId);
    
    // ========== 拼团操作 ==========
    
    /**
     * 发起拼团（开团）
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param quantity 数量
     * @return 返回团信息（含订单ID）
     */
    Map<String, Object> startTeam(Long activityId, Long userId, Long addressId, Integer quantity);
    
    /**
     * 参与拼团（参团）
     * @param teamId 团ID
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param quantity 数量
     * @return 返回订单ID
     */
    Map<String, Object> joinTeam(Long teamId, Long userId, Long addressId, Integer quantity);
    
    /**
     * 获取正在进行的团列表
     * @param activityId 活动ID
     * @return 团列表
     */
    List<GroupBuyTeam> getActiveTeams(Long activityId);
    
    /**
     * 获取团详情（含成员）
     * @param teamId 团ID
     * @return 团详情
     */
    GroupBuyTeam getTeamDetail(Long teamId);
    
    /**
     * 获取用户的拼团列表
     * @param userId 用户ID
     * @param status 状态筛选（可选）
     * @return 用户参与的团列表
     */
    List<GroupBuyTeam> getUserTeams(Long userId, Integer status);
    
    // ========== 内部方法（由支付回调和定时任务调用） ==========
    
    /**
     * 处理支付成功后的逻辑
     * @param orderId 订单ID
     */
    void handlePaymentSuccess(Long orderId);
    
    /**
     * 完成拼团（满员时调用）
     * @param teamId 团ID
     */
    void completeTeam(Long teamId);
    
    /**
     * 拼团失败（过期未满员）
     * @param teamId 团ID
     */
    void failTeam(Long teamId);
    
    /**
     * 拼团失败（带失败原因）
     * @param teamId 团ID
     * @param failReason 失败原因
     */
    void failTeam(Long teamId, String failReason);
    
    /**
     * 扫描过期的团（定时任务调用）
     */
    void checkExpiredTeams();
}

