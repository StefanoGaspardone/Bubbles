package com.backend.repositories

import com.backend.models.entities.Gear
import com.backend.models.enums.GearCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface GearRepository: JpaRepository<Gear, UUID> {

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Optional<Gear>

    @Query(
        """
        SELECT g FROM Gear g
        WHERE g.owner.id = :ownerId
          AND (:category IS NULL OR g.category = :category)
          AND (:includeArchived = TRUE OR g.archivedAt IS NULL)
        ORDER BY g.category, g.name
        """
    )
    fun search(@Param("ownerId") ownerId: UUID, @Param("category") category: GearCategory?, @Param("includeArchived") includeArchived: Boolean): List<Gear>

    fun findAllByIdInAndOwnerId(ids: Collection<UUID>, ownerId: UUID): List<Gear>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Gear g WHERE g.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: UUID): Int
}