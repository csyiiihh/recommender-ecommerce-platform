package org.example.springboot.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.mapper.GroupBuyActivityMapper;
import org.example.springboot.mapper.GroupBuyTeamMapper;
import org.example.springboot.service.GroupBuyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * 拼团定时任务
 * 用于检查过期的拼团并自动处理退款和库存恢复
 * 以及自动更新过期活动的状态
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "group-buy.tasks.enabled", havingValue = "true", matchIfMissing = true)
public class GroupBuyTask {
    
    private static final Logger log = LoggerFactory.getLogger(GroupBuyTask.class);
    
    @Autowired
    private GroupBuyTeamMapper teamMapper;
    
    @Autowired
    private GroupBuyActivityMapper activityMapper;
    
    @Autowired
    private GroupBuyService groupBuyService;
    
    /**
     * 每分钟检查一次过期的拼团
     * cron表达式：每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkExpiredTeams() {
        try {
            // 查询所有过期且未完成的拼团
            QueryWrapper<GroupBuyTeam> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 0) // 拼团中
                   .lt("expire_time", new Date())
                   .orderByAsc("expire_time", "id").last("LIMIT 100"); // bounded scan
            
            List<GroupBuyTeam> expiredTeams = teamMapper.selectList(wrapper);
            var failedWithPaidOrders = teamMapper.selectList(new QueryWrapper<GroupBuyTeam>().eq("status", 2)
                .inSql("id", "SELECT group_buy_team_id FROM `order` WHERE (status = 1 OR (status = 5 AND refund_status = 1 AND refund_reason LIKE '拼团失败自动退款%')) AND order_type = 1")
                .orderByAsc("id").last("LIMIT 100"));
            for (GroupBuyTeam failed : failedWithPaidOrders) {
                try { groupBuyService.failTeam(failed.getId(), failed.getFailReason()); }
                catch (Exception e) { log.error("重试拼团失败处理异常，团ID={}", failed.getId(), e); }
            }
            
            if (!expiredTeams.isEmpty()) {
                log.info("发现 {} 个过期的拼团，开始处理...", expiredTeams.size());
                
                for (GroupBuyTeam team : expiredTeams) {
                    try {
                        // 调用拼团失败处理方法
                        groupBuyService.failTeam(team.getId());
                        log.info("拼团失败处理成功，团ID: {}，当前人数: {}/{}",
                                team.getId(), team.getCurrentPeople(), team.getRequiredPeople());
                    } catch (Exception e) {
                        log.error("处理过期拼团失败，团ID: " + team.getId(), e);
                    }
                }
                
                log.info("过期拼团处理完成，共处理 {} 个", expiredTeams.size());
            }
        } catch (Exception e) {
            log.error("检查过期拼团任务执行失败", e);
        }
    }
    
    /**
     * 每30分钟执行一次健康检查
     * 用于记录系统状态
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void healthCheck() {
        try {
            // 统计当前拼团中的数量
            QueryWrapper<GroupBuyTeam> ongoingWrapper = new QueryWrapper<>();
            ongoingWrapper.eq("status", 0);
            long ongoingCount = teamMapper.selectCount(ongoingWrapper);
            
            // 统计今天成功的拼团数量
            QueryWrapper<GroupBuyTeam> successWrapper = new QueryWrapper<>();
            successWrapper.eq("status", 1)
                         .apply("DATE(success_time) = CURDATE()");
            long successCount = teamMapper.selectCount(successWrapper);
            
            log.info("拼团系统健康检查 - 进行中: {}，今日成功: {}", ongoingCount, successCount);
        } catch (Exception e) {
            log.error("健康检查失败", e);
        }
    }
    
    /**
     * 每5分钟检查一次过期的活动，自动更新状态
     * cron表达式：每5分钟的第0秒执行
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void checkExpiredActivities() {
        java.time.Instant cutoff = java.time.Instant.now();
        try {
            List<GroupBuyActivity> due = activityMapper.selectList(new QueryWrapper<GroupBuyActivity>()
                .eq("status", 1).lt("end_time", Timestamp.from(cutoff))
                .orderByAsc("end_time", "id").last("LIMIT 100"));
            for (GroupBuyActivity activity : due) {
                try {
                    groupBuyService.expireActivityIfDue(activity.getId(), cutoff);
                } catch (Exception e) {
                    log.error("活动到期处理失败，活动ID={}", activity.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("查询到期活动失败", e);
        }
    }
}
