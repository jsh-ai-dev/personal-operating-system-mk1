package com.jsh.pos.application.port.out

import com.jsh.pos.domain.note.Note

/**
 * 노트 저장(CUD: Create, Update, Delete) 포트입니다.
 *
 * port.out의 의미:
 * - 애플리케이션이 외부(저장소, API 등)에 의존하는 지점
 * - 구현체는 adapter/out에서 제공 (JPA Repository 등)
 * - 유스케이스는 이 인터페이스만 의존, 실제 DB 접근 방식은 모름
 *
 * 명명 규칙:
 * - 조회(Read): QueryPort
 * - 저장/변경(Create, Update, Delete): CommandPort
 * - 이렇게 나누면 읽기 전용 포트와 쓰기 포트를 분리 가능
 */
interface NoteCommandPort {
    /**
     * 노트를 저장합니다 (생성 또는 수정).
     *
     * @param note 저장할 노트
     * @return 저장된 노트 (ID 등 DB가 할당한 값 포함)
     */
    fun save(note: Note): Note

    /**
     * ID로 노트를 삭제합니다.
     *
     * @param id 삭제할 노트의 ID
     * @return 삭제 성공 여부 (true: 삭제됨, false: 대상 없음)
     */
    fun deleteById(id: String): Boolean
}



