package org.example.springboot.config;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置类
 */
@Data
@Component
public class AlipayConfig {

    /**
     * 沙箱appId
     */
    public static final String APPID = "9021000158690721";

    /**
     * 请求网关 - 沙箱环境
     */
    public static final String URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /**
     * 编码格式
     */
    public static final String CHARSET = "UTF-8";

    /**
     * 返回格式
     */
    public static final String FORMAT = "json";

    /**
     * 签名方式
     */
    public static final String SIGNTYPE = "RSA2";

    /**
     * 异步通知地址
     */
    public static final String NOTIFY_URL = "http://" + PropertiesUtil.getDomain() + "/alipay/notify";

    /**
     * 同步地址
     */
    public static final String RETURN_URL = "http://" + PropertiesUtil.getDomain() + "/alipay/success";

    /**
     * 应用私钥 (pkcs8格式)
     * 注意：这是示例密钥，实际使用时需要替换为支付宝开放平台获取的应用私钥
     */
    public static final String RSA_PRIVATE_KEY = "REDACTED_FOR_GITHUB_MIGRATION";

    /**
     * 支付宝公钥
     * 注意：这是示例密钥，实际使用时需要替换为支付宝开放平台获取的支付宝公钥
     */
    public static final String ALIPAY_PUBLIC_KEY = "REDACTED_FOR_GITHUB_MIGRATION";
}
