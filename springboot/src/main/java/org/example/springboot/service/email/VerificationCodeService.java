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
    private record SendWindow(long start, int count) {}
    private final Map<String, SendWindow> sendWindows = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    public String generateCode() { return String.format("%06d", random.nextInt(1000000)); }
    public synchronized void storeCode(String purpose, String email, String code, int minutes) {
        String key = purpose + ":" + email.strip().toLowerCase(java.util.Locale.ROOT);
        long now = System.currentTimeMillis();
        CodeInfo old = codes.get(key);
        if(old != null && now - old.sent() < 60000) throw new IllegalArgumentException("请60秒后重试");
        if (codes.size() >= 10000 && old == null) { cleanExpiredCodes(); if(codes.size() >= 10000) throw new IllegalArgumentException("请求过多，请稍后重试"); }
        codes.put(key, new CodeInfo(code, now + minutes * 60000L, now, 0));
    }
    public synchronized boolean verifyCode(String purpose, String email, String input) {
        String key = purpose + ":" + email.strip().toLowerCase(java.util.Locale.ROOT);
        CodeInfo info = codes.get(key);
        if(info == null) return false;
        if(info.expires() < System.currentTimeMillis() || info.attempts() >= 5) { codes.remove(key); return false; }
        if(info.code().equals(input)) { codes.remove(key); return true; }
        codes.put(key, new CodeInfo(info.code(), info.expires(), info.sent(), info.attempts()+1));
        return false;
    }
    /** Use the servlet peer address; forwarded headers require a separately trusted proxy configuration. */
    public synchronized void reserveSend(String peer) {
        long now = System.currentTimeMillis();
        sendWindows.entrySet().removeIf(e -> now - e.getValue().start() >= 60_000);
        SendWindow old = sendWindows.get(peer);
        if ((old != null && old.count() >= 5) || (old == null && sendWindows.size() >= 10_000)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "发送过于频繁，请稍后重试");
        }
        sendWindows.put(peer, new SendWindow(old == null ? now : old.start(), old == null ? 1 : old.count() + 1));
    }
    public synchronized void discardCode(String purpose, String email, String code) {
        String key = purpose + ":" + email.strip().toLowerCase(java.util.Locale.ROOT);
        codes.computeIfPresent(key, (k, value) -> value.code().equals(code) ? null : value);
    }
    @Scheduled(fixedDelay = 60000)
    public synchronized void cleanExpiredCodes() { long now = System.currentTimeMillis(); codes.entrySet().removeIf(e -> e.getValue().expires() < now); sendWindows.entrySet().removeIf(e -> now - e.getValue().start() >= 60_000); }
}
