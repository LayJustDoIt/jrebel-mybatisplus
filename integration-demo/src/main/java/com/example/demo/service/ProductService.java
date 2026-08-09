package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Product getById(Long id) {
        return productMapper.selectByTest(id);
    }

    public Product selectById(Long id) {
        return productMapper.selectById(id);
    }

    public List<Product> selectList() {
        return productMapper.selectList(null);
    }

    public IPage<Product> selectPage(int pageNum, int pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        return productMapper.selectPage(page, null);
    }
}
