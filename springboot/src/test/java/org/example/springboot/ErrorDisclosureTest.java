package org.example.springboot;
import org.example.springboot.exception.*;
import org.junit.jupiter.api.Test;
import org.example.springboot.controller.*;
import org.example.springboot.service.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
class ErrorDisclosureTest {
 final GlobalExceptionHandler handler=new GlobalExceptionHandler();
 @Test void internalDetailsAreHiddenWithoutDependingOnProfile() {assertFalse(handler.handleException(new RuntimeException("jdbc:mysql://private password=secret")).getMsg().contains("jdbc"));}
 @Test void wrappedOperationFailureIsSanitized() {assertFalse(handler.handleBusinessException(new BusinessException("1003","SQL syntax at private_table")).getMsg().contains("SQL"));}
 @Test void businessReasonRemainsUseful() {assertEquals("库存不足",handler.handleBusinessException(new BusinessException("1000","库存不足")).getMsg());}
 @Test void serverStatusReasonCannotExposeInfrastructure() {assertFalse(handler.handleStatus(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"private_table")).getBody().getMsg().contains("private_table"));}

 private MockMvc mvc(Object controller) {
  return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler).build();
 }
 @Test void groupControllerDoesNotBypassGlobalErrorHandling() throws Exception {
  GroupBuyService service=mock(GroupBuyService.class); GroupBuyController controller=new GroupBuyController();
  ReflectionTestUtils.setField(controller,"groupBuyService",service);
  when(service.getActiveActivityList(1,10)).thenThrow(new IllegalStateException("jdbc private_table"));
  mvc(controller).perform(get("/group-buy/activities/active"))
   .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.msg").value("系统内部错误，请联系管理员"));
 }
 @Test void groupControllerPreservesExpectedBusinessReason() throws Exception {
  GroupBuyService service=mock(GroupBuyService.class); GroupBuyController controller=new GroupBuyController();
  ReflectionTestUtils.setField(controller,"groupBuyService",service);
  when(service.getActivityDetail(1L)).thenThrow(new BusinessException("活动不存在"));
  mvc(controller).perform(get("/group-buy/activity/1"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.msg").value("活动不存在"));
 }
 @Test void customerControllerDoesNotReturnDatabaseExceptionText() throws Exception {
  CustomerServiceService service=mock(CustomerServiceService.class); CustomerServiceController controller=new CustomerServiceController();
  ReflectionTestUtils.setField(controller,"customerServiceService",service);
  ReflectionTestUtils.setField(controller,"access",mock(AccessService.class));
  when(service.getOrCreateSession(1L)).thenThrow(new IllegalStateException("jdbc private_table"));
  mvc(controller).perform(post("/customerService/session/create").param("userId","1"))
   .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.msg").value("系统内部错误，请联系管理员"));
 }
 @Test void redisDiagnosticsDoNotLeakNestedInfrastructureDetails() {
  RedisTemplate<?,?> redis=mock(RedisTemplate.class); RedisTestController controller=new RedisTestController();
  ReflectionTestUtils.setField(controller,"redisTemplate",redis);
  when(redis.opsForValue()).thenThrow(new IllegalStateException("redis://private password=secret"));
  var connection=controller.testConnection();
  assertFalse(connection.getMsg().contains("private"));
  assertFalse(connection.getData().toString().contains("secret"));
  when(redis.getConnectionFactory()).thenThrow(new IllegalStateException("redis://private password=secret"));
  assertFalse(controller.getRedisInfo().getData().toString().contains("secret"));
 }
}
