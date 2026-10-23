package org.example.springboot;
import org.example.springboot.controller.ProductController;
import org.example.springboot.service.*;
import org.example.springboot.mapper.*;
import org.example.springboot.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProductVisibilityTest {
    @Mock ProductService service;
    @Mock AccessService access;
    @InjectMocks ProductController controller;
    @Test void publicClientCannotRequestUnpublishedProducts() {
        controller.getProductsByPage(null,null,null,0,1,10,null,null,null,null,null,null,null);
        verify(service).getProductsByPage(null,null,null,1,1,10,null,null,null,null,null,null,null);
        verifyNoInteractions(access);
    }
    @Test void managementListingIsAuthorizedAndScoped() {
        when(access.scope(null)).thenReturn(7L);
        controller.getManagedProductsByPage(null,null,null,0,1,10,null,null,null,null,null,null,null);
        verify(access).staff();verify(service).getProductsByPage(null,null,7L,0,1,10,null,null,null,null,null,null,null);
    }
    @Test void unpublishedDetailIsNotPublic() {
        ProductMapper products=mock(ProductMapper.class);UserMapper users=mock(UserMapper.class);
        ProductService real=new ProductService();org.springframework.test.util.ReflectionTestUtils.setField(real,"productMapper",products);org.springframework.test.util.ReflectionTestUtils.setField(real,"userMapper",users);
        Product product=new Product();product.setStatus(0);when(products.selectById(1L)).thenReturn(product);
        assertNotEquals("0",real.getProductById(1L).getCode());verifyNoInteractions(users);
    }
}
