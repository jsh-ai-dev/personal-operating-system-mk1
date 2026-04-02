package com.jsh.pos.application.port.out

import com.jsh.pos.domain.note.Note

/**
 * 노트 조회(R: Read) 포트입니다.
 *
 * 설계 원칙 (CQRS 흉내):
 * - 읽기와 쓰기 포트를 분리하면 나중에 각각 다른 전략 적용 가능
 * - 예: 조회는 Redis 캐시, 쓰기는 PostgreSQL 같은 식으로
 * - 또는 읽기 전용 복제 DB 사용 시에도 유연함
 */
interface NoteQueryPort {
    /**
     * ID로 노트를 조회합니다.
     *
     * @param id 조회할 노트의 ID
     * @return 노트 (없으면 null)
     */
    fun findById(id: String): Note?
}




