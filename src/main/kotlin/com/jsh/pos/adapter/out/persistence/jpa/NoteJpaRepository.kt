package com.jsh.pos.adapter.out.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Spring Data JPA 전용 Repository 인터페이스입니다.
 *
 * 역할 분리:
 * - NoteCommandPort / NoteQueryPort: 애플리케이션이 의존하는 "업무용 계약"
 * - NoteJpaRepository: JPA가 제공하는 "기술용 계약"
 *
 * 즉, 서비스 계층은 이 인터페이스를 직접 몰라도 되고,
 * 바깥 어댑터(JpaNotePersistenceAdapter)만 이 인터페이스를 사용합니다.
 */
interface NoteJpaRepository : JpaRepository<NoteJpaEntity, String> {
    /**
     * 제목, 본문, AI 요약, 태그를 대상으로 부분 일치 검색을 수행합니다.
     *
     * 구현 포인트:
     * - lower(... like lower(...)) 형태로 대소문자 구분 없이 검색합니다.
     * - tags는 별도 테이블이라 left join으로 함께 조회합니다.
     * - 한 노트가 여러 태그로 중복 매칭될 수 있으므로 distinct를 사용합니다.
     * - 최신 수정 순으로 보여 주면 사용자가 방금 다룬 메모를 찾기 쉽습니다.
     */
    @Query(
        """
        select distinct note
        from NoteJpaEntity note
        left join note.tags tag
        where lower(note.title) like lower(concat('%', :keyword, '%'))
           or lower(note.content) like lower(concat('%', :keyword, '%'))
           or lower(coalesce(note.aiSummary, '')) like lower(concat('%', :keyword, '%'))
           or lower(tag) like lower(concat('%', :keyword, '%'))
        order by note.updatedAt desc
        """,
    )
    fun searchByKeyword(@Param("keyword") keyword: String): List<NoteJpaEntity>

    /**
     * 북마크된 노트를 최신 등록순으로 조회합니다.
     *
     * Spring Data JPA의 메서드 이름 규칙:
     * - findAll: 전체 조회
     * - ByBookmarkedTrue: bookmarked = true 조건
     * - OrderByCreatedAtDesc: createdAt 내림차순 정렬
     *
     * 별도 @Query 없이 메서드 이름만으로 쿼리를 자동 생성합니다.
     */
    fun findAllByBookmarkedTrueOrderByCreatedAtDesc(): List<NoteJpaEntity>

    /**
     * 전체 노트를 최신 작성순으로 조회합니다.
     */
    fun findAllByOrderByCreatedAtDesc(): List<NoteJpaEntity>
}
