package com.backend.repositories

import com.backend.models.entities.DiveSite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DiveSiteRepository: JpaRepository<DiveSite, UUID> {

    @Query("SELECT s FROM DiveSite s WHERE s.id = :id AND (s.owner IS NULL OR s.owner.id = :userId)")
    fun findVisibleById(@Param("id") id: UUID, @Param("userId") userId: UUID): Optional<DiveSite>

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Optional<DiveSite>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiveSite s WHERE s.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: UUID): Int
}