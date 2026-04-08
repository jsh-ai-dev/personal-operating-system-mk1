package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SaveNoteSummaryUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import com.jsh.pos.domain.note.Note
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class SaveNoteSummaryService(
    private val noteQueryPort: NoteQueryPort,
    private val noteCommandPort: NoteCommandPort,
    private val noteListCachePort: NoteListCachePort,
    private val noteSearchIndexPort: NoteSearchIndexPort,
    private val clock: Clock,
) : SaveNoteSummaryUseCase {

    override fun save(command: SaveNoteSummaryUseCase.Command): Note? {
        val existing = noteQueryPort.findById(command.id) ?: return null
        val updated = existing.updateSummary(command.summary, clock.instant())
        return noteCommandPort.save(updated).also {
            runCatching { noteSearchIndexPort.upsert(it) }
                .onFailure { ex ->
                    logger.warn("[note-search] index upsert failed after summary save. noteId={}, reason={}", it.id, ex.message)
                }
            noteListCachePort.evictOwner(it.ownerUsername)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SaveNoteSummaryService::class.java)
    }
}

