package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * UpdateNoteService 단위 테스트입니다.
 */
class UpdateNoteServiceTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-04-02T12:00:00Z"), ZoneOffset.UTC)
    private val fakeRepository = FakeNoteRepository()
    private val noteListCachePort = RecordingNoteListCachePort()
    private val updateNoteService = UpdateNoteService(fakeRepository, fakeRepository, noteListCachePort, fixedClock)

    @Test
    fun `updateById updates existing note and keeps createdAt`() {
        val existing = Note.create(
            id = "note-1",
            title = "old",
            content = "old-content",
            visibility = Visibility.PRIVATE,
            tags = setOf("old"),
            now = Instant.parse("2026-04-01T00:00:00Z"),
        )
        fakeRepository.save(existing)

        val result = updateNoteService.updateById(
            id = "note-1",
            command = UpdateNoteUseCase.Command(
                title = "  new title  ",
                content = "  new content  ",
                visibility = Visibility.PUBLIC,
                tags = setOf(" kotlin ", " "),
            ),
        )

        assertNotNull(result)
        assertEquals("new title", result!!.title)
        assertEquals("new content", result.content)
        assertEquals(Visibility.PUBLIC, result.visibility)
        assertEquals(setOf("kotlin"), result.tags)
        assertEquals(existing.createdAt, result.createdAt)
        assertEquals(Instant.parse("2026-04-02T12:00:00Z"), result.updatedAt)
        assertEquals("anonymousUser", noteListCachePort.lastEvictedOwner)
    }

    @Test
    fun `updateById returns null when target note does not exist`() {
        val result = updateNoteService.updateById(
            id = "missing",
            command = UpdateNoteUseCase.Command(
                title = "title",
                content = "content",
                visibility = Visibility.PRIVATE,
            ),
        )

        assertNull(result)
    }

    private class FakeNoteRepository : NoteCommandPort, NoteQueryPort {
        private val store = mutableMapOf<String, Note>()

        override fun save(note: Note): Note {
            store[note.id] = note
            return note
        }

        override fun findById(id: String): Note? = store[id]

        override fun deleteById(id: String): Boolean = store.remove(id) != null
    }

    private class RecordingNoteListCachePort : NoteListCachePort {
        var lastEvictedOwner: String? = null

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun evictOwner(ownerUsername: String) {
            lastEvictedOwner = ownerUsername
        }
    }
}



