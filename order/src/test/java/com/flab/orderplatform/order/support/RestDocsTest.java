package com.flab.orderplatform.order.support;

import com.flab.orderplatform.order.infrastructure.web.common.ApiResponseAdvice;
import com.flab.orderplatform.order.infrastructure.web.common.RestExceptionHandler;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * restDocs 테스트 용 애노테이션
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@AutoConfigureRestDocs
@Import(RestDocsTest.CommonWebBeansSelector.class)
public @interface RestDocsTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value();

    class CommonWebBeansSelector implements ImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata metadata) {
            Map<String, Object> attributes =
                    metadata.getAnnotationAttributes(RestDocsTest.class.getName());
            Class<?>[] controllers = (Class<?>[]) attributes.get("value");

            List<String> imports = new ArrayList<>();
            imports.add(ApiResponseAdvice.class.getName());
            imports.add(RestExceptionHandler.class.getName());
            imports.add(RestDocsConfig.class.getName());
            for (Class<?> controller : controllers) {
                imports.add(controller.getName());
            }
            return imports.toArray(new String[0]);
        }
    }
}
