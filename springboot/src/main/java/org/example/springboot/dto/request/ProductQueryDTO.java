package org.example.springboot.dto.request;

import lombok.Data;

/**
 * 商品查询DTO
 * 用于封装商品查询参数
 */
@Data
public class ProductQueryDTO {
    /**
     * 商品名称（模糊查询）
     */
    private String name;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 商户ID
     */
    private Long merchantId;
    
    /**
     * 商品状态（0-下架，1-上架）
     */
    private Integer status;
    
    /**
     * 当前页码
     */
    private Integer currentPage = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 排序字段（sales-销量，price-价格，createdAt-创建时间）
     */
    private String sortField;
    
    /**
     * 排序方向（asc-升序，desc-降序）
     */
    private String sortOrder;
    
    /**
     * 最低价格
     */
    private Double minPrice;
    
    /**
     * 最高价格
     */
    private Double maxPrice;
    
    /**
     * 作者（模糊查询）
     */
    private String author;
    
    /**
     * 出版社（模糊查询）
     */
    private String publisher;
    
    /**
     * ISBN（模糊查询）
     */
    private String isbn;
}

