package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

/**
 * 노트 조회 유스케이스의 계약입니다.
 * 
 * 포트 인터페이스의 장점:
 * - 구현체가 없어도 테스트에서 Mock으로 대체 가능
 * - 저장소(JPA, Redis, API 등)를 나중에 바꿔도 영향 없음
 * - 비즈니스 로직과 기술 세부사항이 명확히 분리됨
 */
interface GetNoteUseCase {
    /**
     * ID로 노트를 조회합니다.
     * 
     * @param id 조회할 노트의 ID
     * @return 노트 (없으면 null)
     */
    fun getById(id: String): Note?
}


