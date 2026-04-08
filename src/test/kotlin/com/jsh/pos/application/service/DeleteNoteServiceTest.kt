package com.jsh.pos.application.service

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * DeleteNoteService 단위 테스트입니다.
 *
 * 목표:
 * - 삭제 성공/실패 여부가 서비스에서 그대로 전달되는지 검증
 * - 외부 저장소 구현 없이 서비스 로직만 빠르게 확인
 */
class DeleteNoteServiceTest {

    private val repository = RecordingNoteRepository()
    private val noteListCachePort = RecordingNoteListCachePort()
    private val noteSearchIndexPort = RecordingNoteSearchIndexPort()
    private val deleteNoteService = DeleteNoteService(repository, repository, noteListCachePort, noteSearchIndexPort)

    @Test
    fun `deleteById returns true when repository deletes`() {
        repository.note = sampleNote("note-1")
        repository.nextDeleteResult = true

        val result = deleteNoteService.deleteById("note-1")

        assertTrue(result)
        assertEquals("note-1", noteSearchIndexPort.lastDeletedId)
        assertEquals("pos-admin", noteListCachePort.lastEvictedOwner)
    }

    @Test
    fun `deleteById returns false when repository does not find target`() {
        repository.note = null

        val result = deleteNoteService.deleteById("missing-note")

        assertFalse(result)
    }

    private class RecordingNoteRepository : NoteCommandPort, NoteQueryPort {
        var note: Note? = null
        var nextDeleteResult: Boolean = false

        override fun save(note: Note): Note = note

        override fun deleteById(id: String): Boolean = nextDeleteResult

        override fun findById(id: String): Note? = note
    }

    private class RecordingNoteListCachePort : NoteListCachePort {
        var lastEvictedOwner: String? = null

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun evictOwner(ownerUsername: String) {
            lastEvictedOwner = ownerUsername
        }
    }

    private class RecordingNoteSearchIndexPort : NoteSearchIndexPort {
        var lastDeletedId: String? = null

        override fun upsert(note: Note) = Unit

        override fun deleteById(id: String) {
            lastDeletedId = id
        }
    }

    private fun sampleNote(id: String): Note = Note(
        id = id,
        ownerUsername = "pos-admin",
        title = "title",
        content = "content",
        visibility = Visibility.PRIVATE,
        tags = emptySet(),
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
    )
}

