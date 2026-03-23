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
                   .lt("expire_time", new Date()); // 已过期
            
            List<GroupBuyTeam> expiredTeams = teamMapper.selectList(wrapper);
            
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
        try {
            Date now = new Date();
            
            // 查询所有状态为"进行中"但已经过了结束时间的活动
            QueryWrapper<GroupBuyActivity> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1)  // 进行中
                   .lt("end_time", now);  // 结束时间 < 当前时间
            
            List<GroupBuyActivity> expiredActivities = activityMapper.selectList(wrapper);
            
            if (!expiredActivities.isEmpty()) {
                log.info("发现 {} 个已过期的活动，开始更新状态...", expiredActivities.size());
                
                int successCount = 0;
                for (GroupBuyActivity activity : expiredActivities) {
                    try {
                        // 更新活动状态为"已结束"
                        activity.setStatus(2);  // 2-已结束
                        activity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                        activityMapper.updateById(activity);
                        
                        successCount++;
                        log.info("活动已自动结束，活动ID: {}，活动名称: {}，结束时间: {}",
                                activity.getId(), activity.getActivityName(), activity.getEndTime());
                    } catch (Exception e) {
                        log.error("更新活动状态失败，活动ID: " + activity.getId(), e);
                    }
                }
                
                log.info("过期活动状态更新完成，成功: {}，失败: {}", 
                        successCount, expiredActivities.size() - successCount);
            }
        } catch (Exception e) {
            log.error("检查过期活动任务执行失败", e);
        }
    }
}
