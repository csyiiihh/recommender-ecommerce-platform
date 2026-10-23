package org.example.springboot.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/alipay")
public class AlipayController {
    private final PaymentService payments;
    private final String frontendUrl;
    public AlipayController(PaymentService payments, @Value("${app.frontend-url}") String frontendUrl) { this.payments=payments; this.frontendUrl=frontendUrl; }
    @ResponseBody
    @PostMapping(value="/create", produces="text/html;charset=utf-8")
    public String create(@RequestParam("orderNo") String orderNo) { return payments.create(orderNo); }
    @ResponseBody
    @PostMapping("/refund")
    public Result<?> refund() { return Result.error("400", "请通过订单退款审核流程退款"); }
    @GetMapping("/success")
    public void success(HttpServletResponse response) throws java.io.IOException {
        // A browser redirect is never proof of payment. The order page queries the server.
        response.sendRedirect(frontendUrl + "/order");
    }
    @ResponseBody
    @PostMapping("/notify")
    public String payNotify(@RequestParam Map<String,String> params) {
        try { payments.notifyPaid(params); return "success"; }
        catch(Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("支付通知未处理成功: {}", e.getMessage()); return "failure"; }
    }
}
