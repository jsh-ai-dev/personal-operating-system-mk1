package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 노트 삭제 유스케이스 구현체입니다.
 *
 * 현재는 단순 위임 구조지만, 이후 다음 로직을 이 계층에 확장할 수 있습니다.
 * - 삭제 감사 로그 기록
 * - soft delete 정책 분기
 * - 캐시 무효화
 */
@Service
class DeleteNoteService(
    private val noteQueryPort: NoteQueryPort,
    private val noteCommandPort: NoteCommandPort,
    private val noteListCachePort: NoteListCachePort,
    private val noteSearchIndexPort: NoteSearchIndexPort,
) : DeleteNoteUseCase {

    override fun deleteById(id: String): Boolean {
        // [2-DELETE] 삭제 유스케이스 계층입니다.
        // 현재는 단순 위임이지만, 나중에 soft delete/감사로그가 들어갈 자리입니다.
        // [4-DELETE] 실제 삭제는 저장소 어댑터가 수행합니다.
        val existing = noteQueryPort.findById(id) ?: return false
        val deleted = noteCommandPort.deleteById(id)
        if (deleted) {
            runCatching { noteSearchIndexPort.deleteById(id) }
                .onFailure { ex ->
                    logger.warn("[note-search] index delete failed after delete. noteId={}, reason={}", id, ex.message)
                }
            noteListCachePort.evictOwner(existing.ownerUsername)
        }
        return deleted
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DeleteNoteService::class.java)
    }
}


