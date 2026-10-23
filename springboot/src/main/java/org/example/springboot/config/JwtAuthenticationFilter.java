package org.example.springboot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserMapper users;
    public JwtAuthenticationFilter(UserMapper users) { this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("token");
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) token = bearer.substring(7);
        if (token != null && !token.isBlank()) {
            try {
                var jwt = JwtTokenUtils.verify(token);
                if(jwt.getAudience() == null || jwt.getAudience().size() != 1) throw new IllegalArgumentException("Missing identity");
                var user = users.selectById(Long.valueOf(jwt.getAudience().get(0)));
                if (user == null || user.getPassword() == null || !Integer.valueOf(1).equals(user.getStatus()) ||
                    !JwtTokenUtils.fingerprint(user.getPassword()).equals(jwt.getClaim("credential").asString())) {
                    throw new IllegalArgumentException("Invalid account");
                }
                var auth = new UsernamePasswordAuthenticationToken(user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (com.auth0.jwt.exceptions.JWTVerificationException | IllegalArgumentException | IndexOutOfBoundsException e) {
                // Invalid or legacy tokens never establish an authenticated identity.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
