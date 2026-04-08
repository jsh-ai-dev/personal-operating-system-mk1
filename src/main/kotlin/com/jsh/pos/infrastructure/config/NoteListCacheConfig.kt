package com.jsh.pos.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.infrastructure.cache.NoteListCacheProperties
import com.jsh.pos.infrastructure.cache.RedisNoteListCacheAdapter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
@EnableConfigurationProperties(NoteListCacheProperties::class)
class NoteListCacheConfig {

    @Bean
    @ConditionalOnBean(StringRedisTemplate::class)
    fun noteListCachePort(
        stringRedisTemplate: StringRedisTemplate,
        objectMapper: ObjectMapper,
        properties: NoteListCacheProperties,
    ): NoteListCachePort = RedisNoteListCacheAdapter(stringRedisTemplate, objectMapper, properties)

    @Bean
    @ConditionalOnMissingBean(NoteListCachePort::class)
    fun noOpNoteListCachePort(): NoteListCachePort = object : NoteListCachePort {
        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun evictOwner(ownerUsername: String) = Unit
    }
}

