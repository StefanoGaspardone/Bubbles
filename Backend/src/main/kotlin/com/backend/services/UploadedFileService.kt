package com.backend.services

import com.backend.exceptions.EmptyFileException
import com.backend.exceptions.FileTooLargeException
import com.backend.exceptions.InvalidFileTypeException
import com.backend.exceptions.UploadedFileNotFoundException
import com.backend.models.dtos.StoredFileContent
import com.backend.models.dtos.UploadedFileDTO
import com.backend.models.entities.UploadedFile
import com.backend.models.enums.FileFormat
import com.backend.models.enums.FileOwnerType
import com.backend.properties.S3Properties
import com.backend.repositories.UploadedFileRepository
import com.backend.repositories.UserRepository
import com.backend.utils.FileTypeDetector
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.util.*

sealed interface FileDelivery {
    data class Redirect(val url: String): FileDelivery
    data class Stream(val content: StoredFileContent): FileDelivery
}

@Service
class UploadedFileService(
    private val uploadedFileRepository: UploadedFileRepository,
    private val userRepository: UserRepository,
    private val storageService: StorageService,
    private val s3Properties: S3Properties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_FILE_NAME_LENGTH = 255
        val UNSAFE_FILE_NAME_CHARS = Regex("[\\p{Cntrl}\"\\\\/]")

        const val CONTENT_PATH = "/api/v1/files/%s/content"
    }

    @Transactional
    fun uploadFile(
        ownerUserId: UUID,
        ownerType: FileOwnerType,
        ownerId: UUID,
        file: MultipartFile,
        allowedFormats: Set<FileFormat> = ownerType.allowedFormats,
    ): UploadedFileDTO {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][upload_file] Uploading file\n\townerType={}\n\townerId={}\n\tsize={}", ownerType, ownerId, file.size)

        try {
            require(ownerType.allowedFormats.containsAll(allowedFormats)) { "Formats not allowed for $ownerType: $allowedFormats" }
            val format = validate(ownerType, file, allowedFormats)
            val objectKey = buildObjectKey(ownerType, ownerId, format)

            file.inputStream.use { stream ->
                storageService.putObject(objectKey, stream, file.size, format.mimeType)
            }

            afterRollback { deleteObjectQuietly(objectKey) }

            val uploadedFile = uploadedFileRepository.save(
                UploadedFile(
                    // Riferimento senza query: l'utente esiste, lo garantisce il filtro JWT
                    ownerUser = userRepository.getReferenceById(ownerUserId),
                    ownerType = ownerType,
                    ownerId = ownerId,
                    visibility = ownerType.visibility,
                    fileName = sanitizeFileName(file.originalFilename, format),
                    objectKey = objectKey,
                    contentType = format.mimeType,
                    size = file.size,
                )
            )

            logger.info("\n\t[INFO] [uploaded_file_service][upload_file] File uploaded\n\townerType={}\n\townerId={}\n\tfileId={}", ownerType, ownerId, uploadedFile.id)
            return toDTO(uploadedFile)
        } catch(e: EmptyFileException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][upload_file] Empty file\n\townerType={}\n\townerId={}", ownerType, ownerId)
            throw e
        } catch(e: FileTooLargeException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][upload_file] File too large\n\townerType={}\n\townerId={}\n\tsize={}", ownerType, ownerId, file.size)
            throw e
        } catch(e: InvalidFileTypeException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][upload_file] Unsupported file type\n\townerType={}\n\townerId={}\n\tdeclaredType={}", ownerType, ownerId, file.contentType)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][upload_file] Error uploading file\n\townerType={}\n\townerId={}\n\treason={}", ownerType, ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun listFiles(ownerType: FileOwnerType, ownerId: UUID): List<UploadedFileDTO> {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][list_files] Listing files\n\townerType={}\n\townerId={}", ownerType, ownerId)

        try {
            val files = uploadedFileRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId)

            logger.debug("\n\t[DEBUG] [uploaded_file_service][list_files] Retrieved files\n\townerType={}\n\townerId={}\n\tcount={}", ownerType, ownerId, files.size)
            return files.map(::toDTO)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][list_files] Error listing files\n\townerType={}\n\townerId={}\n\treason={}", ownerType, ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun countFiles(ownerType: FileOwnerType, ownerId: UUID): Long =
        uploadedFileRepository.countByOwnerTypeAndOwnerId(ownerType, ownerId)

    @Transactional(readOnly = true)
    fun getFile(requesterId: UUID, fileId: UUID): UploadedFileDTO {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][get_file] Retrieving file\n\tfileId={}\n\trequesterId={}", fileId, requesterId)

        try {
            return toDTO(findAccessibleFile(requesterId, fileId))
        } catch(e: UploadedFileNotFoundException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][get_file] File not found or not accessible\n\tfileId={}\n\trequesterId={}", fileId, requesterId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][get_file] Error retrieving file\n\tfileId={}\n\treason={}", fileId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun deliverFile(requesterId: UUID?, fileId: UUID): FileDelivery {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][deliver_file] Delivering file\n\tfileId={}\n\trequesterId={}", fileId, requesterId)

        try {
            val uploadedFile = findAccessibleFile(requesterId, fileId)

            if(uploadedFile.isPublic()) {
                return FileDelivery.Redirect(publicUrl(uploadedFile))
            }

            val stream = storageService.getObject(uploadedFile.objectKey)
            return FileDelivery.Stream(
                StoredFileContent(
                    resource = InputStreamResource(stream),
                    fileName = uploadedFile.fileName,
                    contentType = uploadedFile.contentType,
                    size = uploadedFile.size,
                )
            )
        } catch(e: UploadedFileNotFoundException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][deliver_file] File not found or not accessible\n\tfileId={}\n\trequesterId={}", fileId, requesterId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][deliver_file] Error delivering file\n\tfileId={}\n\treason={}", fileId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteFile(ownerUserId: UUID, fileId: UUID) {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][delete_file] Deleting file\n\tfileId={}\n\townerUserId={}", fileId, ownerUserId)

        try {
            if(!deleteFileIfPresent(ownerUserId, fileId)) {
                throw UploadedFileNotFoundException(fileId)
            }
        } catch(e: UploadedFileNotFoundException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][delete_file] File not found\n\tfileId={}\n\townerUserId={}", fileId, ownerUserId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][delete_file] Error deleting file\n\tfileId={}\n\treason={}", fileId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteResourceFile(ownerUserId: UUID, ownerType: FileOwnerType, ownerId: UUID, fileId: UUID) {
        logger.debug("\n\t[DEBUG] [uploaded_file_service][delete_resource_file] Deleting file\n\tfileId={}\n\townerType={}\n\townerId={}", fileId, ownerType, ownerId)

        try {
            val uploadedFile = uploadedFileRepository
                .findByIdAndOwnerUserIdAndOwnerTypeAndOwnerId(fileId, ownerUserId, ownerType, ownerId)
                .orElseThrow { UploadedFileNotFoundException(fileId) }
            val objectKey = uploadedFile.objectKey

            uploadedFileRepository.delete(uploadedFile)
            afterCommit { deleteObjectQuietly(objectKey) }

            logger.info("\n\t[INFO] [uploaded_file_service][delete_resource_file] File deleted\n\tfileId={}", fileId)
        } catch(e: UploadedFileNotFoundException) {
            logger.warn("\n\t[WARN] [uploaded_file_service][delete_resource_file] File not found for resource\n\tfileId={}\n\townerType={}\n\townerId={}", fileId, ownerType, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][delete_resource_file] Error deleting file\n\tfileId={}\n\treason={}", fileId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteFileIfPresent(ownerUserId: UUID, fileId: UUID): Boolean {
        val uploadedFile = uploadedFileRepository.findByIdAndOwnerUserId(fileId, ownerUserId).orElse(null)
            ?: return false
        val objectKey = uploadedFile.objectKey

        uploadedFileRepository.delete(uploadedFile)
        afterCommit { deleteObjectQuietly(objectKey) }

        logger.info("\n\t[INFO] [uploaded_file_service][delete_file_if_present] File deleted\n\tfileId={}", fileId)
        return true
    }

    @Transactional
    fun deleteAllForOwner(ownerType: FileOwnerType, ownerId: UUID): Int {
        val objectKeys = uploadedFileRepository.findObjectKeysByOwner(ownerType, ownerId)
        if(objectKeys.isEmpty()) return 0

        uploadedFileRepository.deleteByOwner(ownerType, ownerId)
        afterCommit { deleteObjectsQuietly(objectKeys) }

        logger.info("\n\t[INFO] [uploaded_file_service][delete_all_for_owner] Files deleted\n\townerType={}\n\townerId={}\n\tcount={}", ownerType, ownerId, objectKeys.size)
        return objectKeys.size
    }

    @Transactional
    fun deleteAllForUser(ownerUserId: UUID): Int {
        val objectKeys = uploadedFileRepository.findObjectKeysByOwnerUser(ownerUserId)
        if(objectKeys.isEmpty()) return 0

        uploadedFileRepository.deleteByOwnerUser(ownerUserId)
        afterCommit { deleteObjectsQuietly(objectKeys) }

        logger.info("\n\t[INFO] [uploaded_file_service][delete_all_for_user] Files deleted\n\townerUserId={}\n\tcount={}", ownerUserId, objectKeys.size)
        return objectKeys.size
    }

    private fun validate(ownerType: FileOwnerType, file: MultipartFile, allowedFormats: Set<FileFormat>): FileFormat {
        if(file.isEmpty) {
            throw EmptyFileException()
        }
        if(file.size > ownerType.maxSizeBytes) {
            throw FileTooLargeException(ownerType.maxSizeBytes)
        }

        val header = file.inputStream.use { it.readNBytes(FileTypeDetector.HEADER_SIZE) }
        val format = FileTypeDetector.detect(header)

        if(format == null || format !in allowedFormats) {
            val allowed = allowedFormats.joinToString(", ") { it.extension.uppercase() }
            throw InvalidFileTypeException("Unsupported file type: allowed formats are $allowed")
        }

        return format
    }

    private fun buildObjectKey(ownerType: FileOwnerType, ownerId: UUID, format: FileFormat): String =
        "${ownerType.visibility.keyPrefix}/${ownerType.keySegment}/$ownerId/${UUID.randomUUID()}.${format.extension}"

    private fun sanitizeFileName(originalName: String?, format: FileFormat): String {
        val baseName = originalName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(UNSAFE_FILE_NAME_CHARS, "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "file.${format.extension}"

        return baseName.take(MAX_FILE_NAME_LENGTH)
    }

    private fun publicUrl(file: UploadedFile): String =
        "${s3Properties.publicEndpoint.trimEnd('/')}/${s3Properties.bucket}/${file.objectKey}"

    private fun toDTO(file: UploadedFile): UploadedFileDTO =
        UploadedFileDTO.from(file, CONTENT_PATH.format(file.id))

    private fun findAccessibleFile(requesterId: UUID?, fileId: UUID): UploadedFile {
        val uploadedFile = uploadedFileRepository.findById(fileId)
            .orElseThrow { UploadedFileNotFoundException(fileId) }

        if(!uploadedFile.isPublic() && (requesterId == null || uploadedFile.ownerUser.id != requesterId)) {
            throw UploadedFileNotFoundException(fileId)
        }

    return uploadedFile
    }

    private fun deleteObjectQuietly(objectKey: String) {
        try {
            storageService.deleteObject(objectKey)
        } catch(_: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][delete_object_quietly] Orphan object left in storage\n\tobjectKey={}", objectKey)
        }
    }

    private fun deleteObjectsQuietly(objectKeys: List<String>) {
        try {
            objectKeys.chunked(1000).forEach { storageService.deleteObjects(it) }
        } catch(_: Exception) {
            logger.error("\n\t[ERROR] [uploaded_file_service][delete_objects_quietly] Orphan objects left in storage\n\tcount={}", objectKeys.size)
        }
    }

    private fun afterCommit(action: () -> Unit) {
        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {
            override fun afterCommit() = action()
        })
    }

    private fun afterRollback(action: () -> Unit) {
        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if(status == TransactionSynchronization.STATUS_ROLLED_BACK) action()
            }
        })
    }
}