package com.backend.repositories

import com.backend.models.entities.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE RefreshToken t
        SET t.revokedAt = :now, t.updatedAt = :now
        WHERE t.user.id = :userId AND t.revokedAt IS NULL
        """
    )
    fun revokeAllByUserId(@Param("userId") userId: UUID, @Param("now") now: Instant): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE RefreshToken t
        SET t.revokedAt = :now, t.updatedAt = :now
        WHERE t.familyId = :familyId AND t.revokedAt IS NULL
        """
    )
    fun revokeFamily(@Param("familyId") familyId: UUID, @Param("now") now: Instant): Int

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :threshold")
    fun deleteExpiredBefore(@Param("threshold") threshold: Instant): Int
}