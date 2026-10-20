package com.backend.repositories

import com.backend.models.entities.UploadedFile
import com.backend.models.enums.FileOwnerType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UploadedFileRepository: JpaRepository<UploadedFile, UUID> {

    fun findByIdAndOwnerUserId(id: UUID, ownerUserId: UUID): Optional<UploadedFile>

    fun findAllByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType: FileOwnerType, ownerId: UUID): List<UploadedFile>

    fun countByOwnerTypeAndOwnerId(ownerType: FileOwnerType, ownerId: UUID): Long

    fun findByIdAndOwnerUserIdAndOwnerTypeAndOwnerId(id: UUID, ownerUserId: UUID, ownerType: FileOwnerType, ownerId: UUID): Optional<UploadedFile>

    @Query("SELECT f.objectKey FROM UploadedFile f WHERE f.ownerType = :ownerType AND f.ownerId = :ownerId")
    fun findObjectKeysByOwner(@Param("ownerType") ownerType: FileOwnerType, @Param("ownerId") ownerId: UUID): List<String>

    @Query("SELECT f.objectKey FROM UploadedFile f WHERE f.ownerUser.id = :ownerUserId")
    fun findObjectKeysByOwnerUser(@Param("ownerUserId") ownerUserId: UUID): List<String>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UploadedFile f WHERE f.ownerType = :ownerType AND f.ownerId = :ownerId")
    fun deleteByOwner(@Param("ownerType") ownerType: FileOwnerType, @Param("ownerId") ownerId: UUID): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UploadedFile f WHERE f.ownerUser.id = :ownerUserId")
    fun deleteByOwnerUser(@Param("ownerUserId") ownerUserId: UUID): Int
}