package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service

/**
 * 전체 노트 목록 조회 유스케이스 구현체입니다.
 */
@Service
class GetAllNotesService(
    private val noteQueryPort: NoteQueryPort,
) : GetAllNotesUseCase {

    override fun getAll(): List<Note> = noteQueryPort.findAll()
}

