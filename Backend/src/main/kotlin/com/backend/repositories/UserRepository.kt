package com.backend.repositories

import com.backend.models.entities.User
import com.backend.models.enums.AccountStatus
import com.backend.models.projections.UserAuthProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    @Query("SELECT u.accountStatus AS accountStatus, u.role AS role FROM User u WHERE u.id = :id")
    fun findAuthViewById(@Param("id") id: UUID): UserAuthProjection?

    @Query("SELECT u.diveNumberOffset FROM User u WHERE u.id = :id")
    fun findDiveNumberOffset(@Param("id") id: UUID): Int?
}