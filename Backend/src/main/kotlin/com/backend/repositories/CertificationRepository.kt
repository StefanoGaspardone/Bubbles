package com.backend.repositories

import com.backend.models.entities.Certification
import com.backend.models.enums.CertificationAgency
import com.backend.models.enums.CertificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Repository
interface CertificationRepository: JpaRepository<Certification, UUID> {

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Optional<Certification>

    @Query(
        """
        SELECT c FROM Certification c
        WHERE c.owner.id = :ownerId
          AND (:agency IS NULL OR c.agency = :agency)
          AND (:certificationType IS NULL OR c.certificationType = :certificationType)
          AND (:includeExpired = TRUE OR c.expiryDate IS NULL OR c.expiryDate >= :today)
        ORDER BY c.issueDate DESC, c.courseName
        """
    )
    fun search(
        @Param("ownerId") ownerId: UUID,
        @Param("agency") agency: CertificationAgency?,
        @Param("certificationType") certificationType: CertificationType?,
        @Param("includeExpired") includeExpired: Boolean,
        @Param("today") today: LocalDate,
    ): List<Certification>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Certification c WHERE c.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: UUID): Int
}