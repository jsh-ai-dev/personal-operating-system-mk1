package com.jsh.pos.adapter.out.persistence.jpa

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * PostgreSQL(JPA)를 사용하는 노트 영속성 어댑터입니다.
 *
 * 이 클래스가 하는 일:
 * - 애플리케이션이 기대하는 포트(NoteCommandPort, NoteQueryPort)를 구현
 * - 도메인 Note와 JPA 엔티티 사이를 변환
 * - 실제 저장/조회는 Spring Data JPA Repository에 위임
 *
 * @Primary를 붙인 이유:
 * - 같은 포트를 구현하는 InMemoryNoteRepository도 남아 있기 때문입니다.
 * - 특별히 inmemory 프로필을 켜지 않는 한, 기본 저장소는 이 JPA 어댑터가 되게 합니다.
 * - 덕분에 서비스 계층 코드는 전혀 바꾸지 않고 저장 방식만 교체할 수 있습니다.
 */
@Repository
@Primary
@Transactional
class JpaNotePersistenceAdapter(
    private val noteJpaRepository: NoteJpaRepository,
) : NoteCommandPort, NoteQueryPort {

    /**
     * 노트를 저장합니다.
     *
     * 생성과 수정을 같은 save로 처리하는 이유:
     * - 현재 도메인 Note는 이미 id를 가진 완성된 객체입니다.
     * - JPA는 같은 id가 있으면 update, 없으면 insert로 처리합니다.
     */
    override fun save(note: Note): Note {
        val entity = NoteJpaEntity.fromDomain(note)
        val saved = noteJpaRepository.save(entity)
        return saved.toDomain()
    }

    /**
     * ID로 노트를 조회합니다.
     */
    @Transactional(readOnly = true)
    override fun findById(id: String): Note? =
        noteJpaRepository.findById(id)
            .map { it.toDomain() }
            .orElse(null)

    /**
     * ID로 노트를 삭제합니다.
     *
     * deleteById는 대상이 없어도 예외 없이 지나갈 수 있으므로,
     * 기존 계약(Boolean 반환)을 맞추기 위해 존재 여부를 먼저 확인합니다.
     */
    override fun deleteById(id: String): Boolean {
        if (!noteJpaRepository.existsById(id)) {
            return false
        }

        noteJpaRepository.deleteById(id)
        return true
    }

    /**
     * 키워드 검색입니다.
     *
     * 검색어 trim/blank 검증은 유스케이스 계층에서 이미 수행하므로,
     * 어댑터는 저장소 호출과 결과 변환에만 집중합니다.
     */
    @Transactional(readOnly = true)
    override fun searchByKeyword(keyword: String): List<Note> =
        noteJpaRepository.searchByKeyword(keyword)
            .map { it.toDomain() }

    /**
     * 북마크된 노트 목록을 반환합니다.
     *
     * Spring Data JPA 메서드 이름 규칙으로 자동 생성된 쿼리를 사용합니다.
     * 결과는 createdAt 내림차순(최신 등록순)으로 정렬됩니다.
     */
    @Transactional(readOnly = true)
    override fun findAllBookmarked(): List<Note> =
        noteJpaRepository.findAllByBookmarkedTrueOrderByCreatedAtDesc()
            .map { it.toDomain() }

    /**
     * 전체 노트 목록을 최신 작성순으로 반환합니다.
     */
    @Transactional(readOnly = true)
    override fun findAll(): List<Note> =
        noteJpaRepository.findAllByOrderByCreatedAtDesc()
            .map { it.toDomain() }
}
