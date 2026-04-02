package com.jsh.pos.adapter.out.persistence.jpa

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant

/**
 * 데이터베이스에 저장될 노트의 JPA 엔티티입니다.
 *
 * 왜 도메인 Note를 그대로 @Entity로 만들지 않았나?
 * - 도메인 계층은 Spring/JPA 같은 기술 세부사항을 몰라야 하기 때문입니다.
 * - Clean Architecture 관점에서 도메인은 가장 안쪽에 있고,
 *   JPA 엔티티는 바깥쪽(adapter/out/persistence)에 두는 편이 경계가 깔끔합니다.
 * - 이렇게 분리해 두면 나중에 저장소 기술이 바뀌어도 도메인 모델은 안정적으로 유지됩니다.
 *
 * 설계 포인트:
 * - id는 애플리케이션에서 UUID 문자열을 만들어 넘기므로 @GeneratedValue를 쓰지 않습니다.
 * - tags는 별도 테이블(note_tags)에 저장합니다.
 *   지금은 단순 문자열 컬렉션이므로 @ElementCollection이 가장 이해하기 쉽습니다.
 * - content는 메모 본문이라 길어질 수 있으므로 TEXT 컬럼으로 지정했습니다.
 */
@Entity
@Table(name = "notes")
class NoteJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    val id: String,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    val visibility: Visibility,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "note_tags",
        joinColumns = [JoinColumn(name = "note_id")],
    )
    @Column(name = "tag", nullable = false, length = 50)
    @OrderBy("asc")
    val tags: Set<String>,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
) {
    /**
     * JPA 엔티티를 도메인 Note로 변환합니다.
     *
     * 주의:
     * - 여기서는 검증을 다시 하지 않습니다.
     * - 이미 DB에 저장된 값은 애플리케이션이 과거에 검증한 결과라고 보고,
     *   저장된 값을 그대로 도메인 객체로 복원합니다.
     */
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        visibility = visibility,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        /**
         * 도메인 Note를 JPA 엔티티로 변환합니다.
         *
         * save(create/update) 모두 이 팩토리 메서드를 사용하면
         * 매핑 규칙이 한 곳에 모여서 유지보수가 쉬워집니다.
         */
        fun fromDomain(note: Note): NoteJpaEntity = NoteJpaEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            visibility = note.visibility,
            tags = note.tags,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }
}

