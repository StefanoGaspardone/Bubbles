package com.backend.repositories

import com.backend.models.entities.DiveBuddy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DiveBuddyRepository: JpaRepository<DiveBuddy, UUID> {

    @Query(
        """
        SELECT b FROM DiveBuddy b
        JOIN FETCH b.dive d
        JOIN FETCH d.owner
        JOIN FETCH d.site
        WHERE d.id = :diveId AND b.buddyUser.id = :userId
        """
    )
    fun findForConfirmation(@Param("diveId") diveId: UUID, @Param("userId") userId: UUID): Optional<DiveBuddy>
}