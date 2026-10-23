package org.example.springboot.service.email;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class VerificationCodeService {
    private record CodeInfo(String code, long expires, long sent, int attempts) {}
    private final Map<String, CodeInfo> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    public String generateCode() { return String.format("%06d", random.nextInt(1000000)); }
    public synchronized void storeCode(String purpose, String email, String code, int minutes) {
        String key = purpose + ":" + email;
        long now = System.currentTimeMillis();
        CodeInfo old = codes.get(key);
        if(old != null && now - old.sent() < 60000) throw new IllegalArgumentException("请60秒后重试");
        if (codes.size() >= 10000 && old == null) { cleanExpiredCodes(); if(codes.size() >= 10000) throw new IllegalArgumentException("请求过多，请稍后重试"); }
        codes.put(key, new CodeInfo(code, now + minutes * 60000L, now, 0));
    }
    public synchronized boolean verifyCode(String purpose, String email, String input) {
        String key = purpose + ":" + email;
        CodeInfo info = codes.get(key);
        if(info == null) return false;
        if(info.expires() < System.currentTimeMillis() || info.attempts() >= 5) { codes.remove(key); return false; }
        if(info.code().equals(input)) { codes.remove(key); return true; }
        codes.put(key, new CodeInfo(info.code(), info.expires(), info.sent(), info.attempts()+1));
        return false;
    }
    @Scheduled(fixedDelay = 60000)
    public synchronized void cleanExpiredCodes() { long now = System.currentTimeMillis(); codes.entrySet().removeIf(e -> e.getValue().expires() < now); }
}
