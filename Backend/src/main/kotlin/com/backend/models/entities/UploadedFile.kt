package com.backend.models.entities

import com.backend.models.enums.FileOwnerType
import com.backend.models.enums.FileVisibility
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "uploaded_files")
class UploadedFile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, updatable = false)
    var ownerUser: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30, updatable = false)
    var ownerType: FileOwnerType,

    @Column(name = "owner_id", nullable = false, updatable = false)
    var ownerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10, updatable = false)
    var visibility: FileVisibility,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "object_key", nullable = false, unique = true, length = 512, updatable = false)
    var objectKey: String,

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    var contentType: String,

    @Column(name = "size", nullable = false, updatable = false)
    var size: Long,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {

    fun isPublic(): Boolean =
        visibility == FileVisibility.PUBLIC

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as UploadedFile
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "UploadedFile(id=$id, ownerType=$ownerType, ownerId=$ownerId, contentType=$contentType, size=$size)"
}