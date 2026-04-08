package com.jsh.pos.application.service

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * BookmarkNoteService의 단위 테스트입니다.
 *
 * 테스트 범위:
 * 1. bookmark() - 북마크 등록 (존재하는 노트, 없는 노트)
 * 2. unbookmark() - 북마크 해제 (존재하는 노트, 없는 노트)
 * 3. updatedAt이 변경되지 않아야 함 (북마크는 내용 수정이 아님)
 * 4. GetBookmarkedNotesService - 북마크 목록 조회
 */
class BookmarkNoteServiceTest {

    // 인메모리 포트: 두 포트를 동시에 구현해 의존성 연결
    private val store = InMemoryNoteStore()
    private val noteListCachePort = RecordingNoteListCachePort()
    private val noteSearchIndexPort = RecordingNoteSearchIndexPort()
    private val bookmarkNoteService = BookmarkNoteService(store, store, noteListCachePort, noteSearchIndexPort)
    private val getBookmarkedNotesService = GetBookmarkedNotesService(store)

    /**
     * 테스트: 존재하는 노트를 북마크하면 bookmarked=true가 되는가?
     */
    @Test
    fun `bookmark sets bookmarked to true`() {
        store.save(makeNote(id = "note-1", bookmarked = false))

        val result = bookmarkNoteService.bookmark("note-1")

        assertNotNull(result)
        assertTrue(result!!.bookmarked)
        assertEquals("note-1", noteSearchIndexPort.lastUpsertedId)
        assertEquals("anonymousUser", noteListCachePort.lastEvictedOwner)
    }

    /**
     * 테스트: 북마크해도 updatedAt이 바뀌지 않는가?
     *
     * 북마크는 내용 수정이 아니라 즐겨찾기 표시이므로
     * 수정 시간이 바뀌면 정렬 기준이 의도치 않게 달라집니다.
     */
    @Test
    fun `bookmark does not change updatedAt`() {
        val originalUpdatedAt = Instant.parse("2026-04-03T00:00:00Z")
        store.save(makeNote(id = "note-2", updatedAt = originalUpdatedAt))

        val result = bookmarkNoteService.bookmark("note-2")

        assertEquals(originalUpdatedAt, result!!.updatedAt)
    }

    /**
     * 테스트: 존재하지 않는 노트를 북마크하면 null을 반환하는가?
     */
    @Test
    fun `bookmark returns null when note not found`() {
        val result = bookmarkNoteService.bookmark("nonexistent-id")
        assertNull(result)
    }

    /**
     * 테스트: 북마크된 노트의 북마크를 해제하면 bookmarked=false가 되는가?
     */
    @Test
    fun `unbookmark sets bookmarked to false`() {
        store.save(makeNote(id = "note-3", bookmarked = true))

        val result = bookmarkNoteService.unbookmark("note-3")

        assertNotNull(result)
        assertFalse(result!!.bookmarked)
        assertEquals("note-3", noteSearchIndexPort.lastUpsertedId)
        assertEquals("anonymousUser", noteListCachePort.lastEvictedOwner)
    }

    /**
     * 테스트: 존재하지 않는 노트를 unbookmark하면 null을 반환하는가?
     */
    @Test
    fun `unbookmark returns null when note not found`() {
        val result = bookmarkNoteService.unbookmark("nonexistent-id")
        assertNull(result)
    }

    /**
     * 테스트: 북마크된 노트만 목록에 반환되는가?
     */
    @Test
    fun `getBookmarked returns only bookmarked notes`() {
        store.save(makeNote(id = "note-a", bookmarked = true))
        store.save(makeNote(id = "note-b", bookmarked = false))
        store.save(makeNote(id = "note-c", bookmarked = true))

        val result = getBookmarkedNotesService.getBookmarked()

        // 북마크된 노트만 2개 반환
        assertEquals(2, result.size)
        // 반환된 노트는 모두 bookmarked=true
        assertTrue(result.all { it.bookmarked })
        // note-b(북마크 아님)는 포함되지 않음
        assertFalse(result.any { it.id == "note-b" })
    }

    // ─── 테스트용 헬퍼 ───────────────────────────────────────────────────────

    private fun makeNote(
        id: String,
        bookmarked: Boolean = false,
        updatedAt: Instant = Instant.parse("2026-04-03T00:00:00Z"),
    ): Note = Note(
        id = id,
        title = "테스트 노트",
        content = "내용",
        visibility = Visibility.PRIVATE,
        tags = emptySet(),
        bookmarked = bookmarked,
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = updatedAt,
    )

    /**
     * 테스트용 인메모리 저장소입니다.
     *
     * NoteCommandPort와 NoteQueryPort를 동시에 구현합니다.
     * - 실제 DB 없이 BookmarkNoteService와 GetBookmarkedNotesService를 모두 테스트 가능
     */
    private class InMemoryNoteStore : NoteCommandPort, NoteQueryPort {
        private val notes = mutableMapOf<String, Note>()

        override fun save(note: Note): Note {
            notes[note.id] = note
            return note
        }

        override fun findById(id: String): Note? = notes[id]

        override fun deleteById(id: String): Boolean = notes.remove(id) != null

        override fun findAllBookmarked(): List<Note> =
            notes.values.filter { it.bookmarked }.sortedByDescending { it.updatedAt }
    }

    private class RecordingNoteListCachePort : NoteListCachePort {
        var lastEvictedOwner: String? = null

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun evictOwner(ownerUsername: String) {
            lastEvictedOwner = ownerUsername
        }
    }

    private class RecordingNoteSearchIndexPort : NoteSearchIndexPort {
        var lastUpsertedId: String? = null

        override fun upsert(note: Note) {
            lastUpsertedId = note.id
        }

        override fun deleteById(id: String) = Unit
    }
}


