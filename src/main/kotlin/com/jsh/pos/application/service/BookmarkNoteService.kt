package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service

/**
 * 노트 북마크 유스케이스 구현체입니다.
 *
 * 북마크 처리 흐름:
 * 1. 저장소에서 기존 노트를 조회 (없으면 null 반환)
 * 2. 도메인 메서드(bookmark / unbookmark)를 호출해 새 Note 인스턴스 생성
 * 3. 저장소에 저장
 *
 * 왜 도메인에서 bookmark()를 호출하나?
 * - 비즈니스 규칙(예: 이미 북마크됐는지 등)이 나중에 도메인에 추가될 수 있기 때문
 * - 서비스 계층이 직접 flag를 바꾸면 규칙이 분산되고 추적이 어려워짐
 *
 * updatedAt을 갱신하지 않는 이유:
 * - 북마크는 "노트 내용 변경"이 아니라 "즐겨찾기 표시"에 해당
 * - 검색 결과나 정렬 순서를 북마크로 바꾸지 않기 위함
 */
@Service
class BookmarkNoteService(
    // 기존 노트 조회용
    private val noteQueryPort: NoteQueryPort,
    // 북마크 상태를 반영한 노트 저장용
    private val noteCommandPort: NoteCommandPort,
    private val noteListCachePort: NoteListCachePort,
) : BookmarkNoteUseCase {

    /**
     * 노트에 북마크를 등록합니다.
     *
     * 이미 북마크된 노트를 다시 북마크해도 같은 결과 (멱등)
     */
    override fun bookmark(id: String): Note? {
        // 1. 조회: 없으면 바로 null 반환 (컨트롤러가 404 처리)
        val note = noteQueryPort.findById(id) ?: return null

        // 2. 도메인 메서드로 북마크 상태 변경 (새 Note 인스턴스 생성)
        val bookmarked = note.bookmark()

        // 3. 저장 후 반환
        return noteCommandPort.save(bookmarked).also {
            noteListCachePort.evictOwner(it.ownerUsername)
        }
    }

    /**
     * 노트의 북마크를 해제합니다.
     *
     * 북마크되지 않은 노트를 해제해도 같은 결과 (멱등)
     */
    override fun unbookmark(id: String): Note? {
        val note = noteQueryPort.findById(id) ?: return null
        val unbookmarked = note.unbookmark()
        return noteCommandPort.save(unbookmarked).also {
            noteListCachePort.evictOwner(it.ownerUsername)
        }
    }
}

