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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.springboot.service.CheckoutIdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 拼团业务实现类
 */
@Service
public class GroupBuyServiceImpl implements GroupBuyService {
    @Autowired private org.example.springboot.service.AutomaticRefundService automaticRefunds;
    @Autowired private CheckoutIdempotencyService checkoutRequests;

    
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
    public GroupBuyActivity createActivity(GroupBuyActivity input) {
        Product product = productMapper.selectForUpdate(input.getProductId());
        validateActivity(input, product);
        if (input.getEndTime().before(new Date())) throw new BusinessException("结束时间必须晚于当前时间");
        GroupBuyActivity activity = editableActivity(input);
        activity.setId(null);
        activity.setMerchantId(product.getMerchantId());
        activity.setOriginalPrice(product.getPrice());
        activity.setStock(product.getStock());
        activity.setSalesCount(0);
        activity.setStatus(input.getStartTime().after(new Date()) ? 0 : 1);
        if (activityMapper.insert(activity) != 1) throw new BusinessException("创建活动失败");
        return activity;
    }
    
    @Override
    public Page<GroupBuyActivity> getActivityList(Integer currentPage, Integer size, Integer status) {
        return getActivityList(currentPage, size, status, null, null);
    }

    @Override
    public Page<GroupBuyActivity> getActivityList(Integer currentPage, Integer size, Integer status, Long merchantId, String activityName) {
        QueryWrapper<GroupBuyActivity> q = new QueryWrapper<>();
        if (status != null) q.eq("status", status);
        if (merchantId != null) q.eq("merchant_id", merchantId);
        if (activityName != null && !activityName.isBlank()) q.like("activity_name", activityName.strip());
        q.orderByDesc("created_at").orderByDesc("id");
        Page<GroupBuyActivity> result = activityMapper.selectPage(new Page<>(currentPage, Math.min(size, 100)), q);
        fillActivityProducts(result.getRecords());
        return result;
    }
    
    @Override
    public Page<GroupBuyActivity> getActiveActivityList(Integer currentPage, Integer size) {
        Page<GroupBuyActivity> page = new Page<>(currentPage, size);
        QueryWrapper<GroupBuyActivity> wrapper = new QueryWrapper<>();
        
        Date now = new Date();
        
        // 只查询进行中的活动（状态=1）
        wrapper.eq("status", 1).inSql("product_id", "SELECT id FROM product WHERE status = 1");
        
        // 且当前时间在活动时间范围内
        wrapper.le("start_time", now)   // 开始时间 <= 当前时间
               .ge("end_time", now);     // 结束时间 >= 当前时间
        
        // 按创建时间倒序排列（最新的在前）
        wrapper.orderByDesc("created_at");
        
        Page<GroupBuyActivity> result = activityMapper.selectPage(page, wrapper);
        
        fillActivityProducts(result.getRecords());

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
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActivityStatus(Long activityId, Integer status) {
        if (status == null || !Set.of(0, 1, 2, 3).contains(status)) throw new BusinessException("无效活动状态");
        GroupBuyActivity activity = activityMapper.selectForUpdate(activityId);
        if (activity == null) throw new BusinessException("活动不存在");
        if (status == 0 && hasActivityHistory(activityId)) throw new BusinessException("已有历史的活动不能恢复为草稿，请下架或结束");
        if (status == 1) {
            Date now = new Date();
            if (activity.getStartTime() == null || activity.getEndTime() == null || !activity.getEndTime().after(now)) {
                throw new BusinessException("活动已过期或时间无效，请修改活动时间");
            }
            if (activity.getStartTime().after(now)) throw new BusinessException("活动开始时间未到，暂不能开启");
        }
        writeActivityState(activityId, status, status == 3 ? "cancelled_offline" : "cancelled_ended");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean expireActivityIfDue(Long activityId, java.time.Instant cutoff) {
        if (cutoff == null) throw new IllegalArgumentException("Missing expiry cutoff");
        GroupBuyActivity activity = activityMapper.selectForUpdate(activityId);
        if (activity == null || !Integer.valueOf(1).equals(activity.getStatus()) || activity.getEndTime() == null
            || !activity.getEndTime().toInstant().isBefore(cutoff)) return false;
        writeActivityState(activityId, 2, "cancelled_ended");
        return true;
    }

    private void writeActivityState(Long activityId, int status, String failReason) {
        if (status == 2 || status == 3) {
            List<GroupBuyTeam> ongoing = teamMapper.selectList(new QueryWrapper<GroupBuyTeam>()
                .eq("activity_id", activityId).eq("status", 0).orderByAsc("id"));
            for (GroupBuyTeam team : ongoing) failTeam(team.getId(), failReason);
        }
        GroupBuyActivity patch = new GroupBuyActivity();
        patch.setId(activityId); patch.setStatus(status); patch.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        if (activityMapper.updateById(patch) != 1) throw new BusinessException("更新活动状态失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActivity(GroupBuyActivity input) {
        Product product = productMapper.selectForUpdate(input.getProductId());
        GroupBuyActivity old = activityMapper.selectForUpdate(input.getId());
        if (old == null) throw new BusinessException("活动不存在");
        validateActivity(input, product);
        if (hasActivityHistory(old.getId()) && tradingRulesChanged(old, input)) {
            throw new BusinessException("活动已有交易或团队历史，仅允许修改名称和说明");
        }
        GroupBuyActivity patch = editableActivity(input);
        patch.setMerchantId(product.getMerchantId());
        patch.setOriginalPrice(product.getPrice());
        // stock, salesCount and status are not part of editableActivity.
        return activityMapper.updateById(patch) == 1;
    }
    
    @Override
    @Transactional
    public void deleteActivity(Long activityId) {
        GroupBuyActivity activity = activityMapper.selectForUpdate(activityId);
        if (activity == null) throw new BusinessException("活动不存在");
        if (hasActivityHistory(activityId)) {
            throw new BusinessException("活动已有订单或团队历史，请下架保留记录");
        }
        if (activityMapper.deleteById(activityId) != 1) throw new BusinessException("删除活动失败");
    }
    
    // ========== 拼团操作 ==========
    
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String, Object> startTeam(Long activityId, Long userId, Long addressId, Integer quantity) {
        return startTeam(activityId, userId, addressId, quantity, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String, Object> startTeam(Long activityId, Long userId, Long addressId, Integer quantity, String requestKey) {
        return createGroupOrder(activityId, userId, addressId, quantity, requestKey, false);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String, Object> joinTeam(Long teamId, Long userId, Long addressId, Integer quantity) {
        return joinTeam(teamId, userId, addressId, quantity, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String, Object> joinTeam(Long teamId, Long userId, Long addressId, Integer quantity, String requestKey) {
        return createGroupOrder(teamId, userId, addressId, quantity, requestKey, true);
    }
    
    @Override
    public List<GroupBuyTeam> getActiveTeams(Long activityId) {
        QueryWrapper<GroupBuyTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("activity_id", activityId)
               .in("status", 0, 1) // 拼团中(0) 和 拼团成功(1)
               .orderByDesc("created_at")
               .last("LIMIT 50"); // 最多显示50个团
        
        List<GroupBuyTeam> teams = teamMapper.selectList(wrapper);
        
        fillTeamPeople(teams);

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
        
        fillTeamPeople(List.of(team));

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
        
        Set<Long> activityIds = teams.stream().map(GroupBuyTeam::getActivityId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<GroupBuyActivity> rows = activityIds.isEmpty() ? List.of() : activityMapper.selectBatchIds(activityIds);
        fillActivityProducts(rows);
        Map<Long, GroupBuyActivity> activities = rows.stream().collect(Collectors.toMap(GroupBuyActivity::getId, x -> x));
        fillTeamPeople(teams);
        for (GroupBuyTeam team : teams) team.setActivity(activities.get(team.getActivityId()));

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
        GroupBuyActivity activity = activityMapper.selectForUpdate(order.getActivityId());
        if (activity == null) {
            log.error("拼团活动不存在，activityId: {}", order.getActivityId());
            throw new BusinessException("拼团活动不存在");
        }
        
        // ✅ 同步商品库存（系统设计：活动库存和商品库存同步）
        Product productForSync = productMapper.selectForUpdate(activity.getProductId());
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
        Product product = productMapper.selectForUpdate(activity.getProductId());
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
        GroupBuyTeam team = teamMapper.selectForUpdate(teamId);
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
        
        int soldQuantity = members.stream().filter(m -> Integer.valueOf(1).equals(m.getStatus())).map(m -> orderMapper.selectById(m.getOrderId())).filter(java.util.Objects::nonNull).mapToInt(Order::getQuantity).sum();
        // ⭐ 拼团成功时增加销量（库存已在支付时扣减，这里不重复扣减）
        GroupBuyActivity activity = activityMapper.selectForUpdate(team.getActivityId());
        if (activity != null) {
            // ❌ 不再扣减库存：activity.setStock(activity.getStock() - team.getCurrentPeople());
            // ✅ 只增加销量
            activity.setSalesCount(activity.getSalesCount() + soldQuantity);
            activityMapper.updateById(activity);
            log.info("拼团成功，增加活动销量，活动ID: {}, 销量: +{}", 
                activity.getId(), team.getCurrentPeople());
        }
        
        // ⭐ 同时增加商品销量
        Product product = productMapper.selectForUpdate(activity.getProductId());
        if (product != null) {
            product.setSalesCount(product.getSalesCount() + soldQuantity);
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
    @Transactional(rollbackFor = Exception.class)
    public void failTeam(Long teamId, String failReason) {
        GroupBuyTeam team = teamMapper.selectForUpdate(teamId);
        if (team == null || team.getStatus() == 1) return;
        if (team.getStatus() == 0) {
            team.setStatus(2);
            team.setFailReason(failReason);
            team.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            if (teamMapper.updateById(team) != 1) throw new BusinessException("更新拼团失败状态失败");
        }
        // No remote calls in the team transition. Pending orders survive failures and restarts.
        automaticRefunds.queueTeam(teamId);
    }
    

    private Map<String, Object> createGroupOrder(Long targetId, Long userId, Long addressId, Integer quantity, String requestKey, boolean joining) {
        if (targetId == null || targetId < 1 || userId == null || userId < 1 || addressId == null || addressId < 1
            || quantity == null || quantity < 1 || quantity > 1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效拼团参数");
        CheckoutIdempotencyService.Token request = null;
        if (requestKey != null) {
            request = checkoutRequests.prepare(userId, joining ? "group-join" : "group-start", requestKey, List.of(targetId, addressId, quantity));
            if (!request.existing().isEmpty()) return groupOrderResult(request.existing().get(0), joining, true);
        } else {
            User buyer = userMapper.selectForUpdate(userId);
            if (buyer == null || !Integer.valueOf(1).equals(buyer.getStatus())) throw new BusinessException("用户不可用");
        }
        GroupBuyTeam initialTeam = joining ? teamMapper.selectById(targetId) : null;
        if (joining && initialTeam == null) throw new BusinessException("拼团不存在");
        Long activityId = joining ? initialTeam.getActivityId() : targetId;
        GroupBuyActivity snapshot = activityMapper.selectById(activityId);
        if (snapshot == null) throw new BusinessException("活动不存在");
        // Match payment's product -> activity -> team lock order.
        Product product = productMapper.selectForUpdate(snapshot.getProductId());
        GroupBuyActivity activity = activityMapper.selectForUpdate(activityId);
        if (activity != null && !Objects.equals(snapshot.getProductId(), activity.getProductId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "活动商品已变化，请刷新重试");
        }
        Date now = new Date();
        if (activity == null || !Integer.valueOf(1).equals(activity.getStatus()) || activity.getStartTime() == null
            || activity.getStartTime().after(now) || activity.getEndTime() == null || !activity.getEndTime().after(now)) {
            throw new BusinessException("活动未开始、已结束或已下架");
        }
        if (product == null || !Integer.valueOf(1).equals(product.getStatus()) || product.getStock() == null || product.getStock() < quantity) {
            throw new BusinessException("商品已下架或库存不足");
        }
        GroupBuyTeam team = joining ? teamMapper.selectForUpdate(targetId) : null;
        if (joining && (team == null || !Integer.valueOf(0).equals(team.getStatus()) || team.getCurrentPeople() >= team.getRequiredPeople()
            || team.getExpireTime() == null || !team.getExpireTime().after(now))) throw new BusinessException("拼团已结束或满员");
        if (joining && memberMapper.selectCount(new QueryWrapper<GroupBuyMember>().eq("team_id", targetId).eq("user_id", userId).ne("status", 2)) > 0) {
            throw new BusinessException("您已参加该拼团");
        }
        if (!joining && teamMapper.selectCount(new QueryWrapper<GroupBuyTeam>().eq("activity_id", activityId).eq("leader_user_id", userId).eq("status", 0)) > 0) {
            throw new BusinessException("您已经在该活动中开团，请等待拼团结束后再开新团");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null || !Objects.equals(userId, address.getUserId())) throw new BusinessException("收货地址不可用");
        QueryWrapper<Order> q = new QueryWrapper<Order>().eq("user_id", userId).eq("activity_id", activityId).eq("order_type", 1).eq("status", 0);
        if (joining) q.eq("group_buy_team_id", targetId); else q.isNull("group_buy_team_id");
        Order existing = orderMapper.selectOne(q.orderByDesc("id").last("LIMIT 1"));
        if (existing != null) {
            if (request != null || !Objects.equals(existing.getQuantity(), quantity)
                || !Objects.equals(existing.getRecvName(), address.getReceiver()) || !Objects.equals(existing.getRecvPhone(), address.getPhone())
                || !Objects.equals(existing.getRecvAddress(), address.getAddress())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "已有待支付拼团订单，请先处理原订单");
            }
            return groupOrderResult(existing, joining, true);
        }
        if (activity.getGroupPrice() == null || activity.getGroupPrice().signum() <= 0) throw new BusinessException("活动价格无效");
        Order order = new Order();
        order.setUserId(userId); order.setProductId(product.getId()); order.setActivityId(activityId);
        if (joining) order.setGroupBuyTeamId(targetId);
        order.setQuantity(quantity); order.setPrice(activity.getGroupPrice()); order.setTotalPrice(activity.getGroupPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        order.setStatus(0); order.setOrderType(1); order.setRefundStatus(0);
        order.setRecvName(address.getReceiver()); order.setRecvPhone(address.getPhone()); order.setRecvAddress(address.getAddress());
        order.setPayMethod("alipay");
        order.setOutTradeNo(request == null ? java.util.UUID.randomUUID().toString().replace("-", "") : request.tradeNo());
        if (orderMapper.insert(order) != 1) throw new BusinessException("创建拼团订单失败");
        return groupOrderResult(order, joining, false);
    }

    private Map<String, Object> groupOrderResult(Order order, boolean joining, boolean replayed) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId()); result.put("activityId", order.getActivityId());
        if (joining) result.put("teamId", order.getGroupBuyTeamId());
        result.put("isLeader", !joining); result.put("replayed", replayed);
        result.put("paymentAllowed", Integer.valueOf(0).equals(order.getStatus()));
        return result;
    }

    private void validateActivity(GroupBuyActivity a, Product p) {
        if (p == null || !Integer.valueOf(1).equals(p.getStatus()) || p.getMerchantId() == null || p.getPrice() == null || p.getPrice().signum() <= 0) throw new BusinessException("请选择有效的上架商品");
        if (a.getActivityName() == null || a.getActivityName().isBlank() || a.getActivityName().length() > 100) throw new BusinessException("活动名称无效");
        if (a.getGroupPrice() == null || a.getGroupPrice().signum() <= 0 || a.getGroupPrice().scale() > 2 || a.getGroupPrice().compareTo(p.getPrice()) >= 0) throw new BusinessException("拼团价须大于0、小于商品原价且最多两位小数");
        if (a.getRequiredMembers() == null || a.getRequiredMembers() < 2 || a.getRequiredMembers() > 100) throw new BusinessException("成团人数应在2至100人之间");
        if (a.getValidityHours() == null || a.getValidityHours() < 1 || a.getValidityHours() > 72) throw new BusinessException("拼团时限应在1至72小时之间");
        if (a.getStartTime() == null || a.getEndTime() == null || !a.getEndTime().after(a.getStartTime())) throw new BusinessException("结束时间必须晚于开始时间");
    }

    private GroupBuyActivity editableActivity(GroupBuyActivity input) {
        GroupBuyActivity a = new GroupBuyActivity();
        a.setId(input.getId()); a.setProductId(input.getProductId()); a.setActivityName(input.getActivityName().strip());
        a.setActivityDesc(input.getActivityDesc()); a.setGroupPrice(input.getGroupPrice()); a.setRequiredMembers(input.getRequiredMembers());
        a.setValidityHours(input.getValidityHours()); a.setStartTime(input.getStartTime()); a.setEndTime(input.getEndTime());
        return a;
    }

    private boolean hasActivityHistory(Long activityId) {
        return teamMapper.selectCount(new QueryWrapper<GroupBuyTeam>().eq("activity_id", activityId)) > 0
            || orderMapper.selectCount(new QueryWrapper<Order>().eq("activity_id", activityId)) > 0;
    }

    private boolean tradingRulesChanged(GroupBuyActivity a, GroupBuyActivity b) {
        return !Objects.equals(a.getProductId(), b.getProductId()) || a.getGroupPrice() == null || b.getGroupPrice() == null
            || a.getGroupPrice().compareTo(b.getGroupPrice()) != 0 || !Objects.equals(a.getRequiredMembers(), b.getRequiredMembers())
            || !Objects.equals(a.getValidityHours(), b.getValidityHours()) || !Objects.equals(a.getStartTime(), b.getStartTime()) || !Objects.equals(a.getEndTime(), b.getEndTime());
    }

    private void fillActivityProducts(List<GroupBuyActivity> rows) {
        Set<Long> ids = rows.stream().map(GroupBuyActivity::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> products = ids.isEmpty() ? Map.of() : productMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Product::getId, p -> p));
        for (GroupBuyActivity row : rows) {
            Product product = products.get(row.getProductId()); row.setProduct(product);
            if (product != null) row.setStock(product.getStock());
        }
    }

    private void fillTeamPeople(List<GroupBuyTeam> teams) {
        if (teams.isEmpty()) return;
        Set<Long> teamIds = teams.stream().map(GroupBuyTeam::getId).collect(Collectors.toSet());
        List<GroupBuyMember> members = memberMapper.selectList(new QueryWrapper<GroupBuyMember>().in("team_id", teamIds).eq("status", 1).orderByAsc("id"));
        Set<Long> userIds = new java.util.HashSet<>();
        teams.forEach(t -> userIds.add(t.getLeaderUserId())); members.forEach(m -> userIds.add(m.getUserId())); userIds.remove(null);
        Map<Long, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, List<GroupBuyMember>> grouped = members.stream().collect(Collectors.groupingBy(GroupBuyMember::getTeamId));
        for (GroupBuyMember member : members) member.setUser(users.get(member.getUserId()));
        for (GroupBuyTeam team : teams) { team.setLeader(users.get(team.getLeaderUserId())); team.setMembers(grouped.getOrDefault(team.getId(), List.of())); }
    }
}
