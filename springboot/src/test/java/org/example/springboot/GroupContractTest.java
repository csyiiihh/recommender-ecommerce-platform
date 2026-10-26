package org.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.controller.GroupBuyController;
import org.example.springboot.entity.*;
import org.example.springboot.exception.GlobalExceptionHandler;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupContractTest {
    GroupBuyService service;
    GroupBuyActivityMapper activities;
    ProductMapper products;
    MockMvc mvc;
    User user;
    @BeforeEach void setup() {
        service=mock(GroupBuyService.class); activities=mock(GroupBuyActivityMapper.class); products=mock(ProductMapper.class);
        AccessService access=new AccessService(); ReflectionTestUtils.setField(access,"activities",activities); ReflectionTestUtils.setField(access,"products",products);
        GroupBuyController controller=new GroupBuyController(); ReflectionTestUtils.setField(controller,"access",access); ReflectionTestUtils.setField(controller,"groupBuyService",service);
        var builder = new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
        new org.example.springboot.config.JacksonConfig().jsonCustomizer().customize(builder);
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(builder.build())).build();
        user=new User(); user.setId(42L); user.setRole("MERCHANT");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void merchantListCannotChooseAnotherMerchantsScope() throws Exception {
        when(service.getActivityList(1,25,1,42L,"book")).thenReturn(new Page<>());
        mvc.perform(get("/group-buy/activities").param("size","25").param("status","1").param("merchantId","999").param("activityName","book"))
            .andExpect(status().isOk()); verify(service).getActivityList(1,25,1,42L,"book");
    }
    @Test void administratorCanFilterAnExplicitMerchant() throws Exception {
        user.setRole("ADMIN"); when(service.getActivityList(1,10,null,7L,null)).thenReturn(new Page<>());
        mvc.perform(get("/group-buy/activities").param("merchantId","7")).andExpect(status().isOk()); verify(service).getActivityList(1,10,null,7L,null);
    }
    @Test void buyerCannotReadManagementList() throws Exception {
        user.setRole("USER"); mvc.perform(get("/group-buy/activities")).andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    String activityBody() {
        return "{\"id\":2,\"productId\":9,\"activityName\":\"book\",\"groupPrice\":20,\"requiredMembers\":3,\"validityHours\":24,\"startTime\":\"2035-01-01 00:00:00\",\"endTime\":\"2035-01-02 00:00:00\",\"merchantId\":999,\"stock\":999,\"salesCount\":999,\"status\":3}";
    }
    void ownedActivity(Long owner) {
        GroupBuyActivity a=new GroupBuyActivity(); a.setId(2L); a.setMerchantId(owner); when(activities.selectById(2L)).thenReturn(a);
        Product p=new Product(); p.setId(9L); p.setMerchantId(owner); when(products.selectById(9L)).thenReturn(p);
    }
    @Test void editableRequestDoesNotBindCountersStatusOrMerchant() throws Exception {
        ownedActivity(42L); when(service.updateActivity(any())).thenReturn(true);
        mvc.perform(put("/group-buy/activity").contentType("application/json").content(activityBody())).andExpect(status().isOk());
        ArgumentCaptor<GroupBuyActivity> captured=ArgumentCaptor.forClass(GroupBuyActivity.class); verify(service).updateActivity(captured.capture());
        GroupBuyActivity input=captured.getValue(); assertNull(input.getStock()); assertNull(input.getSalesCount()); assertNull(input.getMerchantId()); assertNull(input.getStatus());
    }
    @Test void merchantCannotEditAnotherActivity() throws Exception {
        ownedActivity(99L); mvc.perform(put("/group-buy/activity").contentType("application/json").content(activityBody())).andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void invalidActivityBodyIsRejectedBeforeWrites() throws Exception {
        mvc.perform(put("/group-buy/activity").contentType("application/json").content(activityBody().replace("\"requiredMembers\":3","\"requiredMembers\":1"))).andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void missingAndFractionalOrderParametersAreClientErrors() throws Exception {
        for (String input:List.of("{}","{\"userId\":42,\"addressId\":8,\"activityId\":2,\"quantity\":0}","{\"userId\":\"bad-id\",\"addressId\":8,\"quantity\":1}","{\"userId\":42,\"addressId\":8,\"activityId\":2,\"quantity\":1.5}")) {
            mvc.perform(post("/group-buy/start").contentType("application/json").content(input)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void malformedPathIdReturns400InsteadOf500() throws Exception {
        mvc.perform(get("/group-buy/activity/not-a-number")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("400"));
    }
    @Test void publicTeamRouteDoesNotExposePrivateAccounts() throws Exception {
        User u=new User(); u.setId(8L); u.setName("Reader"); u.setEmail("secret@example.invalid"); u.setBusinessLicense("private-license");
        GroupBuyTeam t=new GroupBuyTeam(); t.setId(2L); t.setLeader(u); t.setLeaderOrderId(9876L); GroupBuyMember m=new GroupBuyMember(); m.setOrderId(9876L); m.setUser(u); t.setMembers(List.of(m));
        when(service.getActiveTeams(2L)).thenReturn(List.of(t));
        String body=mvc.perform(get("/group-buy/teams").param("activityId","2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for(String value:List.of("email","businessLicense","orderId","leaderOrderId")) assertFalse(body.contains(value));
    }
}
