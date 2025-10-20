package com.store.store.service;

import com.store.store.dto.ProductDto;

import java.util.List;
import java.util.Optional;

public interface IProductService {
    List<ProductDto> getProducts();
    Optional<ProductDto> getProductById(Long id);
}
