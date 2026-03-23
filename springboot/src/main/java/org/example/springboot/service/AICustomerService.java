package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.springboot.entity.CustomerServiceMessage;
import org.example.springboot.mapper.CustomerServiceMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI客服服务类
 * 支持多种大模型：OpenAI、百度文心、阿里通义、腾讯混元等
 */
@Service
public class AICustomerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AICustomerService.class);
    
    @Autowired
    private CustomerServiceMessageMapper messageMapper;
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private CustomerServiceService customerServiceService;
    
    // AI配置项
    @Value("${ai.enabled:false}")
    private boolean aiEnabled;
    
    @Value("${ai.provider:openai}")
    private String aiProvider; // openai, baidu, alibaba, tencent
    
    @Value("${ai.api.key:}")
    private String apiKey;
    
    @Value("${ai.api.url:}")
    private String apiUrl;
    
    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${ai.service.id:0}")
    private Long aiServiceId; // AI客服的系统ID，用于标识AI回复
    
    /**
     * 生成AI客服回复
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param conversationHistory 对话历史
     * @return AI回复内容
     */
    public String generateReply(Long sessionId, String userMessage, List<CustomerServiceMessage> conversationHistory) {
        if (!aiEnabled) {
            LOGGER.warn("AI客服未启用");
            return null;
        }
        
        if (StrUtil.isBlank(apiKey)) {
            LOGGER.error("AI API Key未配置");
            return null;
        }
        
        try {
            String reply = null;
            switch (aiProvider.toLowerCase()) {
                case "openai":
                    reply = callOpenAI(userMessage, conversationHistory);
                    break;
                case "baidu":
                    reply = callBaiduWenxin(userMessage, conversationHistory);
                    break;
                case "alibaba":
                    reply = callAlibabaTongyi(userMessage, conversationHistory);
                    break;
                case "tencent":
                    reply = callTencentHunyuan(userMessage, conversationHistory);
                    break;
                default:
                    LOGGER.error("不支持的AI提供商: {}", aiProvider);
                    return null;
            }
            
            return reply;
        } catch (Exception e) {
            LOGGER.error("生成AI回复失败", e);
            return "抱歉，我现在无法回答您的问题，请稍后再试或联系人工客服。";
        }
    }
    
    /**
     * 调用OpenAI API
     */
    private String callOpenAI(String userMessage, List<CustomerServiceMessage> conversationHistory) {
        String url = StrUtil.isNotBlank(apiUrl) ? apiUrl : "https://api.openai.com/v1/chat/completions";
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        
        // 构建消息列表
        JSONArray messages = new JSONArray();
        
        // 添加系统提示词
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", buildSystemPrompt());
        messages.add(systemMsg);
        
        // 添加对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (CustomerServiceMessage msg : conversationHistory) {
                JSONObject msgObj = new JSONObject();
                msgObj.set("role", "USER".equals(msg.getSenderType()) ? "user" : "assistant");
                msgObj.set("content", msg.getContent());
                messages.add(msgObj);
            }
        }
        
        // 添加当前用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);
        
        requestBody.set("messages", messages);
        requestBody.set("temperature", 0.7);
        requestBody.set("max_tokens", 1000);
        
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute();
        
        if (response.getStatus() == 200) {
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray choices = result.getJSONArray("choices");
            if (choices != null && choices.size() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getStr("content");
            }
        } else {
            LOGGER.error("OpenAI API调用失败: {}", response.body());
        }
        
        return null;
    }
    
    /**
     * 调用百度文心一言API
     */
    private String callBaiduWenxin(String userMessage, List<CustomerServiceMessage> conversationHistory) {
        String url = StrUtil.isNotBlank(apiUrl) ? apiUrl : "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
        
        // 百度API需要先获取access_token
        String accessToken = getBaiduAccessToken();
        if (StrUtil.isBlank(accessToken)) {
            return null;
        }
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        
        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (CustomerServiceMessage msg : conversationHistory) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", "USER".equals(msg.getSenderType()) ? "user" : "assistant");
                msgMap.put("content", msg.getContent());
                messages.add(msgMap);
            }
        }
        
        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        requestBody.set("messages", messages);
        requestBody.set("temperature", 0.7);
        
        HttpResponse response = HttpRequest.post(url + "?access_token=" + accessToken)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute();
        
        if (response.getStatus() == 200) {
            JSONObject result = JSONUtil.parseObj(response.body());
            if (result.containsKey("result")) {
                return result.getStr("result");
            }
        } else {
            LOGGER.error("百度文心API调用失败: {}", response.body());
        }
        
        return null;
    }
    
    /**
     * 获取百度access_token
     */
    private String getBaiduAccessToken() {
        // 这里需要根据百度API文档实现获取access_token的逻辑
        // 通常需要API Key和Secret Key
        // String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
        // 实现获取token的逻辑
        // 注意：实际使用时需要：
        // 1. 从配置中读取API Key和Secret Key
        // 2. 调用百度OAuth接口获取access_token
        // 3. 缓存token（token有效期通常为30天）
        return apiKey; // 简化处理，实际应该调用token接口
    }
    
    /**
     * 调用阿里通义千问API
     */
    private String callAlibabaTongyi(String userMessage, List<CustomerServiceMessage> conversationHistory) {
        String url = StrUtil.isNotBlank(apiUrl) ? apiUrl : "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        
        // 构建输入
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        
        // 添加系统提示词
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", buildSystemPrompt());
        messages.add(systemMsg);
        
        // 添加对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (CustomerServiceMessage msg : conversationHistory) {
                JSONObject msgObj = new JSONObject();
                msgObj.set("role", "USER".equals(msg.getSenderType()) ? "user" : "assistant");
                msgObj.set("content", msg.getContent());
                messages.add(msgObj);
            }
        }
        
        // 添加当前用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);
        
        input.set("messages", messages);
        requestBody.set("input", input);
        requestBody.set("parameters", JSONUtil.createObj()
                .set("temperature", 0.7)
                .set("max_tokens", 1000));
        
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute();
        
        if (response.getStatus() == 200) {
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject output = result.getJSONObject("output");
            if (output != null) {
                return output.getStr("text");
            }
        } else {
            LOGGER.error("阿里通义API调用失败: {}", response.body());
        }
        
        return null;
    }
    
    /**
     * 调用腾讯混元API
     */
    private String callTencentHunyuan(String userMessage, List<CustomerServiceMessage> conversationHistory) {
        String url = StrUtil.isNotBlank(apiUrl) ? apiUrl : "https://hunyuan.tencentcloudapi.com/";
        
        // 腾讯云API需要签名，这里简化处理
        // 实际使用时需要按照腾讯云API文档实现签名逻辑
        JSONObject requestBody = new JSONObject();
        requestBody.set("Model", model);
        
        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            for (CustomerServiceMessage msg : conversationHistory) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("Role", "USER".equals(msg.getSenderType()) ? "user" : "assistant");
                msgMap.put("Content", msg.getContent());
                messages.add(msgMap);
            }
        }
        
        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("Role", "user");
        userMsg.put("Content", userMessage);
        messages.add(userMsg);
        
        requestBody.set("Messages", messages);
        requestBody.set("Temperature", 0.7);
        requestBody.set("MaxTokens", 1000);
        
        // 这里需要添加腾讯云的签名逻辑
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute();
        
        if (response.getStatus() == 200) {
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject responseObj = result.getJSONObject("Response");
            if (responseObj != null) {
                return responseObj.getStr("Content");
            }
        } else {
            LOGGER.error("腾讯混元API调用失败: {}", response.body());
        }
        
        return null;
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return "你是一个专业的在线图书销售平台的客服助手。你的职责是：\n" +
                "1. 友好、耐心地回答用户关于图书、订单、物流、退换货等问题\n" +
                "2. 提供准确的产品信息和购买建议\n" +
                "3. 帮助用户解决订单相关问题\n" +
                "4. 如果遇到无法解决的问题，建议用户联系人工客服\n" +
                "5. 回复要简洁明了，使用中文，语气要友好专业\n" +
                "6. 不要编造不存在的产品或信息";
    }
    
    /**
     * 异步发送AI回复
     */
    @Async
    public void sendAIReplyAsync(Long sessionId, Long userId, String userMessage) {
        try {
            // 获取对话历史（最近20条）
            List<CustomerServiceMessage> history = messageMapper.selectRecentMessages(sessionId, 20);
            // 反转列表，使其按时间正序排列
            if (history != null && !history.isEmpty()) {
                List<CustomerServiceMessage> reversedHistory = new ArrayList<>();
                for (int i = history.size() - 1; i >= 0; i--) {
                    reversedHistory.add(history.get(i));
                }
                history = reversedHistory;
            }
            
            // 生成AI回复
            String aiReply = generateReply(sessionId, userMessage, history);
            
            if (StrUtil.isNotBlank(aiReply)) {
                // 使用AI客服ID发送回复
                Long serviceId = aiServiceId > 0 ? aiServiceId : 1L; // 默认使用ID为1的系统账号
                customerServiceService.sendMessage(sessionId, serviceId, "SERVICE", aiReply, "TEXT");
                LOGGER.info("AI客服自动回复成功，会话ID: {}, 回复: {}", sessionId, aiReply);
            }
        } catch (Exception e) {
            LOGGER.error("AI客服自动回复失败", e);
        }
    }
}

