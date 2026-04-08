package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import com.jsh.pos.domain.note.Note
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 노트 수정 유스케이스 구현체입니다.
 */
@Service
class UpdateNoteService(
    private val noteQueryPort: NoteQueryPort,
    private val noteCommandPort: NoteCommandPort,
    private val noteListCachePort: NoteListCachePort,
    private val noteSearchIndexPort: NoteSearchIndexPort,
    private val clock: Clock,
) : UpdateNoteUseCase {

    override fun updateById(id: String, command: UpdateNoteUseCase.Command): Note? {
        // [2-PUT] 수정 유스케이스 계층입니다.
        // 먼저 기존 노트가 있는지 조회하고, 있으면 도메인 규칙으로 새 인스턴스를 만듭니다.
        // [4-PUT-1] 수정 전 원본 노트를 저장소에서 조회
        val existing = noteQueryPort.findById(id) ?: return null

        // [3-PUT] 실제 수정 규칙(공백 제거, updatedAt 갱신, createdAt 유지)은 도메인에서 처리
        val updated = existing.update(
            title = command.title,
            content = command.content,
            visibility = command.visibility,
            tags = command.tags,
            now = clock.instant(),
        )

        // [4-PUT-2] 수정된 결과를 저장소에 다시 저장
        return noteCommandPort.save(updated).also {
            runCatching { noteSearchIndexPort.upsert(it) }
                .onFailure { ex ->
                    logger.warn("[note-search] index upsert failed after update. noteId={}, reason={}", it.id, ex.message)
                }
            noteListCachePort.evictOwner(it.ownerUsername)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(UpdateNoteService::class.java)
    }
}


