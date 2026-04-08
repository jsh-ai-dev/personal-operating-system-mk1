package com.jsh.pos.infrastructure.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "pos.cache.note-list")
data class NoteListCacheProperties(
    val keyPrefix: String = "pos:notes:list:v1:",
    val ttl: Duration = Duration.ofMinutes(5),
)

