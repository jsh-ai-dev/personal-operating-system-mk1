package com.jsh.pos.domain.note

/**
 * 노트의 공개 범위를 나타내는 열거형입니다.
 * 
 * 특징:
 * - 도메인 계층에 속해 무조건 이 두 값 중 하나만 허용
 * - 문자열 대신 Enum을 사용해 타입 안전성 확보
 * - 컨트롤러에서 요청 JSON 문자열이 자동으로 변환됨
 * 
 * PUBLIC: 인증 없이 누구나 조회 가능
 * PRIVATE: 작성자만 조회 가능 (인증 필수)
 */
enum class Visibility {
    PUBLIC,   // 공개
    PRIVATE,  // 비공개
}


