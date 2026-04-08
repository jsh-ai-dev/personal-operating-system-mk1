package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SaveNoteSummaryUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class SaveNoteSummaryService(
    private val noteQueryPort: NoteQueryPort,
    private val noteCommandPort: NoteCommandPort,
    private val noteListCachePort: NoteListCachePort,
    private val clock: Clock,
) : SaveNoteSummaryUseCase {

    override fun save(command: SaveNoteSummaryUseCase.Command): Note? {
        val existing = noteQueryPort.findById(command.id) ?: return null
        val updated = existing.updateSummary(command.summary, clock.instant())
        return noteCommandPort.save(updated).also {
            noteListCachePort.evictOwner(it.ownerUsername)
        }
    }
}

