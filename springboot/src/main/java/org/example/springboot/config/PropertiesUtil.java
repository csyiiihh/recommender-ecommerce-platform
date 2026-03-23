package org.example.springboot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件工具类
 */
public class PropertiesUtil {
    
    private static final Properties PROPS = new Properties();
    
    static {
        try {
            InputStream in = PropertiesUtil.class.getClassLoader().getResourceAsStream("config.properties");
            if (in != null) {
                PROPS.load(in);
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getDomain() {
        return PROPS.getProperty("domain", "127.0.0.1:1234");
    }
    
    public static String getCallback() {
        return PROPS.getProperty("callback", "localhost:8080");
    }
    
    public static String getRedisHost() {
        return PROPS.getProperty("redisHost", "localhost");
    }
    
    public static String getRedisPwd() {
        return PROPS.getProperty("redisPwd", "");
    }
}

