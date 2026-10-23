package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Cart;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.Logistics;
import org.example.springboot.entity.GroupBuyMember;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired private AccessService access;
    @Autowired private FulfillmentPolicy fulfillment;
    @Autowired private InventoryReservationService inventory;
    @Autowired private AlipayService alipay;
    @Autowired private AutomaticRefundService automaticRefunds;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private AddressMapper addressMapper;
    
    @Autowired
    private CartMapper cartMapper;
    
    @Autowired
    private GroupBuyMemberMapper groupBuyMemberMapper;
    
    @Autowired
    private GroupBuyTeamMapper groupBuyTeamMapper;
    
    @Autowired
    private GroupBuyActivityMapper groupBuyActivityMapper;
    
    @Autowired
    private GroupBuyService groupBuyService;



    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Result<?> createOrder(Order order) {
        access.owner(order.getUserId());
        if(order.getQuantity() == null || order.getQuantity() < 1) throw new BusinessException("数量必须大于0");
        Long buyerId = access.current().getId();
        inventory.lockBuyer(buyerId);
        Product product = productMapper.selectById(order.getProductId());
        if(product == null || !Integer.valueOf(1).equals(product.getStatus())) throw new BusinessException("商品不存在或已下架");
        if(product.getStock() < order.getQuantity()) return ResultUtils.stockInsufficient();
        BigDecimal price = Integer.valueOf(1).equals(product.getIsDiscount()) && product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
        if(price == null || price.signum() <= 0) throw new BusinessException("商品价格无效");
        Order safe = new Order(); safe.setUserId(buyerId); safe.setProductId(product.getId());
        safe.setQuantity(order.getQuantity()); safe.setPrice(price); safe.setTotalPrice(price.multiply(BigDecimal.valueOf(order.getQuantity())));
        safe.setStatus(0); safe.setLastStatus(0); safe.setOrderType(0); safe.setRefundStatus(0);
        safe.setRecvName(order.getRecvName()); safe.setRecvAddress(order.getRecvAddress()); safe.setRecvPhone(order.getRecvPhone()); safe.setRemark(order.getRemark());
        safe.setOutTradeNo(UUID.randomUUID().toString().replace("-", ""));
        if(orderMapper.insert(safe) != 1) throw new BusinessException("创建订单失败");
        inventory.reserve(safe);
        return ResultUtils.success(safe);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateOrderStatus(Long id, Integer status) {
        access.order(id, true);
        Order order = orderMapper.selectForUpdate(id);
        if(order == null) return ResultUtils.dataNotFound("订单不存在");
        int old = order.getStatus();
        int fulfillment = old == 7 && order.getLastStatus() != null ? order.getLastStatus() : old;
        boolean cancel = old == 0 && Integer.valueOf(4).equals(status);
        boolean ship = fulfillment == 1 && Integer.valueOf(2).equals(status) && (access.isAdmin() || access.isMerchant());
        boolean complete = fulfillment == 2 && Integer.valueOf(3).equals(status) && (access.isAdmin() || order.getUserId().equals(access.current().getId()));
        if (ship) { access.sellerOrder(id); this.fulfillment.requireShipping(order); }
        if (complete) this.fulfillment.requireReceipt(order);
        if(!cancel && !ship && !complete) throw new BusinessException("不允许的订单状态变更");
        if (cancel) inventory.release(order);
        order.setLastStatus(old); order.setStatus(status);
        if(orderMapper.updateById(order) != 1) throw new BusinessException("更新订单失败");
        if(complete) {
            Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId, id));
            if(logistics != null) { logistics.setStatus(2); logisticsMapper.updateById(logistics); }
        }
        return ResultUtils.success(order);
    }

    public Result<?> deleteOrder(Long id) {
        access.order(id, false);
        // Keep transaction history and callback idempotency records. Cancellation is a separate operation.
        return Result.error("400", "订单属于交易历史，请使用取消订单，暂不支持物理删除");
    }

    public void deleteRelation(Long id){
        // 删除物流记录
        logisticsMapper.delete(new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId,id));
        
        // 检查是否是拼团订单
        Order order = orderMapper.selectById(id);
        if (order != null && order.getOrderType() != null && order.getOrderType() == 1) {
            // 查询该订单对应的成员记录
            LambdaQueryWrapper<GroupBuyMember> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(GroupBuyMember::getOrderId, id);
            GroupBuyMember member = groupBuyMemberMapper.selectOne(memberQuery);
            
            if (member != null) {
                Long teamId = member.getTeamId();
                Integer isLeader = member.getIsLeader();
                
                // 删除成员记录
                groupBuyMemberMapper.delete(memberQuery);
                
                if (isLeader != null && isLeader == 1) {
                    // 如果是团长订单，删除整个团和所有成员
                    LOGGER.info("删除团长订单，同时删除整个拼团，团ID：{}", teamId);
                    
                    // 删除该团的所有成员记录
                    groupBuyMemberMapper.delete(
                        new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getTeamId, teamId)
                    );
                    
                    // 删除团记录
                    groupBuyTeamMapper.deleteById(teamId);
                    
                } else {
                    // 如果是团员订单，更新团的人数
                    GroupBuyTeam team = groupBuyTeamMapper.selectById(teamId);
                    if (team != null && team.getCurrentPeople() > 0) {
                        team.setCurrentPeople(team.getCurrentPeople() - 1);
                        groupBuyTeamMapper.updateById(team);
                        LOGGER.info("删除团员订单，更新团人数，团ID：{}，剩余人数：{}", teamId, team.getCurrentPeople());
                    }
                }
            }
        }
        
        LOGGER.info("删除订单关联数据成功，订单ID：{}", id);
    }

    public Result<?> getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            LOGGER.warn("查询订单失败，订单ID：{} 不存在", id);
            return ResultUtils.dataNotFound("订单不存在");
        }
        
        // 填充关联信息
        order.setUser(userMapper.selectById(order.getUserId()));
        order.setProduct(productMapper.selectById(order.getProductId()));
        return ResultUtils.success(order);
    }

    public Result<?> getOrdersByUserId(Long userId) {
        try {
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getUserId, userId);
            queryWrapper.orderByDesc(Order::getCreatedAt);
            List<Order> orders = orderMapper.selectList(queryWrapper);
            
            if (orders != null && !orders.isEmpty()) {
                // 填充关联信息
                fillRelations(orders);
                LOGGER.debug("查询用户订单，用户ID：{}，订单数：{}", userId, orders.size());
                return ResultUtils.success(orders);
            }
            LOGGER.info("查询用户订单，用户ID：{}，结果为空", userId);
            return ResultUtils.success(orders); // 返回空列表而不是错误
        } catch (Exception e) {
            LOGGER.error("查询用户订单异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询订单列表失败");
        }
    }

    /**
     * 获取订单状态
     */
    public Result<Integer> getOrderStatus(Long id) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("获取订单状态失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }
            return ResultUtils.success(order.getStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("获取订单状态异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取订单状态失败：" + e.getMessage());
        }
    }

    public Result<?> getOrdersByPage(Long userId,Long id,String status, Long merchantId,Integer currentPage, Integer size) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        if (id != null) {
            queryWrapper.eq(Order::getId, id);
        }
        if(StringUtils.isNotBlank(status)){
            queryWrapper.eq(Order::getStatus,status);
        }
        if (merchantId != null) {
            List<Product> product = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));

            if(!product.isEmpty()){
            List<Long> productIds = product.stream().map(Product::getId).collect(Collectors.toList());
            queryWrapper.in(Order::getProductId, productIds);
            }else{
                Page<Order> page = new Page<>(currentPage, size);
                page.setTotal(0);
                page.setRecords(null);
                return ResultUtils.success(page);
            }
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(currentPage, size);
        Page<Order> result = orderMapper.selectPage(page, queryWrapper);

        // 填充关联信息
        fillRelations(result.getRecords());

        LOGGER.debug("分页查询订单，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
        return ResultUtils.success(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<?> refundOrder(Long id, String reason) {
        try {
            Order order = orderMapper.selectForUpdate(id);
            if (order == null) {
                LOGGER.warn("申请退款失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单状态是否允许退款
            if (order.getStatus() != 1 && order.getStatus() != 2) {
                LOGGER.warn("申请退款失败，订单ID：{}，当前状态：{} 不允许退款", id, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许退款");
            }

            order.setLastStatus(order.getStatus());  // 保存当前状态
            order.setStatus(5);  // 设为退款中
            order.setRefundStatus(1); // 申请退款
            order.setRefundReason(reason);
            int result = orderMapper.updateById(order);
            if (result > 0) {
                LOGGER.info("申请退款成功，订单ID：{}，退款原因：{}", id, reason);
                return ResultUtils.success(order);
            }
            LOGGER.warn("申请退款失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "申请退款失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("申请退款异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "申请退款失败：" + e.getMessage());
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        return Result.error("400", "交易历史不支持批量物理删除");
    }
    @Transactional
    public Result<?> payOrder(Long id) {
        return ResultUtils.error(ResultCode.PAYMENT_FAILED, "该支付入口已停用，请使用支付宝完成真实支付");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateOrderAddress(String name, Long id, String address, String phone) {
        access.order(id, false);
        if (name == null || name.isBlank() || address == null || address.isBlank() || phone == null || phone.isBlank()) throw new BusinessException("收货信息不能为空");
        Order order = orderMapper.selectForUpdate(id);
        if (order == null) return ResultUtils.dataNotFound("订单不存在");
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(1).equals(order.getStatus())) return ResultUtils.businessError("当前状态不允许修改收货信息");
        if (orderMapper.updateDeliveryIfUnshipped(id, name.strip(), address.strip(), phone.strip()) != 1) return ResultUtils.businessError("订单状态已变化，请刷新重试");
        order.setRecvName(name.strip()); order.setRecvAddress(address.strip()); order.setRecvPhone(phone.strip());
        return ResultUtils.success(order);
    }
    public Result<?> updateOrder(Long id, Order order) {
        return Result.error("400", "请使用订单地址、状态或退款专用接口");
    }

    public Result<?> getOrderLogistics(Long orderId) {
        try {
            // 检查订单是否存在
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                LOGGER.warn("查询订单物流信息失败，订单ID：{} 不存在", orderId);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 查询物流信息
            LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Logistics::getOrderId, orderId);
            Logistics logistics = logisticsMapper.selectOne(queryWrapper);
            
            if (logistics != null) {
                // 填充关联信息
                logistics.setOrder(order);
                return ResultUtils.success(logistics);
            }
            LOGGER.info("查询订单物流信息，订单ID：{}，物流信息不存在", orderId);
            return ResultUtils.dataNotFound("未找到物流信息");
        } catch (Exception e) {
            LOGGER.error("查询订单物流信息异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询订单物流信息失败：" + e.getMessage());
        }
    }

    /**
     * 处理退款申请
     * @param id 订单ID
     * @param status 退款状态：6-同意退款 7-拒绝退款
     * @param remark 处理备注
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> handleRefund(Long id, Integer status, String remark) {
        if(!Integer.valueOf(6).equals(status) && !Integer.valueOf(7).equals(status)) throw new BusinessException("无效退款处理状态");
        try {
            Order order = orderMapper.selectForUpdate(id);
            if (order == null) {
                LOGGER.warn("处理退款失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单是否处于退款中状态
            if (order.getStatus() != 5) {
                LOGGER.warn("处理退款失败，订单ID：{}，当前状态：{} 不是退款中", id, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不是退款中");
            }

            boolean compensationWithoutStock = AutomaticRefundService.isAutomatic(order) && AutomaticRefundService.UNFULFILLABLE.equals(order.getRefundReason());
            if (AutomaticRefundService.isAutomatic(order) && status == 7) throw new BusinessException("系统补偿退款不能拒绝，请核对支付记录");
            if(status == 6) {
                if(!"alipay".equals(order.getPayMethod()) || order.getTradeNo() == null) throw new BusinessException("无真实支付记录，请人工核对历史订单");
                alipay.refund(order.getOutTradeNo(), order.getTotalPrice().toPlainString(), "REFUND_ORDER_" + order.getId());
            }
            // 保存原始状态
            // Keep the fulfillment state saved when the refund was requested.
            // 更新状态
            order.setStatus(status);
            order.setRefundStatus(status == 6 ? 3 : 4); // 3-已退款 4-退款失败
            order.setRefundTime(Timestamp.valueOf(LocalDateTime.now()));
            order.setRemark(remark);
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                // 如果同意退款，恢复商品库存
                if (status == 6 && !compensationWithoutStock) {
                    Product product = productMapper.selectForUpdate(order.getProductId());
                    if (product != null) {
                        // 增加库存
                        // Shipped goods are not available stock until physically returned and entered in the stock ledger.
                        if (Integer.valueOf(1).equals(order.getLastStatus())) product.setStock(product.getStock() + order.getQuantity());
                        
                        // ⭐ 对于拼团订单，销量的处理逻辑不同
                        if (order.getOrderType() != null && order.getOrderType() == 1) {
                            // 拼团订单：先不处理销量，交给 handleGroupBuyRefund 处理
                            LOGGER.info("拼团订单退款，商品库存已恢复，销量将在拼团逻辑中处理");
                        } else {
                            // 普通订单：直接减少销量
                            if (product.getSalesCount() >= order.getQuantity()) {
                                product.setSalesCount(product.getSalesCount() - order.getQuantity());
                            }
                        }
                        
                        productMapper.updateById(product);
                        LOGGER.info("退款成功，已恢复商品库存，商品ID：{}，数量：{}", product.getId(), order.getQuantity());
                    }
                    
                    // ⭐ 如果是拼团订单，需要处理拼团相关数据
                    if (order.getOrderType() != null && order.getOrderType() == 1) {
                        handleGroupBuyRefund(order);
                    }
                }

                // 同步更新物流状态
                LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Logistics::getOrderId, id);
                Logistics logistics = logisticsMapper.selectOne(queryWrapper);
                if (logistics != null && status == 6) { // 如果同意退款
                    logistics.setStatus(3); // 设置物流状态为已取消
                    logisticsMapper.updateById(logistics);
                    LOGGER.info("订单退款成功，同步更新物流状态为已取消，物流ID：{}", logistics.getId());
                }
                
                LOGGER.info("处理退款成功，订单ID：{}，处理结果：{}", id, status == 6 ? "已退款" : "拒绝退款");
                return ResultUtils.success(order);
            }
            LOGGER.warn("处理退款失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "处理退款失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("处理退款异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "处理退款失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理拼团订单退款的特殊逻辑
     * @param order 拼团订单
     */
    private void handleGroupBuyRefund(Order order) {
        GroupBuyMember member = groupBuyMemberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getOrderId, order.getId()));
        if (member == null) throw new BusinessException("拼团成员记录缺失，请核对");
        GroupBuyActivity activity = groupBuyActivityMapper.selectForUpdate(order.getActivityId());
        GroupBuyTeam team = groupBuyTeamMapper.selectForUpdate(member.getTeamId());
        if (activity == null || team == null) throw new BusinessException("拼团记录缺失，请核对");
        boolean completed = Integer.valueOf(1).equals(team.getStatus());
        member.setStatus(2);
        member.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        groupBuyMemberMapper.updateById(member);
        // Completed groups remain completed: post-sale refunds affect only this order.
        if (!completed && Integer.valueOf(0).equals(team.getStatus())) {
            if (Integer.valueOf(1).equals(member.getIsLeader())) {
                team.setStatus(2);
                team.setFailReason("leader_refund");
                automaticRefunds.queueTeam(team.getId());
            }
            team.setCurrentPeople(Math.max(0, team.getCurrentPeople() - 1));
            team.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            groupBuyTeamMapper.updateById(team);
        }
        if (Integer.valueOf(1).equals(order.getLastStatus())) activity.setStock(activity.getStock() + order.getQuantity());
        if (completed) {
            activity.setSalesCount(Math.max(0, activity.getSalesCount() - order.getQuantity()));
            Product product = productMapper.selectForUpdate(order.getProductId());
            if (product != null) {
                product.setSalesCount(Math.max(0, product.getSalesCount() - order.getQuantity()));
                productMapper.updateById(product);
            }
        }
        groupBuyActivityMapper.updateById(activity);
    }

    /**
     * 从购物车创建订单（支付宝支付）
     * @param params 包含购物车ID列表、收货信息等
     * @return 返回商户订单号用于支付
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Result<?> createOrdersFromCart(Map<String, Object> params) {
        try {
            // 解析参数
            @SuppressWarnings("unchecked")
            List<Number> cartIds = (List<Number>) params.get("cartIds");
            Long userId = Long.parseLong(params.get("userId").toString());
            String recvName = (String) params.get("recvName");
            String recvAddress = (String) params.get("recvAddress");
            String recvPhone = (String) params.get("recvPhone");
            
            if (cartIds == null || cartIds.isEmpty()) {
                LOGGER.warn("从购物车创建订单失败，购物车ID列表为空");
                return ResultUtils.paramError("购物车为空");
            }
            
            access.owner(userId);
            inventory.lockBuyer(userId);

            // 生成商户订单号（时间戳 + 随机数）
            String outTradeNo = UUID.randomUUID().toString().replace("-", "");
            
            int orderCount = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            // 遍历购物车项创建订单
            for (Number cartId : cartIds.stream().distinct().toList()) {
                Cart cart = cartMapper.selectById(cartId.longValue());
                if (cart == null || !userId.equals(cart.getUserId()) || cart.getQuantity() == null || cart.getQuantity() < 1) throw new BusinessException("购物车记录无效");
                
                // 检查商品和库存
                Product product = productMapper.selectById(cart.getProductId());
                if (product == null) {
                    LOGGER.warn("从购物车创建订单失败，商品ID：{} 不存在", cart.getProductId());
                    throw new BusinessException("商品不存在");
                }
                if (!Integer.valueOf(1).equals(product.getStatus()) || product.getStock() < cart.getQuantity()) {
                    LOGGER.warn("从购物车创建订单失败，商品ID：{}，库存不足，需要：{}，库存：{}", 
                        cart.getProductId(), cart.getQuantity(), product.getStock());
                    throw new BusinessException("商品库存不足");
                }
                
                // 创建订单
                Order order = new Order();
                order.setUserId(userId);
                order.setProductId(cart.getProductId());
                order.setQuantity(cart.getQuantity());
                
                // 判断是否有折扣
                BigDecimal unitPrice;
                if (product.getIsDiscount() != null && product.getIsDiscount() == 1 
                    && product.getDiscountPrice() != null) {
                    unitPrice = product.getDiscountPrice();
                } else {
                    unitPrice = product.getPrice();
                }
                
                if(unitPrice == null || unitPrice.signum() <= 0) throw new BusinessException("商品价格无效");
                order.setOrderType(0);
                order.setPrice(unitPrice);
                order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
                order.setStatus(0);  // 待支付
                order.setLastStatus(0);
                order.setRecvName(recvName);
                order.setRecvAddress(recvAddress);
                order.setRecvPhone(recvPhone);
                order.setPayMethod("alipay");
                order.setOutTradeNo(outTradeNo);  // 所有订单使用同一个outTradeNo
                order.setRefundStatus(0);
                
                if (orderMapper.insert(order) != 1) throw new BusinessException("创建订单失败");
                inventory.reserve(order);
                
                orderCount++;
                totalAmount = totalAmount.add(order.getTotalPrice());
            }
            
            LOGGER.info("创建购物车订单成功，商户订单号：{}，订单数量：{}，总金额：{}", 
                    outTradeNo, orderCount, totalAmount);
            
            // 返回商户订单号和总金额
            Map<String, Object> result = new HashMap<>();
            result.put("outTradeNo", outTradeNo);
            result.put("totalAmount", totalAmount);
            result.put("orderCount", orderCount);
            
            return ResultUtils.success(result);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建购物车订单异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 准备单个订单支付（支付宝支付）
     * @param orderId 订单ID
     * @return 返回商户订单号和支付金额
     */
    @Transactional
    public Result<?> prepareOrderPay(Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                LOGGER.warn("准备订单支付失败，订单ID：{} 不存在", orderId);
                return ResultUtils.dataNotFound("订单不存在");
            }
            
            // 检查订单状态，只有待支付的订单才能支付
            if (order.getStatus() != 0) {
                LOGGER.warn("准备订单支付失败，订单ID：{}，当前状态：{} 不允许支付", orderId, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
            }
            
            // 生成商户订单号（如果还没有）
            String outTradeNo = order.getOutTradeNo();
            if (StringUtils.isBlank(outTradeNo)) {
                outTradeNo = UUID.randomUUID().toString().replace("-", "");
                order.setOutTradeNo(outTradeNo);
                orderMapper.updateById(order);
            }
            
            LOGGER.info("准备订单支付，订单ID：{}，商户订单号：{}，支付金额：{}", 
                    orderId, outTradeNo, order.getTotalPrice());
            
            // 返回商户订单号和支付金额
            Map<String, Object> result = new HashMap<>();
            result.put("outTradeNo", outTradeNo);
            result.put("totalAmount", order.getTotalPrice());
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("准备订单支付异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "准备支付失败：" + e.getMessage());
        }
    }
    
    /**
     * 余额支付（一键支付）- 直接完成支付，无需余额检查
     * 适用于普通订单
     * @param orderId 订单ID
     * @return 支付结果
     */
    @Transactional
    public Result<?> payByBalance(Long orderId) {
        return ResultUtils.error(ResultCode.PAYMENT_FAILED, "该支付入口已停用，请使用支付宝完成真实支付");
    }
    
    /**
     * 余额支付（一键支付）- 批量支付购物车订单
     * @param params 包含商户订单号的参数
     * @return 支付结果
     */
    @Transactional
    public Result<?> payCartByBalance(Map<String, Object> params) {
        return ResultUtils.error(ResultCode.PAYMENT_FAILED, "该支付入口已停用，请使用支付宝完成真实支付");
    }

    private void fillRelations(List<Order> list) {
        if(list == null || list.isEmpty()) return;
        Set<Long> productIds = list.stream().map(Order::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> products = productIds.isEmpty() ? Map.of() : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));
        Set<Long> userIds = list.stream().map(Order::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        products.values().stream().map(Product::getMerchantId).filter(Objects::nonNull).forEach(userIds::add);
        Map<Long, org.example.springboot.entity.User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(org.example.springboot.entity.User::getId, u -> u));
        for(Order order : list) { Product product=products.get(order.getProductId()); order.setProduct(product); order.setUser(users.get(order.getUserId())); order.setMerchant(product == null ? null : users.get(product.getMerchantId())); }
    }
}
