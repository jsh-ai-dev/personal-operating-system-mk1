package com.jsh.pos.domain.note

import java.time.Instant

/**
 * 노트의 도메인 엔티티입니다.
 *
 * 역할:
 * - 노트의 핵심 데이터와 비즈니스 규칙을 담당
 * - 불변(immutable) data class로 설계해 상태 변경의 실수 방지
 * - 공장 메서드(create)를 통해서만 생성되도록 강제
 *
 * 주목할 점:
 * - Domain 계층이므로 Spring, JPA 등의 프레임워크 의존성 없음
 * - 데이터베이스 접근이나 저장은 이 엔티티의 책임이 아님
 * - 비즈니스 규칙 검증(blank, trim 등)을 여기서 수행
 */
data class Note(
    val id: String,                  // 노트의 고유 ID (UUID)
    val title: String,               // 노트의 제목
    val content: String,             // 노트의 본문 (Markdown 형식)
    val visibility: Visibility,      // 공개/비공개
    val tags: Set<String>,           // 태그들 (검색/분류용)
    val createdAt: Instant,          // 작성 시간
    val updatedAt: Instant,          // 최종 수정 시간
) {
    /**
     * 기존 노트를 수정한 새 인스턴스를 반환합니다.
     *
     * 불변 객체 패턴을 유지하기 위해 기존 인스턴스를 변경하지 않고
     * copy를 활용해 새로운 Note를 생성합니다.
     *
     * 수정 정책:
     * - createdAt은 유지
     * - updatedAt만 현재 시각(now)으로 갱신
     */
    fun update(
        title: String,
        content: String,
        visibility: Visibility,
        tags: Set<String>,
        now: Instant,
    ): Note {
        require(title.isNotBlank()) { "제목은 비워둘 수 없습니다" }
        require(content.isNotBlank()) { "본문은 비워둘 수 없습니다" }

        return copy(
            title = title.trim(),
            content = content.trim(),
            visibility = visibility,
            tags = tags.map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            updatedAt = now,
        )
    }

    companion object {
        /**
         * 노트를 생성하는 공장 메서드입니다.
         *
         * 왜 공장 메서드를 쓸까?
         * - 생성 시점에 비즈니스 규칙을 강제할 수 있음
         * - 입력값을 정제(trim, filter)하는 로직을 중앙화
         * - 주 생성자를 숨겨 유효하지 않은 상태의 Note 생성 방지
         *
         * @param id 생성할 노트의 ID (일반적으로 UUID)
         * @param title 노트 제목 (필수, 공백 제거)
         * @param content 노트 본문 (필수, 공백 제거)
         * @param visibility 공개 범위
         * @param tags 태그 집합 (공백 제거, 빈 문자열 필터링)
         * @param now 현재 시간
         * @return 유효성 검증을 통과한 Note 인스턴스
         * @throws IllegalArgumentException 제목이나 본문이 공백일 때
         */
        fun create(
            id: String,
            title: String,
            content: String,
            visibility: Visibility,
            tags: Set<String>,
            now: Instant,
        ): Note {
            // 제목 검증: 공백만 있으면 안 됨
            require(title.isNotBlank()) { "제목은 비워둘 수 없습니다" }

            // 본문 검증: 공백만 있으면 안 됨
            require(content.isNotBlank()) { "본문은 비워둘 수 없습니다" }

            return Note(
                id = id,
                title = title.trim(),           // 앞뒤 공백 제거
                content = content.trim(),       // 앞뒤 공백 제거
                visibility = visibility,
                // 태그: 각 항목의 공백 제거 후 빈 문자열 필터링, 중복 제거(Set)
                tags = tags
                    .map { it.trim() }          // 각 태그의 공백 제거
                    .filter { it.isNotBlank() } // 빈 문자열 제외
                    .toSet(),                   // 중복 제거
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}



