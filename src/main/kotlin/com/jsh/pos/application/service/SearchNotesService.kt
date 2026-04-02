package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service

/**
 * 노트 검색 유스케이스 구현체입니다.
 *
 * 현재 단계에서는 검색어 정제/검증과 포트 위임까지만 담당합니다.
 */
@Service
class SearchNotesService(
    private val noteQueryPort: NoteQueryPort,
) : SearchNotesUseCase {

    override fun search(command: SearchNotesUseCase.Command): List<Note> {
        val keyword = command.keyword.trim()
        require(keyword.isNotBlank()) { "검색어는 비워둘 수 없습니다" }

        return noteQueryPort.searchByKeyword(keyword)
    }
}

