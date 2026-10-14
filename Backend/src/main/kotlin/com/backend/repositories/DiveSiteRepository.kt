package com.backend.repositories

import com.backend.models.entities.DiveSite
import com.backend.models.enums.DiveSiteType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DiveSiteRepository : JpaRepository<DiveSite, UUID> {

    @Query("SELECT s FROM DiveSite s WHERE s.id = :id AND (s.owner IS NULL OR s.owner.id = :userId)")
    fun findVisibleById(@Param("id") id: UUID, @Param("userId") userId: UUID): Optional<DiveSite>

    @Query(
        value = """
            SELECT s FROM DiveSite s
            WHERE ((:includeMine = TRUE AND s.owner.id = :userId) OR (:includePublic = TRUE AND s.owner IS NULL))
              AND (LOWER(s.name) LIKE :pattern ESCAPE '\' OR LOWER(COALESCE(s.locality, '')) LIKE :pattern ESCAPE '\')
              AND (:siteType IS NULL OR s.siteType = :siteType)
            ORDER BY LOWER(s.name), s.id
        """,
        countQuery = """
            SELECT COUNT(s) FROM DiveSite s
            WHERE ((:includeMine = TRUE AND s.owner.id = :userId) OR (:includePublic = TRUE AND s.owner IS NULL))
              AND (LOWER(s.name) LIKE :pattern ESCAPE '\' OR LOWER(COALESCE(s.locality, '')) LIKE :pattern ESCAPE '\')
              AND (:siteType IS NULL OR s.siteType = :siteType)
        """
    )
    fun search(
        @Param("userId") userId: UUID,
        @Param("pattern") pattern: String,
        @Param("siteType") siteType: DiveSiteType?,
        @Param("includeMine") includeMine: Boolean,
        @Param("includePublic") includePublic: Boolean,
        pageable: Pageable,
    ): Page<DiveSite>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiveSite s WHERE s.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: UUID): Int
}