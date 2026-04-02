package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * SearchNotesService 단위 테스트입니다.
 */
class SearchNotesServiceTest {

    private val queryPort = FakeNoteQueryPort()
    private val searchNotesService = SearchNotesService(queryPort)

    @Test
    fun `search trims keyword and delegates to query port`() {
        val expected = listOf(
            sampleNote(id = "note-1", title = "kotlin", content = "content", tags = setOf("spring")),
        )
        queryPort.nextResult = expected

        val result = searchNotesService.search(SearchNotesUseCase.Command(keyword = "  kotlin  "))

        assertEquals("kotlin", queryPort.lastKeyword)
        assertEquals(expected, result)
    }

    @Test
    fun `search throws when keyword is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            searchNotesService.search(SearchNotesUseCase.Command(keyword = "   "))
        }
    }

    private class FakeNoteQueryPort : NoteQueryPort {
        var lastKeyword: String? = null
        var nextResult: List<Note> = emptyList()

        override fun findById(id: String): Note? = null

        override fun searchByKeyword(keyword: String): List<Note> {
            lastKeyword = keyword
            return nextResult
        }
    }

    private fun sampleNote(id: String, title: String, content: String, tags: Set<String>): Note = Note(
        id = id,
        title = title,
        content = content,
        visibility = Visibility.PRIVATE,
        tags = tags,
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
    )
}

