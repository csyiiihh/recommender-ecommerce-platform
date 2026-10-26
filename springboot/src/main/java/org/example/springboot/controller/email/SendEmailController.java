package org.example.springboot.controller.email;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.springboot.common.Result;
import org.example.springboot.service.UserService;
import org.example.springboot.service.email.EmailService;
import org.example.springboot.service.email.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/email")
public class SendEmailController {
    private static final Logger LOG = LoggerFactory.getLogger(SendEmailController.class);
    @Resource private UserService userService;
    @Resource private EmailService emailService;
    @Resource private VerificationCodeService verificationCodeService;
    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

    @PostMapping("/sendEmail")
    public Result<?> emailRegister(@Valid @RequestBody EmailRequest input, HttpServletRequest request) {
        return send(input.email(), "register", request.getRemoteAddr());
    }
    @PostMapping("/findByEmail")
    public Result<?> findByEmail(@Valid @RequestBody EmailRequest input, HttpServletRequest request) {
        return send(input.email(), "reset", request.getRemoteAddr());
    }
    private Result<?> send(String input, String purpose, String peer) {
        String email = input.strip().toLowerCase(java.util.Locale.ROOT);
        verificationCodeService.reserveSend(peer);
        boolean exists = userService.getByEmail(email) != null;
        // The same public response avoids disclosing whether an address has an account.
        if (("register".equals(purpose) && exists) || ("reset".equals(purpose) && !exists)) return Result.success();
        String code = verificationCodeService.generateCode();
        try { verificationCodeService.storeCode(purpose, email, code, 10); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请60秒后重试"); }
        try {
            if ("register".equals(purpose)) emailService.sendRegisterCode(email, code);
            else emailService.sendResetPasswordCode(email, code);
        } catch (Exception e) {
            verificationCodeService.discardCode(purpose, email, code);
            LOG.error("验证码邮件发送失败，用途={}", purpose, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "邮件发送暂不可用");
        }
        return Result.success();
    }
}
