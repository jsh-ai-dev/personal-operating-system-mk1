package com.jsh.pos.application.model

/**
 * 애플리케이션 계층에서 공통으로 사용하는 페이지 결과 모델입니다.
 */
data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

