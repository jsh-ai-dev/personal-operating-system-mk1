package com.jsh.pos.adapter.out.search.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.application.model.NoteSearchHit
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.application.port.out.NoteSearchIndexPort
import com.jsh.pos.application.port.out.NoteSearchPort
import com.jsh.pos.domain.note.Note
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.StringQuery
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

/**
 * Elasticsearch 우선 검색 어댑터입니다.
 *
 * - Elasticsearch 비활성/오류 시 DB 검색으로 폴백합니다.
 * - 인덱스 동기화는 best-effort로 수행합니다.
 */
@Component
class ElasticsearchNoteSearchAdapter(
    private val noteQueryPort: NoteQueryPort,
    private val objectMapper: ObjectMapper,
    private val elasticsearchOperationsProvider: ObjectProvider<ElasticsearchOperations>,
    @Value("\${pos.search.elasticsearch.enabled:false}")
    private val elasticsearchEnabled: Boolean,
    @Value("\${pos.search.elasticsearch.index-name:notes-v1}")
    private val indexName: String,
) : NoteSearchPort, NoteSearchIndexPort {

    override fun search(ownerUsername: String, keyword: String, sort: String): List<NoteSearchHit> {
        val normalizedOwner = ownerUsername.trim().ifBlank { "anonymousUser" }
        val normalizedKeyword = keyword.trim()
        val normalizedSort = sort.trim().lowercase()
        if (normalizedKeyword.isBlank()) {
            return emptyList()
        }

        if (!elasticsearchEnabled) {
            return fallbackSearch(normalizedOwner, normalizedKeyword, normalizedSort)
        }

        val operations = elasticsearchOperationsProvider.getIfAvailable()
            ?: return fallbackSearch(normalizedOwner, normalizedKeyword, normalizedSort)

        return try {
            val query = buildSearchQuery(normalizedOwner, normalizedKeyword, normalizedSort)
            val hits: SearchHits<NoteSearchDocument> =
                operations.search(query, NoteSearchDocument::class.java, IndexCoordinates.of(indexName))
            hits.searchHits.map { hit ->
                NoteSearchHit(
                    note = hit.content.toDomain(),
                    highlight = extractHighlight(hit),
                )
            }
        } catch (e: Exception) {
            logger.warn("[note-search] Elasticsearch search failed, fallback to DB. reason={}", e.message)
            fallbackSearch(normalizedOwner, normalizedKeyword, normalizedSort)
        }
    }

    override fun upsert(note: Note) {
        if (!elasticsearchEnabled) {
            return
        }
        val operations = elasticsearchOperationsProvider.getIfAvailable() ?: return
        try {
            operations.save(NoteSearchDocument.from(note), IndexCoordinates.of(indexName))
        } catch (e: Exception) {
            logger.warn("[note-search] Elasticsearch upsert failed. noteId={}, reason={}", note.id, e.message)
        }
    }

    override fun deleteById(id: String) {
        if (!elasticsearchEnabled) {
            return
        }
        val operations = elasticsearchOperationsProvider.getIfAvailable() ?: return
        try {
            operations.delete(id, IndexCoordinates.of(indexName))
        } catch (e: Exception) {
            logger.warn("[note-search] Elasticsearch delete failed. noteId={}, reason={}", id, e.message)
        }
    }

    private fun fallbackSearch(ownerUsername: String, keyword: String, sort: String): List<NoteSearchHit> {
        val filtered = noteQueryPort.searchByKeyword(keyword)
            .filter { it.ownerUsername == ownerUsername }

        val sorted = when (sort) {
            "title" -> filtered.sortedBy { it.title.lowercase() }
            // relevance는 DB fallback에서 동일 점수 기반 정렬이 불가능하므로 기본 검색 순서를 유지합니다.
            "relevance" -> filtered
            else -> filtered.sortedByDescending { it.updatedAt }
        }

        return sorted.map { NoteSearchHit(note = it) }
    }

    private fun buildSearchQuery(ownerUsername: String, keyword: String, sort: String): StringQuery {
        val ownerJson = objectMapper.writeValueAsString(ownerUsername)
        val keywordJson = objectMapper.writeValueAsString(keyword)
        val sortJson = if (sort == "recent") {
            """
            ,
              "sort": [
                { "updatedAt": { "order": "desc" } }
              ]
            """.trimIndent()
        } else {
            ""
        }

        val highlightJson = """
            ,
              "highlight": {
                "pre_tags": ["<mark>"],
                "post_tags": ["</mark>"],
                "fields": {
                  "title": {},
                  "aiSummary": {
                    "fragment_size": 120,
                    "number_of_fragments": 1
                  },
                  "content": {
                    "fragment_size": 160,
                    "number_of_fragments": 1
                  }
                }
              }
        """.trimIndent()

        return StringQuery(
            """
            {
              "bool": {
                "must": [
                  { "term": { "ownerUsername": $ownerJson } }
                ],
                "should": [
                  { "match": { "title": { "query": $keywordJson, "boost": 4.0 } } },
                  { "match": { "tags": { "query": $keywordJson, "boost": 3.0 } } },
                  { "match": { "aiSummary": { "query": $keywordJson, "boost": 2.0 } } },
                  { "match": { "content": { "query": $keywordJson } } }
                ],
                "minimum_should_match": 1
              }$sortJson$highlightJson
            }
            """.trimIndent(),
        )
    }

    private fun extractHighlight(hit: SearchHit<NoteSearchDocument>): NoteSearchHighlight {
        val fields = hit.highlightFields
        return NoteSearchHighlight(
            title = fields["title"]?.firstOrNull()?.let { sanitizeSnippet(it) },
            summary = fields["aiSummary"]?.firstOrNull()?.let { sanitizeSnippet(it) },
            content = fields["content"]?.firstOrNull()?.let { sanitizeSnippet(it) },
        )
    }

    private fun sanitizeSnippet(raw: String): String {
        val escaped = HtmlUtils.htmlEscape(raw)
        return escaped
            .replace("&lt;mark&gt;", "<mark>")
            .replace("&lt;/mark&gt;", "</mark>")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ElasticsearchNoteSearchAdapter::class.java)
    }
}

