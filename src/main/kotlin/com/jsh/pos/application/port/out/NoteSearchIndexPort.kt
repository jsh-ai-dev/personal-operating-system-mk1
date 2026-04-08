package com.jsh.pos.application.port.out

import com.jsh.pos.domain.note.Note

/**
 * 노트 검색 인덱스 동기화 포트입니다.
 */
interface NoteSearchIndexPort {
    fun upsert(note: Note)

    fun deleteById(id: String)
}

