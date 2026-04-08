package com.jsh.pos.infrastructure.search

import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Elasticsearch 사용 시 기존 노트를 검색 인덱스로 백필합니다.
 */
@Component
class NoteSearchReindexRunner(
    private val noteQueryPort: NoteQueryPort,
    private val noteSearchIndexPort: NoteSearchIndexPort,
    @Value("\${pos.search.elasticsearch.enabled:false}")
    private val elasticsearchEnabled: Boolean,
    @Value("\${pos.search.elasticsearch.reindex-on-startup:true}")
    private val reindexOnStartup: Boolean,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (!elasticsearchEnabled || !reindexOnStartup) {
            return
        }

        val notes = noteQueryPort.findAll()
        var successCount = 0
        notes.forEach { note ->
            runCatching { noteSearchIndexPort.upsert(note) }
                .onSuccess { successCount += 1 }
                .onFailure { ex ->
                    logger.warn("[note-search] startup reindex failed. noteId={}, reason={}", note.id, ex.message)
                }
        }

        logger.info("[note-search] startup reindex completed. indexed={}/{}", successCount, notes.size)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(NoteSearchReindexRunner::class.java)
    }
}

