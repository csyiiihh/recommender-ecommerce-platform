package org.example.springboot;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.controller.email.SendEmailController;
import org.example.springboot.entity.Article;
import org.example.springboot.exception.*;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.service.ArticleService;
import org.example.springboot.service.FileService;
import org.example.springboot.service.email.*;
import org.example.springboot.util.FileUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditBoundaryTest {
    @Test void objectLevelValidationDoesNotThrowInsideTheErrorHandler() {
        var binding=new BeanPropertyBindingResult(new Object(),"request"); binding.addError(new ObjectError("request","日期范围无效"));
        var result=new GlobalExceptionHandler().handleBindException(new BindException(binding));
        assertEquals("400",result.getCode()); assertTrue(result.getMsg().contains("日期范围无效"));
    }
    @Test void articleOrderingIsAppliedBeforePaginationAndUsesAnAllowlist() {
        new com.baomidou.mybatisplus.core.MybatisConfiguration().addMapper(ArticleMapper.class);
        ArticleMapper mapper=mock(ArticleMapper.class); ArticleService service=new ArticleService(); ReflectionTestUtils.setField(service,"articleMapper",mapper);
        when(mapper.selectPage(any(Page.class),any())).thenReturn(new Page<>());
        service.getArticlesByPage("book",1,2,10,"popular"); ArgumentCaptor<Wrapper> query=ArgumentCaptor.forClass(Wrapper.class); verify(mapper).selectPage(any(Page.class),query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("view_count DESC"));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.getArticlesByPage(null,1,1,10,"id; DROP TABLE article")).getStatusCode().value());
    }
    @Test void batchUploadRollsBackOnlyItsOwnUrlsUsingTheStorageUtility() {
        var first=new MockMultipartFile("file","a.png","image/png",new byte[]{1}); var invalid=new MockMultipartFile("file","b.png","image/png",new byte[]{2});
        try(var files=mockStatic(FileUtil.class)) {
            files.when(()->FileUtil.saveFile(first,null,"img")).thenReturn("/img/new-fixture.png");
            files.when(()->FileUtil.saveFile(invalid,null,"img")).thenThrow(new BusinessException("invalid image"));
            assertThrows(BusinessException.class,()->new FileService().uploadMultiple(List.of(first,invalid)));
            files.verify(()->FileUtil.deleteFile("/img/new-fixture.png"));
        }
    }
    @Test void batchUploadReturnsPathsCoveredByTheImageResourceMapping() {
        var first=new MockMultipartFile("file","a.png","image/png",new byte[]{1});
        try(var files=mockStatic(FileUtil.class)) {
            files.when(()->FileUtil.saveFile(first,null,"img")).thenReturn("/img/new-fixture.png");
            assertEquals(List.of("/img/new-fixture.png"),new FileService().uploadMultiple(List.of(first)));
            files.verify(()->FileUtil.deleteFile(anyString()),never());
        }
    }
    @Test void emailSourcesHaveAnIndependentBoundedSendLimit() {
        VerificationCodeService codes=new VerificationCodeService(); for(int i=0;i<5;i++) codes.reserveSend("fixture-peer");
        assertEquals(429,assertThrows(ResponseStatusException.class,()->codes.reserveSend("fixture-peer")).getStatusCode().value());
        assertDoesNotThrow(()->codes.reserveSend("another-peer"));
    }
    @Test void codeNormalizationAndFailureReleaseDoNotRemoveAnotherCode() {
        VerificationCodeService codes=new VerificationCodeService(); codes.storeCode("register"," Reader@Example.invalid ","123456",10);
        codes.discardCode("register","reader@example.invalid","other"); assertTrue(codes.verifyCode("register","reader@example.invalid","123456"));
        codes.storeCode("reset","reader@example.invalid","654321",10); codes.discardCode("reset","reader@example.invalid","654321");
        assertDoesNotThrow(()->codes.storeCode("reset","reader@example.invalid","111111",10));
    }
    @Test void mailTransportFailureReleasesTheCodeAndCooldown() {
        SendEmailController controller=new SendEmailController(); UserServiceFixture fixture=new UserServiceFixture(controller);
        doThrow(new IllegalStateException("test SMTP unavailable")).when(fixture.mail).sendRegisterCode(anyString(),anyString());
        var input=new SendEmailController.EmailRequest("reader@example.invalid"); var request=new MockHttpServletRequest(); request.setRemoteAddr("fixture-peer");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->controller.emailRegister(input,request)).getStatusCode().value());
        assertDoesNotThrow(()->fixture.codes.storeCode("register","reader@example.invalid","123456",10));
    }
    static class UserServiceFixture {
        final EmailService mail=mock(EmailService.class); final VerificationCodeService codes=new VerificationCodeService();
        UserServiceFixture(SendEmailController controller) {
            ReflectionTestUtils.setField(controller,"userService",mock(org.example.springboot.service.UserService.class));
            ReflectionTestUtils.setField(controller,"emailService",mail); ReflectionTestUtils.setField(controller,"verificationCodeService",codes);
        }
    }
}
