package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.entity.*;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;
import org.example.springboot.service.AlipayService;
import org.example.springboot.service.GroupBuyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拼团业务实现类
 */
@Service
public class GroupBuyServiceImpl implements GroupBuyService {
    
    private static final Logger log = LoggerFactory.getLogger(GroupBuyServiceImpl.class);
    
    @Autowired
    private GroupBuyActivityMapper activityMapper;
    
    @Autowired
    private GroupBuyTeamMapper teamMapper;
    
    @Autowired
    private GroupBuyMemberMapper memberMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AddressMapper addressMapper;
    
    @Autowired
    private AlipayService alipayService;
    
    // ========== 活动管理 ==========
    
    @Override
    @Transactional
    public GroupBuyActivity createActivity(GroupBuyActivity activity) {
        // 检查商品是否存在
        Product product = productMapper.selectById(activity.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 自动设置原价（从商品价格获取）
        if (activity.getOriginalPrice() == null) {
            activity.setOriginalPrice(product.getPrice());
        }
        
        // 设置活动库存（从商品库存获取）
        if (activity.getStock() == null) {
            // 如果没有指定活动库存，使用商品的库存
            activity.setStock(product.getStock());
            log.info("活动库存未指定，使用商品库存: {}", product.getStock());
        } else {
            // 如果指定了活动库存，验证不能超过商品库存
            if (activity.getStock() > product.getStock()) {
                throw new BusinessException("活动库存不能超过商品库存（商品库存: " + product.getStock() + "）");
            }
        }
        
        // 参数验证
        if (activity.getGroupPrice().compareTo(activity.getOriginalPrice()) >= 0) {
            throw new BusinessException("拼团价必须小于原价");
        }
        if (activity.getRequiredMembers() < 2) {
            throw new BusinessException("成团人数至少为2人");
        }
        if (activity.getValidityHours() < 1 || activity.getValidityHours() > 72) {
            throw new BusinessException("拼团时限应在1-72小时之间");
        }
        
        // 设置其他默认值
        if (activity.getStatus() == null) {
            // ⭐ 根据开始时间自动判断状态
            Date now = new Date();
            if (activity.getStartTime() != null && activity.getStartTime().after(now)) {
                // 开始时间在未来，状态为未发布
                activity.setStatus(0);
                log.info("活动开始时间在未来，设置为未发布，开始时间: {}", activity.getStartTime());
            } else {
                // 开始时间是现在或过去，状态为进行中
                activity.setStatus(1);
                log.info("活动开始时间已到，设置为进行中，开始时间: {}", activity.getStartTime());
            }
        }
        if (activity.getSalesCount() == null) {
            activity.setSalesCount(0);
        }
        
        activityMapper.insert(activity);
        log.info("创建拼团活动成功，活动ID: {}，库存: {}", activity.getId(), activity.getStock());
        return activity;
    }
    
    @Override
    public Page<GroupBuyActivity> getActivityList(Integer currentPage, Integer size, Integer status) {
        Page<GroupBuyActivity> page = new Page<>(currentPage, size);
        QueryWrapper<GroupBuyActivity> wrapper = new QueryWrapper<>();
        
        // 按状态筛选
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        // 按创建时间倒序排列（最新的在前）
        wrapper.orderByDesc("created_at");
        
        Page<GroupBuyActivity> result = activityMapper.selectPage(page, wrapper);
        
        // 填充商品信息，并同步商品库存
        for (GroupBuyActivity activity : result.getRecords()) {
            Product product = productMapper.selectById(activity.getProductId());
            activity.setProduct(product);
            
            // ⭐ 使用商品的实际库存（系统设计：活动库存和商品库存同步）
            if (product != null) {
                activity.setStock(product.getStock());
            }
        }
        
        return result;
    }
    
    @Override
    public Page<GroupBuyActivity> getActiveActivityList(Integer currentPage, Integer size) {
        Page<GroupBuyActivity> page = new Page<>(currentPage, size);
        QueryWrapper<GroupBuyActivity> wrapper = new QueryWrapper<>();
        
        Date now = new Date();
        
        // 只查询进行中的活动（状态=1）
        wrapper.eq("status", 1);
        
        // 且当前时间在活动时间范围内
        wrapper.le("start_time", now)   // 开始时间 <= 当前时间
               .ge("end_time", now);     // 结束时间 >= 当前时间
        
        // 按创建时间倒序排列（最新的在前）
        wrapper.orderByDesc("created_at");
        
        Page<GroupBuyActivity> result = activityMapper.selectPage(page, wrapper);
        
        // 填充商品信息，并同步商品库存
        for (GroupBuyActivity activity : result.getRecords()) {
            Product product = productMapper.selectById(activity.getProductId());
            activity.setProduct(product);
            
            // ⭐ 使用商品的实际库存（系统设计：活动库存和商品库存同步）
            if (product != null) {
                activity.setStock(product.getStock());
            }
        }
        
        return result;
    }
    
    @Override
    public GroupBuyActivity getActivityDetail(Long activityId) {
        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        // 填充商品信息
        Product product = productMapper.selectById(activity.getProductId());
        activity.setProduct(product);
        
        // ⭐ 使用商品的实际库存，而不是活动的库存
        // 拼团活动的库存应该和商品库存保持一致（系统设计：同步管理）
        if (product != null) {
            activity.setStock(product.getStock());
        }
        
        return activity;
    }
    
    @Override
    @Transactional
    public boolean updateActivityStatus(Long activityId, Integer status) {
        // 1. 检查活动是否存在
        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        log.info("更新活动{}状态：当前状态={}，目标状态={}，活动结束时间={}", 
                activityId, activity.getStatus(), status, activity.getEndTime());
        
        // 2. 如果是上架(1)或开始操作，检查活动时间是否有效
        if (status == 1) {
            Date now = new Date();
            
            // 检查活动是否已过期
            if (activity.getEndTime() != null && activity.getEndTime().before(now)) {
                log.error("活动{}已过期：结束时间={}，当前时间={}", 
                        activityId, activity.getEndTime(), now);
                throw new BusinessException("活动已过期，无法开启。请先修改活动结束时间。");
            }
            
            // 检查活动是否还未开始
            if (activity.getStartTime() != null && activity.getStartTime().after(now)) {
                log.error("活动{}未开始：开始时间={}，当前时间={}", 
                        activityId, activity.getStartTime(), now);
                throw new BusinessException("活动开始时间未到，无法开启。当前时间：" + 
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now) + 
                    "，活动开始时间：" + 
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(activity.getStartTime()));
            }
            
            log.info("活动{}时间验证通过，允许开启", activityId);
        } else {
            log.info("活动{}操作不需要时间验证：目标状态={}", activityId, status);
        }
        
        // 3. 如果是下架(3)或结束(2)操作，需要处理进行中的团队
        if (status == 3 || status == 2) {
            // 查询所有进行中的团队
            QueryWrapper<GroupBuyTeam> teamQuery = new QueryWrapper<>();
            teamQuery.eq("activity_id", activityId)
                    .eq("status", 0); // 0-拼团中
            List<GroupBuyTeam> ongoingTeams = teamMapper.selectList(teamQuery);
            
            if (!ongoingTeams.isEmpty()) {
                String failReason = (status == 3) ? "cancelled_offline" : "cancelled_ended";
                log.info("管理员{}活动{}，需要处理 {} 个进行中的团队", 
                        status == 3 ? "下架" : "结束", activityId, ongoingTeams.size());
                
                // 让所有进行中的团队失败并退款
                for (GroupBuyTeam team : ongoingTeams) {
                    try {
                        failTeam(team.getId(), failReason);
                        log.info("成功处理团队{}的失败和退款", team.getId());
                    } catch (Exception e) {
                        log.error("处理团队{}失败时出错: ", team.getId(), e);
                        // 继续处理其他团队
                    }
                }
            }
        }
        
        // 4. 更新活动状态
        activity.setStatus(status);
        return activityMapper.updateById(activity) > 0;
    }
    
    @Override
    public boolean updateActivity(GroupBuyActivity activity) {
        return activityMapper.updateById(activity) > 0;
    }
    
    @Override
    @Transactional
    public void deleteActivity(Long activityId) {
        // 1. 检查活动是否存在
        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        // 2. 检查活动状态和是否有进行中的团
        // 状态：0-未发布 1-进行中 2-已结束 3-已下架
        if (activity.getStatus() == 1) {
            // 活动进行中，检查是否有进行中的团
            QueryWrapper<GroupBuyTeam> ongoingTeamQuery = new QueryWrapper<>();
            ongoingTeamQuery.eq("activity_id", activityId)
                           .eq("status", 0); // 0-拼团中
            long ongoingTeamCount = teamMapper.selectCount(ongoingTeamQuery);
            
            if (ongoingTeamCount > 0) {
                throw new BusinessException("活动进行中且有未完成的拼团，无法删除");
            }
        }
        
        // 3. 删除关联数据（按照外键依赖顺序删除）
        // 3.1 查询该活动的所有团
        QueryWrapper<GroupBuyTeam> teamQuery = new QueryWrapper<>();
        teamQuery.eq("activity_id", activityId);
        List<GroupBuyTeam> teams = teamMapper.selectList(teamQuery);
        
        // 3.2 删除每个团的成员记录
        for (GroupBuyTeam team : teams) {
            QueryWrapper<GroupBuyMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("team_id", team.getId());
            memberMapper.delete(memberQuery);
            log.info("删除团{}的成员记录", team.getId());
        }
        
        // 3.3 删除所有团记录
        if (!teams.isEmpty()) {
            teamMapper.delete(teamQuery);
            log.info("删除活动{}的所有团记录，共{}个", activityId, teams.size());
        }
        
        // 3.4 删除活动
        activityMapper.deleteById(activityId);
        
        log.info("删除拼团活动成功，活动ID: {}，状态: {}", activityId, activity.getStatus());
    }
    
    // ========== 拼团操作 ==========
    
    @Override
    @Transactional
    public Map<String, Object> startTeam(Long activityId, Long userId, Long addressId, Integer quantity) {
        // 1. 检查活动是否有效
        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new BusinessException("活动不存在或已下架");
        }
        
        // ✅ 获取商品信息并同步库存（因为系统设计是活动库存和商品库存同步）
        Product product = productMapper.selectById(activity.getProductId());
        if (product != null) {
            activity.setStock(product.getStock());
        }
        
        // 2. 检查库存（现在检查的是商品的实际库存）
        if (activity.getStock() < quantity) {
            throw new BusinessException("活动库存不足");
        }
        
        // 3. ⭐ 检查用户是否有进行中的团（防止同时多个进行中的团）
        // 逻辑：用户在同一活动中，同一时间只能有一个进行中的团
        // 如果团已经成功或失败，可以重新开团
        QueryWrapper<GroupBuyMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId)
                   .eq("is_leader", 1)  // 团长
                   .eq("status", 1);    // 已支付
        
        // 查询该用户作为团长的所有已支付记录
        List<GroupBuyMember> leaderRecords = memberMapper.selectList(memberQuery);
        
        if (!leaderRecords.isEmpty()) {
            // 检查是否在当前活动中有进行中的团
            for (GroupBuyMember member : leaderRecords) {
                GroupBuyTeam existingTeam = teamMapper.selectById(member.getTeamId());
                if (existingTeam != null 
                    && existingTeam.getActivityId().equals(activityId)
                    && existingTeam.getStatus() == 0) {  // ⭐ 关键：只检查进行中的团
                    // 用户在该活动中有一个进行中的团
                    throw new BusinessException("您已经在该活动中开团，请等待拼团结束后再开新团");
                }
            }
        }
        
        // 4. ⭐ 检查用户是否已有该活动的未支付订单（防止重复创建）
        QueryWrapper<Order> existingOrderQuery = new QueryWrapper<>();
        existingOrderQuery.eq("user_id", userId)
                         .eq("product_id", activity.getProductId())
                         .eq("order_type", 1)  // 拼团订单
                         .eq("status", 0)  // 待支付
                         .orderByDesc("created_at")
                         .last("LIMIT 1");
        Order existingOrder = orderMapper.selectOne(existingOrderQuery);
        
        if (existingOrder != null) {
            // 如果已有未支付订单，返回现有订单，不创建新订单
            log.info("用户{}已有该活动的未支付订单，订单ID: {}", userId, existingOrder.getId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", existingOrder.getId());
            result.put("activityId", activityId);
            result.put("isLeader", true);
            return result;
        }
        
        // 5. 获取地址信息
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }
        
        // 6. 商品信息已在前面获取，这里直接使用
        
        // 7. 创建新订单（状态为待支付）
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(activity.getProductId());
        order.setActivityId(activityId);  // ⭐ 设置活动ID
        order.setQuantity(quantity);
        order.setPrice(activity.getGroupPrice());
        order.setTotalPrice(activity.getGroupPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        order.setStatus(0); // 待支付
        order.setOrderType(1); // 拼团订单
        order.setRecvAddress(address.getAddress());
        order.setRecvPhone(address.getPhone());
        order.setRecvName(address.getReceiver());
        order.setPayMethod("alipay");
        orderMapper.insert(order);
        
        // ⭐ 设置 out_trade_no（支付回调需要通过这个字段查询订单）
        order.setOutTradeNo(String.valueOf(order.getId()));
        orderMapper.updateById(order);
        
        // 8. 创建拼团（暂时不创建，等支付成功后再创建）
        // 这样可以避免未支付的空团
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("activityId", activityId);
        result.put("isLeader", true);
        
        log.info("用户{}发起拼团，活动ID: {}, 订单ID: {}", userId, activityId, order.getId());
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> joinTeam(Long teamId, Long userId, Long addressId, Integer quantity) {
        // 1. 锁定团记录（防止并发问题）
        GroupBuyTeam team = teamMapper.selectForUpdate(teamId);
        if (team == null) {
            throw new BusinessException("拼团不存在");
        }
        
        // 2. 检查团状态
        if (team.getStatus() != 0) {
            throw new BusinessException("拼团已结束");
        }
        
        // 3. 检查是否已满员
        if (team.getCurrentPeople() >= team.getRequiredPeople()) {
            throw new BusinessException("拼团已满员");
        }
        
        // 4. 检查是否过期
        if (team.getExpireTime().before(new Date())) {
            throw new BusinessException("拼团已过期");
        }
        
        // 5. 检查用户是否已参团（已支付且未退款）
        QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("team_id", teamId)
                     .eq("user_id", userId)
                     .ne("status", 2);  // 排除已退款的成员，允许退款后重新加入
        if (memberMapper.selectCount(memberWrapper) > 0) {
            throw new BusinessException("您已参加该拼团");
        }
        
        // 5.5 ⭐ 检查用户是否已有该团的未支付订单（防止重复创建）
        QueryWrapper<Order> existingOrderQuery = new QueryWrapper<>();
        existingOrderQuery.eq("user_id", userId)
                         .eq("order_type", 1)  // 拼团订单
                         .eq("group_buy_team_id", teamId)
                         .eq("status", 0)  // 待支付
                         .orderByDesc("created_at")
                         .last("LIMIT 1");
        Order existingOrder = orderMapper.selectOne(existingOrderQuery);
        
        if (existingOrder != null) {
            // 如果已有未支付订单，返回现有订单，不创建新订单
            log.info("用户{}已有该团的未支付订单，订单ID: {}", userId, existingOrder.getId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", existingOrder.getId());
            result.put("teamId", teamId);
            result.put("isLeader", false);
            return result;
        }
        
        // 6. 获取活动信息
        GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
        if (activity == null || activity.getStatus() != 1) {
            throw new BusinessException("活动不存在或已下架");
        }
        
        // 7. 获取地址信息
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }
        
        // 8. 创建新订单（状态为待支付）
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(activity.getProductId());
        order.setActivityId(activity.getId());  // ⭐ 设置活动ID
        order.setQuantity(quantity);
        order.setPrice(activity.getGroupPrice());
        order.setTotalPrice(activity.getGroupPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        order.setStatus(0); // 待支付
        order.setOrderType(1); // 拼团订单
        order.setGroupBuyTeamId(teamId);
        order.setRecvAddress(address.getAddress());
        order.setRecvPhone(address.getPhone());
        order.setRecvName(address.getReceiver());
        order.setPayMethod("alipay");
        orderMapper.insert(order);
        
        // ⭐ 设置 out_trade_no（支付回调需要通过这个字段查询订单）
        order.setOutTradeNo(String.valueOf(order.getId()));
        orderMapper.updateById(order);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("teamId", teamId);
        result.put("isLeader", false);
        
        log.info("用户{}参与拼团，团ID: {}, 订单ID: {}", userId, teamId, order.getId());
        return result;
    }
    
    @Override
    public List<GroupBuyTeam> getActiveTeams(Long activityId) {
        QueryWrapper<GroupBuyTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("activity_id", activityId)
               .in("status", 0, 1) // 拼团中(0) 和 拼团成功(1)
               .orderByDesc("created_at")
               .last("LIMIT 50"); // 最多显示50个团
        
        List<GroupBuyTeam> teams = teamMapper.selectList(wrapper);
        
        // 填充团长和成员信息
        for (GroupBuyTeam team : teams) {
            User leader = userMapper.selectById(team.getLeaderUserId());
            team.setLeader(leader);
            
            // 获取成员列表
            QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
            memberWrapper.eq("team_id", team.getId());
            List<GroupBuyMember> members = memberMapper.selectList(memberWrapper);
            
            // 填充用户信息
            for (GroupBuyMember member : members) {
                User user = userMapper.selectById(member.getUserId());
                member.setUser(user);
            }
            team.setMembers(members);
        }
        
        return teams;
    }
    
    @Override
    public GroupBuyTeam getTeamDetail(Long teamId) {
        GroupBuyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("拼团不存在");
        }
        
        // 填充活动信息
        GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
        if (activity != null) {
            Product product = productMapper.selectById(activity.getProductId());
            activity.setProduct(product);
            team.setActivity(activity);
        }
        
        // 填充团长信息
        User leader = userMapper.selectById(team.getLeaderUserId());
        team.setLeader(leader);
        
        // 填充成员列表
        QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("team_id", teamId).orderByAsc("join_time");
        List<GroupBuyMember> members = memberMapper.selectList(memberWrapper);
        
        for (GroupBuyMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            member.setUser(user);
        }
        team.setMembers(members);
        
        return team;
    }
    
    @Override
    public List<GroupBuyTeam> getUserTeams(Long userId, Integer status) {
        // 通过成员表查询用户参与的团（排除已退款的成员）
        QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("user_id", userId)
                     .ne("status", 2); // 排除已退款的成员（status=2）
        List<GroupBuyMember> members = memberMapper.selectList(memberWrapper);
        
        if (members.isEmpty()) {
            return List.of();
        }
        
        // 获取团ID列表
        List<Long> teamIds = members.stream().map(GroupBuyMember::getTeamId).toList();
        
        // 查询团信息
        QueryWrapper<GroupBuyTeam> teamWrapper = new QueryWrapper<>();
        teamWrapper.in("id", teamIds);
        if (status != null) {
            teamWrapper.eq("status", status);
        }
        teamWrapper.orderByDesc("created_at");
        
        List<GroupBuyTeam> teams = teamMapper.selectList(teamWrapper);
        
        // 填充详细信息
        for (GroupBuyTeam team : teams) {
            GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
            if (activity != null) {
                Product product = productMapper.selectById(activity.getProductId());
                activity.setProduct(product);
                team.setActivity(activity);
            }
            
            User leader = userMapper.selectById(team.getLeaderUserId());
            team.setLeader(leader);
        }
        
        return teams;
    }
    
    // ========== 内部方法 ==========
    
    @Override
    @Transactional
    public void handlePaymentSuccess(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getOrderType() != 1) {
            return; // 不是拼团订单，不处理
        }
        
        // 注意：订单状态已经在 AlipayController 中更新，这里只处理拼团逻辑
        log.info("开始处理拼团逻辑，订单ID: {}", orderId);
        
        // ⭐ 获取拼团活动
        GroupBuyActivity activity = activityMapper.selectById(order.getActivityId());
        if (activity == null) {
            log.error("拼团活动不存在，activityId: {}", order.getActivityId());
            throw new BusinessException("拼团活动不存在");
        }
        
        // ✅ 同步商品库存（系统设计：活动库存和商品库存同步）
        Product productForSync = productMapper.selectById(activity.getProductId());
        if (productForSync != null) {
            activity.setStock(productForSync.getStock());
            log.info("同步商品库存到活动，活动ID: {}, 商品库存: {}", activity.getId(), productForSync.getStock());
        }
        
        // ⭐ 扣减库存（支付成功时只扣库存，不增加销量）
        // 说明：销量只在拼团成功时增加，避免重复计算
        if (activity.getStock() < order.getQuantity()) {
            log.error("活动库存不足，活动ID: {}, 需要: {}, 库存: {}", 
                activity.getId(), order.getQuantity(), activity.getStock());
            throw new BusinessException("活动库存不足");
        }
        
        activity.setStock(activity.getStock() - order.getQuantity());
        // ❌ 不在这里增加销量：activity.setSalesCount(activity.getSalesCount() + order.getQuantity());
        activityMapper.updateById(activity);
        log.info("扣减活动库存成功（不增加销量），活动ID: {}, 扣减数量: {}, 剩余库存: {}", 
            activity.getId(), order.getQuantity(), activity.getStock());
        
        // ⭐ 同步扣减商品库存（也不增加销量）
        Product product = productMapper.selectById(activity.getProductId());
        if (product != null) {
            if (product.getStock() >= order.getQuantity()) {
                product.setStock(product.getStock() - order.getQuantity());
                // ❌ 不在这里增加销量：product.setSalesCount(product.getSalesCount() + order.getQuantity());
                productMapper.updateById(product);
                log.info("扣减商品库存成功（不增加销量），商品ID: {}, 扣减数量: {}, 剩余库存: {}", 
                    product.getId(), order.getQuantity(), product.getStock());
            }
        }
        
        // 检查是否已有团ID
        if (order.getGroupBuyTeamId() == null) {
            // 开团：创建新团
            GroupBuyTeam team = new GroupBuyTeam();
            team.setActivityId(activity.getId());
            team.setLeaderUserId(order.getUserId());
            team.setLeaderOrderId(orderId);
            team.setCurrentPeople(1);
            team.setRequiredPeople(activity.getRequiredMembers());
            team.setStatus(0); // 拼团中
            
            // 计算过期时间
            long expireMillis = System.currentTimeMillis() + activity.getValidityHours() * 3600000L;
            team.setExpireTime(new Timestamp(expireMillis));
            
            teamMapper.insert(team);
            
            // 更新订单的团ID
            order.setGroupBuyTeamId(team.getId());
            orderMapper.updateById(order);
            
            // 创建成员记录（团长）
            GroupBuyMember member = new GroupBuyMember();
            member.setTeamId(team.getId());
            member.setUserId(order.getUserId());
            member.setOrderId(orderId);
            member.setIsLeader(1);
            member.setStatus(1);  // ⭐ 已支付（因为是支付成功后才调用此方法）
            member.setPayTime(new Timestamp(System.currentTimeMillis()));  // ⭐ 支付时间
            memberMapper.insert(member);
            
            log.info("创建拼团成功，团ID: {}, 团长: {}", team.getId(), order.getUserId());
        } else {
            // 参团：加入已有团
            Long teamId = order.getGroupBuyTeamId();
            GroupBuyTeam team = teamMapper.selectForUpdate(teamId);
            
            if (team != null && team.getStatus() == 0) {
                // 增加团人数
                team.setCurrentPeople(team.getCurrentPeople() + 1);
                teamMapper.updateById(team);
                
                // 创建成员记录
                GroupBuyMember member = new GroupBuyMember();
                member.setTeamId(teamId);
                member.setUserId(order.getUserId());
                member.setOrderId(orderId);
                member.setIsLeader(0);
                member.setStatus(1);  // ⭐ 已支付（因为是支付成功后才调用此方法）
                member.setPayTime(new Timestamp(System.currentTimeMillis()));  // ⭐ 支付时间
                memberMapper.insert(member);
                
                log.info("用户{}参团成功，团ID: {}, 当前人数: {}/{}", 
                        order.getUserId(), teamId, team.getCurrentPeople(), team.getRequiredPeople());
                
                // 检查是否满员
                if (team.getCurrentPeople() >= team.getRequiredPeople()) {
                    completeTeam(teamId);
                }
            }
        }
    }
    
    @Override
    @Transactional
    public void completeTeam(Long teamId) {
        GroupBuyTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus() != 0) {
            return;
        }
        
        // 更新团状态为成功
        team.setStatus(1);
        team.setSuccessTime(new Timestamp(System.currentTimeMillis()));
        teamMapper.updateById(team);
        
        // 获取所有成员订单
        QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("team_id", teamId);
        List<GroupBuyMember> members = memberMapper.selectList(memberWrapper);
        
        // 更新所有订单状态为"待发货"
        for (GroupBuyMember member : members) {
            Order order = orderMapper.selectById(member.getOrderId());
            if (order != null && order.getStatus() == 0) {
                order.setStatus(1); // 待发货
                orderMapper.updateById(order);
            }
        }
        
        // ⭐ 拼团成功时增加销量（库存已在支付时扣减，这里不重复扣减）
        GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
        if (activity != null) {
            // ❌ 不再扣减库存：activity.setStock(activity.getStock() - team.getCurrentPeople());
            // ✅ 只增加销量
            activity.setSalesCount(activity.getSalesCount() + team.getCurrentPeople());
            activityMapper.updateById(activity);
            log.info("拼团成功，增加活动销量，活动ID: {}, 销量: +{}", 
                activity.getId(), team.getCurrentPeople());
        }
        
        // ⭐ 同时增加商品销量
        Product product = productMapper.selectById(activity.getProductId());
        if (product != null) {
            product.setSalesCount(product.getSalesCount() + team.getCurrentPeople());
            productMapper.updateById(product);
            log.info("拼团成功，增加商品销量，商品ID: {}, 销量: +{}", 
                product.getId(), team.getCurrentPeople());
        }
        
        log.info("拼团成功！团ID: {}, 人数: {}", teamId, team.getCurrentPeople());
        // TODO: 发送成团通知给所有成员
    }
    
    @Override
    @Transactional
    public void failTeam(Long teamId) {
        failTeam(teamId, "timeout");
    }
    
    /**
     * 让拼团失败（带失败原因）
     * @param teamId 团队ID
     * @param failReason 失败原因：timeout-超时, cancelled-管理员取消, leader_refund-团长退款, member_refund-成员退款
     */
    public void failTeam(Long teamId, String failReason) {
        GroupBuyTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus() != 0) {
            log.warn("团不存在或状态不是拼团中，无需处理失败逻辑，团ID: {}", teamId);
            return;
        }
        
        log.info("开始处理拼团失败，团ID: {}，当前人数: {}/{}，失败原因: {}",
                teamId, team.getCurrentPeople(), team.getRequiredPeople(), failReason);
        
        // 1. 更新团状态为失败
        team.setStatus(2);
        team.setFailReason(failReason);
        team.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        teamMapper.updateById(team);
        
        // 2. 获取拼团活动信息
        GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
        if (activity == null) {
            log.error("拼团活动不存在，activityId: {}", team.getActivityId());
            return;
        }
        
        // 3. 获取所有已支付的成员订单
        QueryWrapper<GroupBuyMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("team_id", teamId)
                    .eq("status", 1); // 只处理已支付的成员
        List<GroupBuyMember> members = memberMapper.selectList(memberWrapper);
        
        if (members.isEmpty()) {
            log.info("该团没有已支付的成员，无需退款，团ID: {}", teamId);
            return;
        }
        
        log.info("团ID: {} 有 {} 个已支付成员需要退款", teamId, members.size());
        
        int successCount = 0;
        int failCount = 0;
        int totalQuantity = 0;
        
        // 4. 对所有已支付订单进行退款和库存恢复
        for (GroupBuyMember member : members) {
            Order order = orderMapper.selectById(member.getOrderId());
            if (order == null) {
                log.error("订单不存在，订单ID: {}", member.getOrderId());
                continue;
            }
            
            // 只处理已支付的订单（status=1）
            if (order.getStatus() != 1) {
                log.warn("订单状态不是已支付，跳过，订单ID: {}，状态: {}", order.getId(), order.getStatus());
                continue;
            }
            
            try {
                // 4.1 调用支付宝退款
                if (order.getTradeNo() != null && !order.getTradeNo().isEmpty()) {
                    alipayService.refund(
                            order.getId().toString(),
                            order.getTotalPrice().toString()
                    );
                    log.info("支付宝退款成功，订单ID: {}，金额: {}", order.getId(), order.getTotalPrice());
                } else {
                    log.warn("订单没有支付宝交易号，可能未实际支付，订单ID: {}", order.getId());
                }
                
                // 4.2 更新订单状态为已退款
                order.setLastStatus(order.getStatus());
                order.setStatus(6); // 6-已退款
                order.setRefundStatus(3); // 3-已退款
                order.setRefundTime(new Timestamp(System.currentTimeMillis()));
                order.setRefundReason("拼团失败自动退款");
                orderMapper.updateById(order);
                
                // 4.3 累计需要恢复的库存数量
                totalQuantity += order.getQuantity();
                
                successCount++;
                log.info("订单退款成功，订单ID: {}，数量: {}", order.getId(), order.getQuantity());
                
            } catch (Exception e) {
                failCount++;
                log.error("订单退款失败，订单ID: " + order.getId(), e);
                
                // 即使退款失败，也更新订单状态为退款中
                order.setLastStatus(order.getStatus());
                order.setStatus(5); // 5-退款中
                order.setRefundStatus(1); // 1-申请退款
                order.setRefundTime(new Timestamp(System.currentTimeMillis()));
                order.setRefundReason("拼团失败自动退款（失败，需人工处理）");
                orderMapper.updateById(order);
            }
        }
        
        // 5. 恢复活动库存（不需要恢复销量，因为从未增加过）
        if (totalQuantity > 0) {
            activity.setStock(activity.getStock() + totalQuantity);
            // ❌ 不需要减少销量：activity.setSalesCount(Math.max(0, activity.getSalesCount() - totalQuantity));
            // 说明：拼团失败时，销量从未增加，所以不需要恢复
            activityMapper.updateById(activity);
            log.info("活动库存恢复成功（不恢复销量），活动ID: {}，恢复数量: {}，当前库存: {}",
                    activity.getId(), totalQuantity, activity.getStock());
            
            // 6. 恢复商品库存（也不需要恢复销量）
            Product product = productMapper.selectById(activity.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + totalQuantity);
                // ❌ 不需要减少销量：product.setSalesCount(Math.max(0, product.getSalesCount() - totalQuantity));
                productMapper.updateById(product);
                log.info("商品库存恢复成功（不恢复销量），商品ID: {}，恢复数量: {}，当前库存: {}",
                        product.getId(), totalQuantity, product.getStock());
            }
        }
        
        log.info("拼团失败处理完成，团ID: {}，退款成功: {}，退款失败: {}，恢复库存: {}",
                teamId, successCount, failCount, totalQuantity);
        
        // TODO: 发送拼团失败通知给所有成员
    }
    
    @Override
    public void checkExpiredTeams() {
        // 查询所有拼团中且已过期的团
        QueryWrapper<GroupBuyTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
               .lt("expire_time", new Date());
        
        List<GroupBuyTeam> expiredTeams = teamMapper.selectList(wrapper);
        
        log.info("扫描到{}个过期拼团", expiredTeams.size());
        
        for (GroupBuyTeam team : expiredTeams) {
            if (team.getCurrentPeople() >= team.getRequiredPeople()) {
                // 已满员，自动成团
                completeTeam(team.getId());
            } else {
                // 未满员，失败退款
                failTeam(team.getId());
            }
        }
    }
}

