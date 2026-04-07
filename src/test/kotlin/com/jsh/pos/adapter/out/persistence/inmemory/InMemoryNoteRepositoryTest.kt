package com.jsh.pos.adapter.out.persistence.inmemory

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * InMemoryNoteRepository 테스트입니다.
 */
class InMemoryNoteRepositoryTest {

    private val repository = InMemoryNoteRepository()

    @Test
    fun `searchByKeyword finds notes by title`() {
        val note = Note(
            id = "note-1",
            title = "Kotlin 기초",
            content = "좋은 언어입니다",
            visibility = Visibility.PUBLIC,
            tags = setOf("kotlin"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        repository.save(note)

        val result = repository.searchByKeyword("kotlin")

        assertEquals(1, result.size)
        assertEquals("note-1", result[0].id)
    }

    @Test
    fun `searchByKeyword finds notes by content`() {
        val note = Note(
            id = "note-2",
            title = "Spring Boot",
            content = "마이크로서비스 프레임워크",
            visibility = Visibility.PRIVATE,
            tags = setOf("spring"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        repository.save(note)

        val result = repository.searchByKeyword("마이크로")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchByKeyword finds notes by tag`() {
        val note = Note(
            id = "note-3",
            title = "Architecture",
            content = "Clean Architecture",
            visibility = Visibility.PUBLIC,
            tags = setOf("clean", "architecture"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        repository.save(note)

        val result = repository.searchByKeyword("clean")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchByKeyword finds notes by aiSummary`() {
        val note = Note(
            id = "note-3-summary",
            title = "Async note",
            content = "original english content",
            visibility = Visibility.PUBLIC,
            tags = emptySet(),
            aiSummary = "비동기 처리 핵심만 한국어로 정리",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        repository.save(note)

        val result = repository.searchByKeyword("비동기 처리")

        assertEquals(1, result.size)
        assertEquals("note-3-summary", result[0].id)
    }

    @Test
    fun `searchByKeyword is case insensitive`() {
        val note = Note(
            id = "note-4",
            title = "KOTLIN",
            content = "content",
            visibility = Visibility.PUBLIC,
            tags = emptySet(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        repository.save(note)

        val result = repository.searchByKeyword("kotlin")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchByKeyword returns empty when no match`() {
        repository.save(
            Note(
                id = "note-5",
                title = "Java",
                content = "Old but gold",
                visibility = Visibility.PUBLIC,
                tags = setOf("java"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        val result = repository.searchByKeyword("python")

        assertEquals(0, result.size)
    }
}

