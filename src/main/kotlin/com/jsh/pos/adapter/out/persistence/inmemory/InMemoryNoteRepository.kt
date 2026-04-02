package com.jsh.pos.adapter.out.persistence.inmemory

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 인메모리 노트 저장소 구현입니다.
 *
 * 용도:
 * - 개발 초기 단계에서 데이터베이스 없이 빠르게 기능 검증
 * - 나중에 JPA 구현으로 교체해도 인터페이스는 동일
 *
 * 특징:
 * - ConcurrentHashMap 사용: 멀티스레드 환경에서도 안전
 * - @Repository로 Spring에 등록: 자동 DI 가능
 * - 두 포트(CommandPort, QueryPort) 동시 구현: 읽기/쓰기 분리 가능
 *
 * 주의사항:
 * - 프로덕션에서는 절대 사용하면 안 됨 (메모리 누수, 재시작하면 데이터 손실)
 * - 테스트나 개발 환경에서만 사용
 */
@Repository
class InMemoryNoteRepository : NoteCommandPort, NoteQueryPort {
    // ConcurrentHashMap: ID를 키로 Note를 저장
    // 멀티스레드 환경에서 동시성 문제 해결
    private val notes = ConcurrentHashMap<String, Note>()

    /**
     * 노트를 메모리에 저장합니다.
     *
     * @param note 저장할 노트
     * @return 저장된 노트 (그대로 반환)
     */
    override fun save(note: Note): Note {
        notes[note.id] = note  // ID를 키로 저장
        return note            // 반환 (같은 객체)
    }

    /**
     * 아이디로 노트를 조회합니다.
     *
     * @param id 조회할 노트의 ID
     * @return 노트 (없으면 null)
     */
    override fun findById(id: String): Note? = notes[id]

    /**
     * 아이디로 노트를 삭제합니다.
     *
     * remove의 반환값이 null이 아니면 실제 삭제가 일어난 것입니다.
     */
    override fun deleteById(id: String): Boolean = notes.remove(id) != null
}





