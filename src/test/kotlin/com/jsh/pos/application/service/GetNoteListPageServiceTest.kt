package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.application.model.NoteSearchHit
import com.jsh.pos.application.model.PageResult
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GetNoteListPageServiceTest {

    private val searchNotesUseCase = FakeSearchNotesUseCase()
    private val noteQueryPort = FakeNoteQueryPort()
    private val noteListCachePort = InMemoryNoteListCachePort()

    private val service = GetNoteListPageService(
        searchNotesUseCase = searchNotesUseCase,
        noteQueryPort = noteQueryPort,
        noteListCachePort = noteListCachePort,
    )

    @Test
    fun `get delegates all-note paging to query port`() {
        noteQueryPort.allResult = PageResult(
            items = listOf(sampleNote(id = "note-11", title = "N11", ownerUsername = "pos-admin")),
            page = 1,
            size = 10,
            totalElements = 25,
            totalPages = 3,
            hasPrevious = true,
            hasNext = true,
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = 1,
                size = 10,
            ),
        )

        assertEquals("pos-admin", noteQueryPort.lastAllOwner)
        assertEquals(1, noteQueryPort.lastAllPage)
        assertEquals(10, noteQueryPort.lastAllSize)
        assertEquals(listOf("note-11"), result.notes.map { it.id })
        assertEquals(25, result.totalElements)
        assertEquals(3, result.totalPages)
    }

    @Test
    fun `get caches non-search result by page and size`() {
        noteQueryPort.allResult = PageResult(
            items = listOf(sampleNote(id = "note-1", title = "n1", ownerUsername = "pos-admin")),
            page = 0,
            size = 10,
            totalElements = 11,
            totalPages = 2,
            hasPrevious = false,
            hasNext = true,
        )

        service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = 0,
                size = 10,
            ),
        )
        service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = 0,
                size = 10,
            ),
        )

        assertEquals(1, noteQueryPort.findPageByOwnerCallCount)

        service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = 1,
                size = 10,
            ),
        )
        assertEquals(2, noteQueryPort.findPageByOwnerCallCount)
    }

    @Test
    fun `get delegates bookmarked paging to query port`() {
        noteQueryPort.bookmarkedResult = PageResult(
            items = listOf(sampleNote(id = "note-bm-1", title = "북마크", ownerUsername = "pos-admin", content = "x")),
            page = 0,
            size = 5,
            totalElements = 1,
            totalPages = 1,
            hasPrevious = false,
            hasNext = false,
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = true,
                sort = "title",
                page = 0,
                size = 5,
            ),
        )

        assertEquals("pos-admin", noteQueryPort.lastBookmarkedOwner)
        assertEquals("title", noteQueryPort.lastBookmarkedSort)
        assertEquals(5, noteQueryPort.lastBookmarkedSize)
        assertEquals(listOf("note-bm-1"), result.notes.map { it.id })
    }

    @Test
    fun `get delegates paged search and keeps highlight`() {
        searchNotesUseCase.nextResult = SearchNotesUseCase.Result(
            hits = listOf(
                NoteSearchHit(
                    note = sampleNote(id = "note-2", title = "zeta", ownerUsername = "pos-admin", content = "kotlin"),
                highlight = NoteSearchHighlight(content = "...<mark>kotlin</mark>..."),
            ),
            ),
            page = 2,
            size = 10,
            totalElements = 21,
            totalPages = 3,
            hasPrevious = true,
            hasNext = false,
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = "  kotlin  ",
                bookmarkedOnly = false,
                sort = "relevance",
                page = 2,
                size = 10,
            ),
        )

        assertEquals("kotlin", searchNotesUseCase.lastKeyword)
        assertEquals("relevance", searchNotesUseCase.lastSort)
        assertEquals(2, searchNotesUseCase.lastPage)
        assertEquals(10, searchNotesUseCase.lastSize)
        assertEquals(listOf("note-2"), result.notes.map { it.id })
        assertEquals("...<mark>kotlin</mark>...", result.highlightsById["note-2"]?.content)
        assertEquals("relevance", result.sort)
        assertEquals("kotlin", result.keyword)
        assertEquals(21, result.totalElements)
    }

    @Test
    fun `get builds fallback highlight when search engine highlight is missing`() {
        searchNotesUseCase.nextResult = SearchNotesUseCase.Result(
            hits = listOf(
                NoteSearchHit(
                    note = sampleNote(
                        id = "note-1",
                        title = "Kotlin note",
                        ownerUsername = "pos-admin",
                        content = "Spring + kotlin + redis",
                    ),
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
            hasPrevious = false,
            hasNext = false,
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

    @Test
    fun `get returns requested page slice and metadata`() {
        noteQueryPort.allResult = PageResult(
            items = (11..20).map { index ->
                sampleNote(
                    id = "note-$index",
                    title = "note-$index",
                    ownerUsername = "pos-admin",
                )
            },
            page = 1,
            size = 10,
            totalElements = 25,
            totalPages = 3,
            hasPrevious = true,
            hasNext = true,
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = 1,
                size = 10,
            ),
        )

        assertEquals(10, result.notes.size)
        assertEquals(listOf("note-11", "note-12", "note-13", "note-14", "note-15", "note-16", "note-17", "note-18", "note-19", "note-20"), result.notes.map { it.id })
        assertEquals(1, result.page)
        assertEquals(10, result.size)
        assertEquals(25, result.totalElements)
        assertEquals(3, result.totalPages)
        assertTrue(result.hasPrevious)
        assertTrue(result.hasNext)
    }

    @Test
    fun `get normalizes negative page and oversize page size`() {
        noteQueryPort.allResult = PageResult(
            items = (1..3).map { index ->
                sampleNote(id = "note-$index", title = "note-$index", ownerUsername = "pos-admin")
            },
            page = 0,
            size = GetNoteListPageUseCase.MAX_PAGE_SIZE,
            totalElements = 3,
            totalPages = 1,
            hasPrevious = false,
            hasNext = false,
        )

        val result = service.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = "pos-admin",
                keyword = null,
                bookmarkedOnly = false,
                sort = "recent",
                page = -5,
                size = 999,
            ),
        )

        assertEquals(0, result.page)
        assertEquals(GetNoteListPageUseCase.MAX_PAGE_SIZE, result.size)
        assertEquals(3, result.notes.size)
        assertEquals(1, result.totalPages)
        assertEquals(0, noteQueryPort.lastAllPage)
        assertEquals(GetNoteListPageUseCase.MAX_PAGE_SIZE, noteQueryPort.lastAllSize)
    }

    private class FakeSearchNotesUseCase : SearchNotesUseCase {
        var lastKeyword: String? = null
        var lastSort: String? = null
        var lastPage: Int? = null
        var lastSize: Int? = null
        var nextResult: SearchNotesUseCase.Result = SearchNotesUseCase.Result(
            hits = emptyList(),
            page = 0,
            size = 20,
            totalElements = 0,
            totalPages = 0,
            hasPrevious = false,
            hasNext = false,
        )

        override fun search(command: SearchNotesUseCase.Command): SearchNotesUseCase.Result {
            lastKeyword = command.keyword
            lastSort = command.sort
            lastPage = command.page
            lastSize = command.size
            return nextResult
        }
    }

    private class FakeNoteQueryPort : NoteQueryPort {
        var allResult: PageResult<Note> = PageResult(emptyList(), 0, 20, 0, 0, false, false)
        var bookmarkedResult: PageResult<Note> = PageResult(emptyList(), 0, 20, 0, 0, false, false)

        var lastAllOwner: String? = null
        var lastAllSort: String? = null
        var lastAllPage: Int? = null
        var lastAllSize: Int? = null

        var lastBookmarkedOwner: String? = null
        var lastBookmarkedSort: String? = null
        var lastBookmarkedPage: Int? = null
        var lastBookmarkedSize: Int? = null
        var findPageByOwnerCallCount: Int = 0

        override fun findById(id: String): Note? = null

        override fun findPageByOwner(ownerUsername: String, sort: String, page: Int, size: Int): PageResult<Note> {
            findPageByOwnerCallCount += 1
            lastAllOwner = ownerUsername
            lastAllSort = sort
            lastAllPage = page
            lastAllSize = size
            return allResult
        }

        override fun findBookmarkedPageByOwner(ownerUsername: String, sort: String, page: Int, size: Int): PageResult<Note> {
            lastBookmarkedOwner = ownerUsername
            lastBookmarkedSort = sort
            lastBookmarkedPage = page
            lastBookmarkedSize = size
            return bookmarkedResult
        }
    }

    private class InMemoryNoteListCachePort : NoteListCachePort {
        private val store = mutableMapOf<NoteListCachePort.Query, PageResult<Note>>()

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun getOrLoadPage(query: NoteListCachePort.Query, loader: () -> PageResult<Note>): PageResult<Note> =
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


