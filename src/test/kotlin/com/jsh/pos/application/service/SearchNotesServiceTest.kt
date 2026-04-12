package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHit
import com.jsh.pos.application.model.PageResult
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteSearchPort
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

    private val searchPort = FakeNoteSearchPort()
    private val searchNotesService = SearchNotesService(searchPort)

    @Test
    fun `search trims keyword and delegates to search port`() {
        val expected = listOf(
            NoteSearchHit(note = sampleNote(id = "note-1", title = "kotlin", content = "content", tags = setOf("spring"))),
        )
        searchPort.nextResult = PageResult(
            items = expected,
            page = 1,
            size = 10,
            totalElements = 11,
            totalPages = 2,
            hasPrevious = true,
            hasNext = true,
        )

        val result = searchNotesService.search(
            SearchNotesUseCase.Command(ownerUsername = "pos-admin", keyword = "  kotlin  ", sort = "relevance", page = 1, size = 10),
        )

        assertEquals("kotlin", searchPort.lastKeyword)
        assertEquals("pos-admin", searchPort.lastOwnerUsername)
        assertEquals("relevance", searchPort.lastSort)
        assertEquals(1, searchPort.lastPage)
        assertEquals(10, searchPort.lastSize)
        assertEquals(expected, result.hits)
        assertEquals(11, result.totalElements)
    }

    @Test
    fun `search throws when keyword is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            searchNotesService.search(
                SearchNotesUseCase.Command(ownerUsername = "pos-admin", keyword = "   ", sort = "recent", page = 0, size = 20),
            )
        }
    }

    private class FakeNoteSearchPort : NoteSearchPort {
        var lastOwnerUsername: String? = null
        var lastKeyword: String? = null
        var lastSort: String? = null
        var lastPage: Int? = null
        var lastSize: Int? = null
        var nextResult: PageResult<NoteSearchHit> = PageResult(
            items = emptyList(),
            page = 0,
            size = 20,
            totalElements = 0,
            totalPages = 0,
            hasPrevious = false,
            hasNext = false,
        )

        override fun search(ownerUsername: String, keyword: String, sort: String, page: Int, size: Int): PageResult<NoteSearchHit> {
            lastOwnerUsername = ownerUsername
            lastKeyword = keyword
            lastSort = sort
            lastPage = page
            lastSize = size
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

