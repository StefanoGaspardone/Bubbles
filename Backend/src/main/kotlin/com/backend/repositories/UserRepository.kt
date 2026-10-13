package com.backend.repositories

import com.backend.models.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}