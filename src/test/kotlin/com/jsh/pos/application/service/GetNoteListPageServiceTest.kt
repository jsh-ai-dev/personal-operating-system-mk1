package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.application.model.NoteSearchHit
import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GetNoteListPageServiceTest {

    private val searchNotesUseCase = FakeSearchNotesUseCase()
    private val getBookmarkedNotesUseCase = FakeGetBookmarkedNotesUseCase()
    private val getAllNotesUseCase = FakeGetAllNotesUseCase()
    private val noteListCachePort = InMemoryNoteListCachePort()

    private val service = GetNoteListPageService(
        searchNotesUseCase = searchNotesUseCase,
        getBookmarkedNotesUseCase = getBookmarkedNotesUseCase,
        getAllNotesUseCase = getAllNotesUseCase,
        noteListCachePort = noteListCachePort,
    )

    @Test
    fun `get caches repeated all-note list lookups`() {
        getAllNotesUseCase.notes = listOf(
            sampleNote(id = "note-2", title = "B", ownerUsername = "pos-admin", updatedAt = Instant.parse("2026-04-01T00:00:00Z")),
            sampleNote(id = "note-1", title = "A", ownerUsername = "pos-admin", updatedAt = Instant.parse("2026-04-02T00:00:00Z")),
        )

        val command = GetNoteListPageUseCase.Command(
            ownerUsername = "pos-admin",
            keyword = null,
            bookmarkedOnly = false,
            sort = "recent",
        )

        val first = service.get(command)
        val second = service.get(command)

        assertEquals(1, getAllNotesUseCase.callCount)
        assertEquals(listOf("note-1", "note-2"), first.notes.map { it.id })
        assertEquals(first, second)
    }

    @Test
    fun `get filters by owner and sorts title search results`() {
        searchNotesUseCase.notes = listOf(
            NoteSearchHit(note = sampleNote(id = "note-3", title = "zeta", ownerUsername = "another-user")),
            NoteSearchHit(
                note = sampleNote(id = "note-2", title = "zeta", ownerUsername = "pos-admin"),
                highlight = NoteSearchHighlight(content = "...<mark>kotlin</mark>..."),
            ),
            NoteSearchHit(note = sampleNote(id = "note-1", title = "alpha", ownerUsername = "pos-admin")),
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = "  kotlin  ",
                bookmarkedOnly = false,
                sort = "title",
            ),
        )

        assertEquals("kotlin", searchNotesUseCase.lastKeyword)
        assertEquals("title", searchNotesUseCase.lastSort)
        assertEquals(listOf("note-1", "note-2"), result.notes.map { it.id })
        assertEquals("...<mark>kotlin</mark>...", result.highlightsById["note-2"]?.content)
        assertEquals("title", result.sort)
        assertEquals("kotlin", result.keyword)
    }

    @Test
    fun `get preserves search order when sort is relevance`() {
        searchNotesUseCase.notes = listOf(
            NoteSearchHit(note = sampleNote(id = "note-2", title = "zeta", ownerUsername = "pos-admin")),
            NoteSearchHit(note = sampleNote(id = "note-1", title = "alpha", ownerUsername = "pos-admin")),
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = "kotlin",
                bookmarkedOnly = false,
                sort = "relevance",
            ),
        )

        assertEquals("relevance", searchNotesUseCase.lastSort)
        assertEquals(listOf("note-2", "note-1"), result.notes.map { it.id })
        assertEquals("relevance", result.sort)
    }

    @Test
    fun `get builds fallback highlight when search engine highlight is missing`() {
        searchNotesUseCase.notes = listOf(
            NoteSearchHit(
                note = sampleNote(
                    id = "note-1",
                    title = "Kotlin note",
                    ownerUsername = "pos-admin",
                    content = "Spring + kotlin + redis",
                ),
            ),
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = "kotlin",
                bookmarkedOnly = false,
                sort = "recent",
            ),
        )

        assertTrue(result.highlightsById["note-1"]?.title?.contains("<mark>", ignoreCase = true) == true)
    }

    private class FakeSearchNotesUseCase : SearchNotesUseCase {
        var lastKeyword: String? = null
        var lastSort: String? = null
        var notes: List<NoteSearchHit> = emptyList()

        override fun search(command: SearchNotesUseCase.Command): List<NoteSearchHit> {
            lastKeyword = command.keyword
            lastSort = command.sort
            return notes
        }
    }

    private class FakeGetBookmarkedNotesUseCase : GetBookmarkedNotesUseCase {
        var notes: List<Note> = emptyList()

        override fun getBookmarked(): List<Note> = notes
    }

    private class FakeGetAllNotesUseCase : GetAllNotesUseCase {
        var callCount: Int = 0
        var notes: List<Note> = emptyList()

        override fun getAll(): List<Note> {
            callCount += 1
            return notes
        }
    }

    private class InMemoryNoteListCachePort : NoteListCachePort {
        private val store = mutableMapOf<NoteListCachePort.Query, List<Note>>()

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> =
            store.getOrPut(query) { loader() }

        override fun evictOwner(ownerUsername: String) {
            store.keys.removeIf { it.ownerUsername == ownerUsername }
        }
    }

    private fun sampleNote(
        id: String,
        title: String,
        ownerUsername: String,
        content: String = "content",
        updatedAt: Instant = Instant.parse("2026-04-01T00:00:00Z"),
    ): Note = Note(
        id = id,
        ownerUsername = ownerUsername,
        title = title,
        content = content,
        visibility = Visibility.PRIVATE,
        tags = emptySet(),
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = updatedAt,
    )
}


