package com.jsh.pos.adapter.`in`.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

/**
 * API 예외를 일관된 JSON 형태로 변환합니다.
 *
 * 현재는 비즈니스 입력 검증 실패(IllegalArgumentException)를 400으로 매핑합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse(
                timestamp = Instant.now().toString(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message ?: "잘못된 요청입니다",
                path = request.requestURI,
            ),
        )
}

data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)

