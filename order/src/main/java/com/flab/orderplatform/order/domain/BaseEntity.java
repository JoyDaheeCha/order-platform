package com.flab.orderplatform.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성자, 수정자, 생성일, 수정일
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '생성일'")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '수정일'")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20) NOT NULL COMMENT '생성자'")
    private String createdBy;

    @LastModifiedBy
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20) NOT NULL COMMENT '수정자'")
    private String updatedBy;
}
