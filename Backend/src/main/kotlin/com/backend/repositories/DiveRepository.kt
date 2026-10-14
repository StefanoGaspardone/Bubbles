package com.backend.repositories

import com.backend.models.entities.Dive
import com.backend.models.projections.DiveNumberProjection
import com.backend.models.projections.SiteDiveCountProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface DiveRepository: JpaRepository<Dive, UUID> {

    @EntityGraph(attributePaths = ["site"])
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Optional<Dive>

    @EntityGraph(attributePaths = ["site"])
    @Query(
        value = """
            SELECT d FROM Dive d
            WHERE d.owner.id = :ownerId
              AND (:siteId IS NULL OR d.site.id = :siteId)
              AND (:from IS NULL OR d.startedAt >= :from)
              AND (:to IS NULL OR d.startedAt < :to)
            ORDER BY d.startedAt DESC, d.id DESC
        """,
        countQuery = """
            SELECT COUNT(d) FROM Dive d
            WHERE d.owner.id = :ownerId
              AND (:siteId IS NULL OR d.site.id = :siteId)
              AND (:from IS NULL OR d.startedAt >= :from)
              AND (:to IS NULL OR d.startedAt < :to)
        """
    )
    fun search(
        @Param("ownerId") ownerId: UUID,
        @Param("siteId") siteId: UUID?,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        pageable: Pageable,
    ): Page<Dive>

    @Query(
        value = """
            SELECT n.id AS "id", n.dive_number AS "diveNumber"
            FROM (
                SELECT d.id, ROW_NUMBER() OVER (ORDER BY d.started_at, d.id) AS dive_number
                FROM dives d
                WHERE d.owner_id = :ownerId
            ) n
            WHERE n.id IN (:ids)
        """,
        nativeQuery = true
    )
    fun findDiveNumbers(@Param("ownerId") ownerId: UUID, @Param("ids") ids: Collection<UUID>): List<DiveNumberProjection>

    @Query(
        value = """
            SELECT COUNT(*)
            FROM dives d
            WHERE d.owner_id = :ownerId
              AND (d.started_at, d.id) <= (SELECT t.started_at, t.id FROM dives t WHERE t.id = :diveId)
        """,
        nativeQuery = true
    )
    fun findDiveNumber(@Param("ownerId") ownerId: UUID, @Param("diveId") diveId: UUID): Long

    fun findFirstByOwnerIdAndStartedAtLessThanOrderByStartedAtDesc(ownerId: UUID, startedAt: Instant): Optional<Dive>

    @Query(
        """
        SELECT COUNT(d) > 0 FROM Dive d
        WHERE d.owner.id = :ownerId
          AND d.startedAt < :endedAt
          AND d.endedAt > :startedAt
          AND (:excludeId IS NULL OR d.id <> :excludeId)
        """
    )
    fun existsOverlapping(
        @Param("ownerId") ownerId: UUID,
        @Param("startedAt") startedAt: Instant,
        @Param("endedAt") endedAt: Instant,
        @Param("excludeId") excludeId: UUID?,
    ): Boolean

    fun countByOwnerId(ownerId: UUID): Long

    fun countBySiteIdAndOwnerId(siteId: UUID, ownerId: UUID): Long

    fun countBySiteId(siteId: UUID): Long

    @Query(
        """
        SELECT d.site.id AS siteId, COUNT(d) AS diveCount
        FROM Dive d
        WHERE d.owner.id = :ownerId AND d.site.id IN :siteIds
        GROUP BY d.site.id
        """
    )
    fun countDivesBySite(@Param("ownerId") ownerId: UUID, @Param("siteIds") siteIds: Collection<UUID>): List<SiteDiveCountProjection>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Dive d WHERE d.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: UUID): Int
}