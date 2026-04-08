package com.jsh.pos.infrastructure.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class RedisNoteListCacheAdapter(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: NoteListCacheProperties,
) : NoteListCachePort {

    override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> {
        val mode = resolveMode(query)
        val ownerSegment = ownerSegment(query.ownerUsername)
        val key = buildCacheKey(query)
        val cachedJson = stringRedisTemplate.opsForValue().get(key)
        if (!cachedJson.isNullOrBlank()) {
            val cachedNotes = objectMapper.readValue(cachedJson, NOTE_LIST_TYPE_REFERENCE)
            logger.info(
                "[note-list-cache] HIT owner={} mode={} sort={} key={} count={}",
                query.ownerUsername,
                mode,
                query.sort,
                key,
                cachedNotes.size,
            )
            return cachedNotes
        }

        logger.info(
            "[note-list-cache] MISS owner={} mode={} sort={} key={}",
            query.ownerUsername,
            mode,
            query.sort,
            key,
        )
        val loadedNotes = loader()
        stringRedisTemplate.opsForValue().set(
            key,
            objectMapper.writeValueAsString(loadedNotes),
            properties.ttl,
        )
        stringRedisTemplate.opsForSet().add(indexKey(ownerSegment, mode), key)
        logger.info(
            "[note-list-cache] PUT owner={} mode={} sort={} key={} count={} ttl={}s",
            query.ownerUsername,
            mode,
            query.sort,
            key,
            loadedNotes.size,
            properties.ttl.seconds,
        )
        return loadedNotes
    }

    override fun evictOwner(ownerUsername: String) {
        evictOwnerModes(ownerUsername, NoteListCachePort.Mode.entries.toSet())
    }

    override fun evictOwnerModes(ownerUsername: String, modes: Set<NoteListCachePort.Mode>) {
        val normalizedOwner = ownerUsername.trim().ifBlank { "anonymousUser" }
        val ownerSegment = ownerSegment(normalizedOwner)
        val targetModes = modes.ifEmpty { NoteListCachePort.Mode.entries.toSet() }
        val keysToDelete = linkedSetOf<String>()

        targetModes.forEach { mode ->
            val members = stringRedisTemplate.opsForSet().members(indexKey(ownerSegment, mode)).orEmpty()
            if (members.isNotEmpty()) {
                keysToDelete.addAll(members)
            }
            stringRedisTemplate.delete(indexKey(ownerSegment, mode))
        }

        if (keysToDelete.isNotEmpty()) {
            stringRedisTemplate.delete(keysToDelete)
        }

        logger.info(
            "[note-list-cache] EVICT owner={} modes={} keyCount={}",
            normalizedOwner,
            targetModes.joinToString(",") { it.name.lowercase() },
            keysToDelete.size,
        )
    }

    private fun buildCacheKey(query: NoteListCachePort.Query): String {
        val ownerSegment = ownerSegment(query.ownerUsername)
        val mode = resolveMode(query)
        val keywordHash = hashKeyword(query.keyword)
        val sort = query.sort.trim().lowercase().ifBlank { "recent" }
        return "${properties.keyPrefix}$ownerSegment:${mode.name.lowercase()}:$sort:$keywordHash"
    }

    private fun resolveMode(query: NoteListCachePort.Query): NoteListCachePort.Mode = when {
        query.keyword.isNotBlank() -> NoteListCachePort.Mode.SEARCH
        query.bookmarkedOnly -> NoteListCachePort.Mode.BOOKMARKED
        else -> NoteListCachePort.Mode.ALL
    }

    private fun ownerSegment(ownerUsername: String): String =
        encodeSegment(ownerUsername.trim().ifBlank { "anonymousUser" })

    private fun indexKey(ownerSegment: String, mode: NoteListCachePort.Mode): String =
        "${properties.keyPrefix}idx:$ownerSegment:${mode.name.lowercase()}"

    private fun encodeSegment(value: String): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun hashKeyword(keyword: String): String {
        if (keyword.isBlank()) {
            return "none"
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(keyword.trim().lowercase().toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(16)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RedisNoteListCacheAdapter::class.java)
        val NOTE_LIST_TYPE_REFERENCE = object : TypeReference<List<Note>>() {}
    }
}

