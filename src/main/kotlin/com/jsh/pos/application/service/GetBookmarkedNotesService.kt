package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service

/**
 * 북마크된 노트 목록 조회 유스케이스 구현체입니다.
 *
 * 현재는 단순 위임 구조이지만, 이후 다음 로직을 이 계층에 확장할 수 있습니다:
 * - 공개된 북마크만 필터링 (visibility 조건 추가)
 * - 페이징(Pageable) 적용
 * - 캐시 레이어(Redis) 연동
 */
@Service
class GetBookmarkedNotesService(
    private val noteQueryPort: NoteQueryPort,
) : GetBookmarkedNotesUseCase {

    override fun getBookmarked(): List<Note> =
        // 저장소에서 북마크된 노트를 최신 등록순으로 가져옴
        noteQueryPort.findAllBookmarked()
}
