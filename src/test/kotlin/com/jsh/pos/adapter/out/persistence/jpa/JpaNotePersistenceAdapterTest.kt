package com.jsh.pos.adapter.out.persistence.jpa

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.Instant

/**
 * JPA 영속성 어댑터 통합 테스트입니다.
 *
 * 왜 이 테스트가 중요한가?
 * - 서비스 단위 테스트만으로는 "진짜 DB 매핑"이 맞는지 알 수 없습니다.
 * - tags가 별도 테이블에 잘 저장되는지
 * - search 쿼리가 title/content/aiSummary/tag를 모두 검색하는지
 * - save/find/delete가 실제 JPA와 연결되어 동작하는지
 *   이런 부분은 영속성 테스트가 꼭 필요합니다.
 */
@DataJpaTest
@Import(JpaNotePersistenceAdapter::class)
class JpaNotePersistenceAdapterTest {

    @Autowired
    private lateinit var adapter: JpaNotePersistenceAdapter

    @Test
    fun `save and findById persist note`() {
        val now = Instant.parse("2026-04-03T00:00:00Z")
        val note = Note.create(
            id = "note-jpa-1",
            title = "  PostgreSQL 저장 테스트  ",
            content = "  실제 DB에 저장되는지 확인합니다  ",
            visibility = Visibility.PRIVATE,
            tags = setOf(" postgres ", " jpa "),
            now = now,
        )

        adapter.save(note)
        val found = adapter.findById("note-jpa-1")

        assertNotNull(found)
        assertEquals("PostgreSQL 저장 테스트", found!!.title)
        assertEquals("실제 DB에 저장되는지 확인합니다", found.content)
        assertEquals(setOf("postgres", "jpa"), found.tags)
        assertEquals(Visibility.PRIVATE, found.visibility)
        assertEquals(now, found.createdAt)
        assertEquals(now, found.updatedAt)
    }

    @Test
    fun `searchByKeyword finds note by title content summary and tag`() {
        val now = Instant.parse("2026-04-03T01:00:00Z")

        val note = Note.create(
            id = "note-jpa-2",
            title = "Kotlin 정리",
            content = "Spring Boot와 함께 사용한 내용",
            visibility = Visibility.PUBLIC,
            tags = setOf("kotlin", "spring"),
            now = now,
        ).updateSummary("코루틴 비동기 처리 핵심 정리", now.plusSeconds(1))

        adapter.save(note)

        assertEquals(1, adapter.searchByKeyword("kotlin").size)
        assertEquals(1, adapter.searchByKeyword("spring boot").size)
        assertEquals(1, adapter.searchByKeyword("비동기 처리").size)
        assertEquals(1, adapter.searchByKeyword("spring").size)
    }

    @Test
    fun `deleteById returns true only when note exists`() {
        val now = Instant.parse("2026-04-03T02:00:00Z")
        adapter.save(
            Note.create(
                id = "note-jpa-3",
                title = "삭제 테스트",
                content = "삭제됩니다",
                visibility = Visibility.PUBLIC,
                tags = emptySet(),
                now = now,
            ),
        )

        assertTrue(adapter.deleteById("note-jpa-3"))
        assertFalse(adapter.deleteById("note-jpa-3"))
        assertEquals(null, adapter.findById("note-jpa-3"))
    }
}

