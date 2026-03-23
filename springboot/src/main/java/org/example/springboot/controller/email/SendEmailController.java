package org.example.springboot.controller.email;

import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.example.springboot.service.email.EmailService;
import org.example.springboot.service.email.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件发送控制器
 * 重构后使用Service层处理业务逻辑，符合分层架构原则
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class SendEmailController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailController.class);

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    @Resource
    private VerificationCodeService verificationCodeService;

    /**
     * 发送注册验证码
     */
    @GetMapping("/sendEmail/{email}")
    public Result<?> emailRegister(@PathVariable String email) {
        LOGGER.info("发送注册验证码请求：{}", email);

        // 检查邮箱是否已被注册
        if (userService.getByEmail(email) != null) {
            return Result.error("-1", "邮箱已被注册");
        }

        try {
            // 生成验证码
            String code = verificationCodeService.generateCode();
            
            // 存储验证码（有效期10分钟）
            verificationCodeService.storeCode(email, code, 10);
            
            // 发送邮件
            emailService.sendRegisterCode(email, code);
            
            LOGGER.info("注册验证码已发送至：{}", email);
            // 注意：实际生产环境不应该返回验证码，这里为了兼容前端暂时返回
            // 建议前端通过单独的验证接口验证码验证
    return Result.success(code);
        } catch (Exception e) {
            LOGGER.error("注册验证码发送失败：{}", e.getMessage(), e);
            return Result.error("-1", "验证码发送失败，请联系管理员");
        }
    }

    /**
     * 发送找回密码验证码
     */
    @GetMapping("/findByEmail/{email}")
    public Result<?> findByEmail(@PathVariable String email) {
        LOGGER.info("发送找回密码验证码请求：{}", email);

        // 检查邮箱是否存在
                User user = userService.getByEmail(email);
        if (user == null) {
            return Result.error("-1", "邮箱不存在");
        }

        try {
            // 生成验证码
            String code = verificationCodeService.generateCode();

            // 存储验证码（有效期10分钟）
            verificationCodeService.storeCode(email, code, 10);
            
            // 发送邮件
            emailService.sendResetPasswordCode(email, code);
            
            LOGGER.info("找回密码验证码已发送至：{}", email);
            // 注意：实际生产环境不应该返回验证码，这里为了兼容前端暂时返回
            return Result.success(code);
        } catch (Exception e) {
            LOGGER.error("找回密码验证码发送失败：{}", e.getMessage(), e);
            return Result.error("-1", "邮件发送失败，请联系管理员");
        }
    }
}
