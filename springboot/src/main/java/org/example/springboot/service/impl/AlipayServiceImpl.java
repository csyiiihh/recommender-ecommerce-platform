package org.example.springboot.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import org.example.springboot.config.AlipayConfig;
import org.example.springboot.service.AlipayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 支付宝支付服务实现类
 */
@Service
public class AlipayServiceImpl implements AlipayService {
    
    private static final Logger log = LoggerFactory.getLogger(AlipayServiceImpl.class);
    private static final String PRODUCTCODE = "FAST_INSTANT_TRADE_PAY";
    
    @Override
    public String create(String orderId, String orderName, String payPrice) {
        try {
            // 创建支付宝客户端
            AlipayClient client = new DefaultAlipayClient(
                AlipayConfig.URL,
                AlipayConfig.APPID,
                AlipayConfig.RSA_PRIVATE_KEY,
                AlipayConfig.FORMAT,
                AlipayConfig.CHARSET,
                AlipayConfig.ALIPAY_PUBLIC_KEY,
                AlipayConfig.SIGNTYPE
            );
            
            // 创建支付请求
            AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            
            // 设置业务参数
            model.setOutTradeNo(orderId);      // 商户订单号
            model.setSubject(orderName);       // 订单标题
            model.setTotalAmount(payPrice);    // 付款金额
            model.setProductCode(PRODUCTCODE); // 产品码
            
            // 设置请求参数
            payRequest.setBizModel(model);
            payRequest.setReturnUrl(AlipayConfig.RETURN_URL);  // 同步回调
            payRequest.setNotifyUrl(AlipayConfig.NOTIFY_URL);  // 异步回调
            
            // 执行请求，返回支付页面HTML
            return client.pageExecute(payRequest).getBody();
        } catch (AlipayApiException e) {
            log.error("[支付宝] 支付失败", e);
            return null;
        }
    }
    
    @Override
    public void refund(String orderId, String payPrice) throws AlipayApiException {
        log.info("[支付宝退款] 开始处理，订单号: {}，退款金额: {}", orderId, payPrice);
        
        // 创建退款客户端
        AlipayClient client = new DefaultAlipayClient(
            AlipayConfig.URL,
            AlipayConfig.APPID,
            AlipayConfig.RSA_PRIVATE_KEY,
            AlipayConfig.FORMAT,
            AlipayConfig.CHARSET,
            AlipayConfig.ALIPAY_PUBLIC_KEY,
            AlipayConfig.SIGNTYPE
        );
        
        // 创建退款请求
        AlipayTradeRefundRequest payRequest = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        
        // 设置退款参数
        model.setOutTradeNo(orderId);        // 原订单号
        model.setRefundAmount(payPrice);     // 退款金额
        model.setRefundReason("用户申请退款");  // 退款原因
        
        payRequest.setBizModel(model);
        
        // 执行退款请求
        String response = client.execute(payRequest).getBody();
        log.info("[支付宝退款] 接口调用成功，订单号: {}，响应: {}", orderId, response);
        
        // 注意：沙箱环境的退款是模拟的，不会真实到账，但接口会返回成功
        // 生产环境切换后，退款会真实到账到用户支付宝账户
    }
}

