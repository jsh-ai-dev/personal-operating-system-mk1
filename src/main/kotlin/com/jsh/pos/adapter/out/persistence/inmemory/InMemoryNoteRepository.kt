package com.jsh.pos.adapter.out.persistence.inmemory

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.context.annotation.Profile
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
 * - 프로덕션에서는 절대 사용하면 안 됨 (재시작하면 데이터 손실)
 * - 지금은 "inmemory" 프로필을 켠 경우에만 Spring Bean으로 등록됨
 * - 즉, 평소 실행에서는 PostgreSQL(JPA) 구현이 기본 저장소 역할을 담당
 */
@Repository
@Profile("inmemory")
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
        // [4-POST/PUT] 실제 저장이 일어나는 지점입니다.
        // 브레이크포인트 추천: note.id가 기대값인지, map에 덮어쓰기/신규 저장이 맞는지 확인
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

    /**
     * 키워드로 노트를 검색합니다.
     *
     * 검색 대상: 제목(title), 본문(content), AI 요약(aiSummary), 태그(tags)
     * 대소문자 구분 없음, 부분 일치 허용
     *
     * @param keyword 검색어 (이미 trim된 상태)
     * @return 매칭된 노트 목록
     */
    override fun searchByKeyword(keyword: String): List<Note> {
        // [4-SEARCH] 실제 검색 로직이 수행되는 지점입니다.
        // 브레이크포인트 추천: normalized 값, title/content/tags 중 어느 조건이 매칭되는지 확인
        val normalized = keyword.lowercase()

        return notes.values.filter { note ->
            note.title.lowercase().contains(normalized) ||
                note.content.lowercase().contains(normalized) ||
                (note.aiSummary?.lowercase()?.contains(normalized) == true) ||
                note.tags.any { tag -> tag.lowercase().contains(normalized) }
        }
    }

    /**
     * 북마크된 노트를 최신 등록순으로 반환합니다.
     */
    override fun findAllBookmarked(): List<Note> =
        notes.values
            .filter { it.bookmarked }
            .sortedByDescending { it.createdAt }

    /**
     * 전체 노트 목록을 최신 등록순으로 반환합니다.
     */
    override fun findAll(): List<Note> =
        notes.values
            .sortedByDescending { it.createdAt }
}
