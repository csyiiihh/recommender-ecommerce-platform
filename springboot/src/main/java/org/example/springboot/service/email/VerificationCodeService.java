package org.example.springboot.service.email;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {
    // 存储验证码：key=邮箱, value=验证码
    private final Map<String, CodeInfo> codeStore = new ConcurrentHashMap<>();

    // 定时清理过期验证码的线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Random random = new Random();

    public VerificationCodeService() {
        // 每小时清理一次过期验证码
        scheduler.scheduleAtFixedRate(this::cleanExpiredCodes, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 生成验证码
     */
    public String generateCode() {
        // 生成6位随机数字
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 存储验证码
     */
    public void storeCode(String email, String code, int expireMinutes) {
        codeStore.put(email, new CodeInfo(code, System.currentTimeMillis() + expireMinutes * 60 * 1000));
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String inputCode) {
        CodeInfo codeInfo = codeStore.get(email);
        if (codeInfo == null) {
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() > codeInfo.expireTime) {
            codeStore.remove(email);
            return false;
        }

        // 验证验证码
        boolean isValid = codeInfo.code.equals(inputCode);
        if (isValid) {
            codeStore.remove(email); // 验证成功后删除
        }
        return isValid;
    }

    /**
     * 清理过期验证码
     */
    private void cleanExpiredCodes() {
        long now = System.currentTimeMillis();
        codeStore.entrySet().removeIf(entry -> now > entry.getValue().expireTime);
    }

    /**
     * 验证码信息内部类
     */
    private static class CodeInfo {
        String code;
        long expireTime;

        CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}