package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.out.NoteCommandPort
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
    private val noteCommandPort: NoteCommandPort,
) : DeleteNoteUseCase {

    override fun deleteById(id: String): Boolean {
        return noteCommandPort.deleteById(id)
    }
}

