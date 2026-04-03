package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

/**
 * 전체 노트 목록 조회 유스케이스 계약입니다.
 *
 * 주로 웹 UI(목록 화면)에서 사용합니다.
 * 검색어 없이 전체를 조회해야 할 때 SearchNotesUseCase 대신 사용합니다.
 */
interface GetAllNotesUseCase {
    /**
     * 전체 노트를 최신 작성순으로 반환합니다.
     *
     * @return 전체 노트 목록 (없으면 빈 리스트)
     */
    fun getAll(): List<Note>
}

