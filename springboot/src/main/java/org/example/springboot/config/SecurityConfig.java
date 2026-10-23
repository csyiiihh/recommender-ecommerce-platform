package org.example.springboot.config;

import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, UserMapper users, JwtTokenUtils jwt) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/login", "/api/user/register", "/api/user/forget", "/alipay/notify").permitAll()
                .requestMatchers(HttpMethod.GET, "/alipay/success", "/api/email/**", "/api/img/**",
                    "/api/product/**", "/api/category/**", "/api/article/**", "/api/carousel/**", "/api/notice/**",
                    "/api/review/product/**", "/api/group-buy/activities/active", "/api/group-buy/activity/*", "/api/group-buy/teams").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"code\":\"401\",\"msg\":\"请重新登录\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"code\":\"403\",\"msg\":\"无权访问\"}"); }))
            .addFilterBefore(new JwtAuthenticationFilter(users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
