package org.example.springboot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.service.AccessService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Set;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final AccessService access;
    public JwtInterceptor(AccessService access) { this.access = access; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || "OPTIONS".equals(request.getMethod())) return true;
        String controller = method.getBeanType().getSimpleName();
        String action = method.getMethod().getName();
        if (Set.of("login", "register", "forgetPassword").contains(action) && "UserController".equals(controller)) return true;
        if ("SendEmailController".equals(controller)) return true;
        if ("AlipayController".equals(controller)) return true; // Dedicated payment service checks ownership; callbacks verify signatures.
        if ("GET".equals(request.getMethod()) && Set.of("ProductController", "CategoryController", "ArticleController", "CarouselItemController", "NoticeController").contains(controller)) return true;
        if ("GET".equals(request.getMethod()) && "ReviewController".equals(controller) && Set.of("getReviewsByProductId", "getPublicReviewPage").contains(action)) return true;
        if ("GroupBuyController".equals(controller) && Set.of("getActiveActivities", "getActivityDetail", "getActiveTeams").contains(action)) return true;
        access.current();
        if (Set.of("UserController", "OrderController", "AddressController", "CartController", "FavoriteController", "ReviewController", "ProductController", "LogisticsController", "GroupBuyController", "RecommendController", "CustomerServiceController", "FileController").contains(controller)) return true;
        access.admin(); // Administrative and diagnostic APIs are closed by default.
        return true;
    }
}
