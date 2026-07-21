package com.flab.orderplatform.order.application.query;

import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQueryService {
    private final ProductRepository productRepository;

    /**
     * 상품코드별 가격 정보
     *
     * @param productCodes 상품 코드 목록
     * @return 상품코드별 가격 맵
     */
    @Transactional(readOnly = true)
    public Map<String, Long> createPriceByProductCodeMap(List<String> productCodes) {
        var products = productRepository.findAllByProductCode(productCodes);
        return products.stream().collect(Collectors.toMap(Product::productCode, Product::price));
    }
}
