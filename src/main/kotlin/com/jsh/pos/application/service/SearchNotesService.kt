package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHit
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteSearchPort
import org.springframework.stereotype.Service

/**
 * 노트 검색 유스케이스 구현체입니다.
 *
 * 현재 단계에서는 검색어 정제/검증과 포트 위임까지만 담당합니다.
 */
@Service
class SearchNotesService(
    private val noteSearchPort: NoteSearchPort,
) : SearchNotesUseCase {

    override fun search(command: SearchNotesUseCase.Command): List<NoteSearchHit> {
        // [2-SEARCH] 검색 유스케이스 계층입니다.
        // 여기서는 검색어를 정제(trim)하고, 공백 검색어를 막는 규칙을 적용합니다.
        val keyword = command.keyword.trim()
        require(keyword.isNotBlank()) { "검색어는 비워둘 수 없습니다" }

        // [4-SEARCH] 실제 검색은 저장소 어댑터에 위임합니다.
        return noteSearchPort.search(
            ownerUsername = command.ownerUsername,
            keyword = keyword,
            sort = command.sort,
        )
    }
}


