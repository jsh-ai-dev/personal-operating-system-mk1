package com.jsh.pos.application.port.out

import com.jsh.pos.application.model.NoteSearchHit

/**
 * 노트 검색 포트입니다.
 *
 * 검색 구현체는 Elasticsearch 같은 전문 검색 엔진이나
 * DB LIKE 기반 대체 구현으로 교체할 수 있습니다.
 */
interface NoteSearchPort {
    fun search(ownerUsername: String, keyword: String, sort: String): List<NoteSearchHit>
}

