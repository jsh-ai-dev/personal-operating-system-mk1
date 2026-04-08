package com.jsh.pos.application.port.out

import com.jsh.pos.domain.note.Note

/**
 * 노트 목록/검색 결과 캐시 포트입니다.
 *
 * 애플리케이션은 "어떻게 캐시하는지(Redis, 메모리, no-op)"를 몰라도 되고,
 * 구현체는 infrastructure 계층에서 제공합니다.
 */
interface NoteListCachePort {
    fun getOrLoad(query: Query, loader: () -> List<Note>): List<Note>

    fun evictOwner(ownerUsername: String)

    // 기본 구현은 owner 전체 무효화로 폴백합니다.
    fun evictOwnerModes(ownerUsername: String, modes: Set<Mode>) {
        evictOwner(ownerUsername)
    }

    enum class Mode {
        ALL,
        SEARCH,
        BOOKMARKED,
    }

    data class Query(
        val ownerUsername: String,
        val keyword: String,
        val bookmarkedOnly: Boolean,
        val sort: String,
    )
}

