package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service

/**
 * 노트 조회 유스케이스의 구현체입니다.
 *
 * 특징:
 * - 매우 간단한 구현: 포트에 그대로 위임
 * - 하지만 인터페이스 사이에 한 계층을 두는 이유가 있음:
 *   1. 나중에 조회 로직이 복잡해질 때 서비스 계층에 로직 추가 가능
 *   2. 테스트에서 GetNoteUseCase를 mock으로 대체 가능
 *   3. 컨트롤러는 구현 세부(NoteQueryPort)를 몰라도 됨
 *
 * 의존성:
 * - NoteQueryPort: 저장소에서 조회하는 책임 (adapter/out에서 구현)
 */
@Service
class GetNoteService(
    // port.out 주입: 조회 로직
    private val noteQueryPort: NoteQueryPort,
) : GetNoteUseCase {

    override fun getById(id: String): Note? {
        // 저장소에 위임
        // null이 반환되면 "존재하지 않음"을 의미
        return noteQueryPort.findById(id)
    }
}


