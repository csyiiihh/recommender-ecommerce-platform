package org.example.springboot;

import org.example.springboot.entity.Product;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.service.FileService;
import org.example.springboot.service.ProductService;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageRetentionTest {
    @Mock ProductMapper products;
    @Mock FileService files;
    @InjectMocks ProductService service;
    Product existing() { Product p=new Product();p.setId(2L);p.setStock(5);p.setSalesCount(7);p.setImageUrl("/img/shared.jpg");when(products.selectForUpdate(2L)).thenReturn(p);return p; }
    Product patch() { Product p=new Product();p.setStock(999);p.setSalesCount(999);p.setImageUrl("/img/new.jpg");return p; }
    @Test void failedUpdateDoesNotDeleteOriginalImage() {
        Product old=existing();assertThrows(BusinessException.class,()->service.updateProduct(2L,patch()));assertEquals("/img/shared.jpg",old.getImageUrl());verifyNoInteractions(files);
    }
    @Test void successfulImageChangePreservesSharedFilesAndLiveStock() {
        existing();when(products.updateById(any(Product.class))).thenReturn(1);Product update=patch();assertEquals("0",service.updateProduct(2L,update).getCode());
        assertEquals(2L,update.getId());assertEquals(5,update.getStock());assertEquals(7,update.getSalesCount());assertEquals("/img/new.jpg",update.getImageUrl());verifyNoInteractions(files);
    }
    @Test void databaseExceptionDoesNotDeleteOriginalImage() {
        existing();when(products.updateById(any(Product.class))).thenThrow(new IllegalStateException("database unavailable"));assertThrows(BusinessException.class,()->service.updateProduct(2L,patch()));verifyNoInteractions(files);
    }
    @Test void missingProductNeverUpdatesOrDeletesFiles() {
        assertNotEquals("0",service.updateProduct(2L,patch()).getCode());verify(products,never()).updateById(any(Product.class));verifyNoInteractions(files);
    }
}
