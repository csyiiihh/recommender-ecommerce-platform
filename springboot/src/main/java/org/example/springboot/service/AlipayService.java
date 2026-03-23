package org.example.springboot.service;

import com.alipay.api.AlipayApiException;

/**
 * 支付宝支付服务接口
 */
public interface AlipayService {
    
    /**
     * 创建支付订单
     * @param orderId   订单编号
     * @param orderName 订单名称
     * @param payPrice  支付金额
     * @return 支付表单HTML
     */
    String create(String orderId, String orderName, String payPrice);
    
    /**
     * 订单退款
     * @param orderId  订单编号
     * @param payPrice 退款金额
     */
    void refund(String orderId, String payPrice) throws AlipayApiException;
}

