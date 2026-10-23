package org.example.springboot;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.config.SecurityConfig;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, SecurityRegressionTest.Config.class})
class SecurityRegressionTest {
    static final String SECRET = "test-only-secret-at-least-32-characters-long";
    @Configuration @EnableWebMvc
    static class Config {
        @Bean UserMapper users() { return mock(UserMapper.class); }
        @Bean JwtTokenUtils jwt() { return new JwtTokenUtils(SECRET); }
        @Bean Endpoints endpoints() { return new Endpoints(); }
        @Bean ReviewController publicReviews() { return new ReviewController(); }
        @Bean ProductController catalog() { return new ProductController(); }
        @Bean org.springframework.web.servlet.config.annotation.WebMvcConfigurer authorization() {
            return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
                @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                    registry.addInterceptor(new org.example.springboot.config.JwtInterceptor(new org.example.springboot.service.AccessService()));
                }
            };
        }
    }
    @RestController static class Endpoints {
        @GetMapping("/api/private") String privateEndpoint() { return "ok"; }
    }
    @RestController static class ProductController {
        @GetMapping("/api/product/page") String publicCatalog() { return "ok"; }
    }
    @RestController static class ReviewController {
        @GetMapping("/api/review/product/{id}/page") String getPublicReviewPage() { return "ok"; }
    }
    @Autowired WebApplicationContext context;
    @Autowired UserMapper users;
    MockMvc mvc;
    User user;
    @BeforeEach void setup() {
        reset(users);
        user = new User(); user.setId(1L); user.setRole("ADMIN"); user.setStatus(1); user.setPassword("stored-hash");
        when(users.selectById(1L)).thenReturn(user);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void anonymousCannotAccessProtectedApi() throws Exception { mvc.perform(get("/api/private")).andExpect(status().isUnauthorized()); }
    @Test void publicCatalogRemainsAccessible() throws Exception { mvc.perform(get("/api/product/page")).andExpect(status().isOk()); }
    @Test void signedTokenAuthenticates() throws Exception { mvc.perform(get("/api/private").header("token", JwtTokenUtils.genToken("1", user.getPassword()))).andExpect(status().isOk()); }
    @Test void ordinaryUserCannotAccessAdministrativeApi() throws Exception {
        user.setRole("USER");
        mvc.perform(get("/api/private").header("token", JwtTokenUtils.genToken("1", user.getPassword()))).andExpect(status().isForbidden());
    }
    @Test void hashSignedLegacyTokenIsRejected() throws Exception {
        String forged = JWT.create().withAudience("1").withIssuer("online-sales").sign(Algorithm.HMAC256(user.getPassword()));
        mvc.perform(get("/api/private").header("token", forged)).andExpect(status().isUnauthorized());
    }
    @Test void expiredTokenIsRejected() throws Exception {
        String expired = JWT.create().withIssuer("online-sales").withAudience("1").withExpiresAt(new Date(1)).sign(Algorithm.HMAC256(SECRET));
        mvc.perform(get("/api/private").header("token", expired)).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeRevokesToken() throws Exception {
        String token = JwtTokenUtils.genToken("1", user.getPassword()); user.setPassword("changed-hash");
        mvc.perform(get("/api/private").header("token", token)).andExpect(status().isUnauthorized());
    }
    @Test void disabledAccountCannotAuthenticate() throws Exception {
        String token = JwtTokenUtils.genToken("1", user.getPassword()); user.setStatus(0);
        mvc.perform(get("/api/private").header("token", token)).andExpect(status().isUnauthorized());
    }
    @Test void tokenInQueryStringDoesNotAuthenticate() throws Exception {
        mvc.perform(get("/api/private").param("token", JwtTokenUtils.genToken("1", user.getPassword()))).andExpect(status().isUnauthorized());
    }
    @Test void passwordIsAcceptedOnInputAndNeverReturned() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.writeValueAsString(user).contains("stored-hash"));
        assertEquals("input", mapper.readValue("{\"password\":\"input\"}", User.class).getPassword());
    }
    @Test void publicReviewPageIsAccessibleWithoutLoginThroughFilterAndInterceptor() throws Exception {
        mvc.perform(get("/api/review/product/1/page")).andExpect(status().isOk());
    }
}
