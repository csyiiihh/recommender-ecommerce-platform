package org.example.springboot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AlipayConfig {
    public static final String CHARSET = "UTF-8";
    public static final String FORMAT = "json";
    public static final String SIGNTYPE = "RSA2";
    @Value("${ALIPAY_APP_ID:${alipay.app-id:}}") private String appId;
    @Value("${ALIPAY_PRIVATE_KEY:${alipay.private-key:}}") private String privateKey;
    @Value("${ALIPAY_PUBLIC_KEY:${alipay.public-key:}}") private String publicKey;
    @Value("${alipay.seller-id:}") private String sellerId;
    @Value("${alipay.gateway}") private String gateway;
    @Value("${alipay.notify-url}") private String notifyUrl;
    @Value("${alipay.return-url}") private String returnUrl;
}
