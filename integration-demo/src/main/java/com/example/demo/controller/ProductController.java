package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/product/selectById/{id}")
    public Product selectById(@PathVariable Long id) {
        return productService.selectById(id);
    }

    @GetMapping("/product/selectList")
    public List<Product> selectList() {
        return productService.selectList();
    }

    @GetMapping("/product/selectPage")
    public IPage<Product> selectPage(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "2") int pageSize) {
        return productService.selectPage(pageNum, pageSize);
    }
}
