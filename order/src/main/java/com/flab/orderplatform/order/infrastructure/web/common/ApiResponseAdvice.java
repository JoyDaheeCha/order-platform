package com.flab.orderplatform.order.infrastructure.web.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 api 응답을 {@link ApiResponse} 봉투로 감싼다<br>
 * 컨트롤러는 순수 payload만 반환하고 응답 값 파싱은 본 advice에서 처리한다.
 */
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "com.flab.orderplatform.order.infrastructure.web")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {

        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
                || StringHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        var envelope = wrap(body);

        // String 반환시, Json 문자열로 직렬화 필요.
        // String 그대로 넘기면 ClassCastException이 발생한다.
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(envelope);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("api 응답을 봉투로 변경 실패하였습니다.", e);
            }
        }
        return envelope;
    }

    private Object wrap(Object body) {
        if (body instanceof ApiResponse<?>) {
            return body; // 이미 봉투 적용되어있다면 이중 래핑 방지 (예. 예외핸들러에서 이미 봉투패턴으로 반환한 경우)
        }
        if (body instanceof ErrorResponse error) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.ok(body);
    }
}
