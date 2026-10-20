package com.backend.services

import com.backend.exceptions.StorageDeleteException
import com.backend.exceptions.StorageNotFoundException
import com.backend.exceptions.StorageReadException
import com.backend.exceptions.StorageWriteException
import com.backend.properties.S3Properties
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.RemoveObjectsArgs
import io.minio.errors.ErrorResponseException
import io.minio.messages.DeleteObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class StorageService(
    private val s3Client: MinioClient,
    private val properties: S3Properties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        val NOT_FOUND_CODES = setOf("NoSuchKey", "NoSuchObject")
    }

    fun putObject(objectKey: String, input: InputStream, size: Long, contentType: String) {
        logger.debug("\n\t[DEBUG] [storage_service][put_object] Starting storage\n\tobjectKey={}\n\tsize={}", objectKey, size)

        try {
            s3Client.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket)
                    .`object`(objectKey)
                    .stream(input, size, -1)
                    .contentType(contentType)
                    .build()
            )
            logger.info("\n\t[INFO] [storage_service][put_object] Successful storage\n\tobjectKey={}", objectKey)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [storage_service][put_object] Failed storage\n\tobjectKey={}", objectKey, e)
            throw StorageWriteException(objectKey)
        }
    }

    fun getObject(objectKey: String): InputStream {
        logger.debug("\n\t[DEBUG] [storage_service][get_object] Starting retrieval\n\tobjectKey={}", objectKey)

        try {
            val result = s3Client.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.bucket)
                    .`object`(objectKey)
                    .build()
            )
            logger.info("\n\t[INFO] [storage_service][get_object] Successful retrieval\n\tobjectKey={}", objectKey)
            return result
        } catch(e: ErrorResponseException) {
            if(e.errorResponse().code() in NOT_FOUND_CODES) {
                logger.warn("\n\t[WARN] [storage_service][get_object] Object not found\n\tobjectKey={}", objectKey)
                throw StorageNotFoundException(objectKey)
            }

            logger.error("\n\t[ERROR] [storage_service][get_object] Failed retrieval\n\tobjectKey={}", objectKey, e)
            throw StorageReadException(objectKey)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [storage_service][get_object] Failed retrieval\n\tobjectKey={}", objectKey, e)
            throw StorageReadException(objectKey)
        }
    }

    fun deleteObject(objectKey: String) {
        logger.debug("\n\t[DEBUG] [storage_service][delete_object] Starting deletion\n\tobjectKey={}", objectKey)

        try {
            s3Client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.bucket)
                    .`object`(objectKey)
                    .build()
            )
            logger.info("\n\t[INFO] [storage_service][delete_object] Successful deletion\n\tobjectKey={}", objectKey)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [storage_service][delete_object] Failed deletion\n\tobjectKey={}", objectKey, e)
            throw StorageDeleteException(objectKey)
        }
    }

    fun deleteObjects(objectKeys: Collection<String>) {
        if(objectKeys.isEmpty()) return

        require(objectKeys.size <= 1000) { "At most 1000 objects can be deleted in a single request" }
        logger.debug("\n\t[DEBUG] [storage_service][delete_objects] Starting deletion\n\tcount={}", objectKeys.size)

        val failedKeys = mutableListOf<String>()

        try {
            val results = s3Client.removeObjects(
                RemoveObjectsArgs.builder()
                    .bucket(properties.bucket)
                    .objects(objectKeys.map { DeleteObject(it) })
                    .build()
            )

            for(result in results) {
                val error = result.get()
                failedKeys += error.objectName()

                logger.error("\n\t[ERROR] [storage_service][delete_objects] Failed deletion\n\tobjectKey={}\n\terror={}", error.objectName(), error.message())
            }
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [storage_service][delete_objects] Failed bulk deletion\n\tcount={}", objectKeys.size, e)
            throw StorageDeleteException(objectKeys.joinToString(", "))
        }

        if(failedKeys.isNotEmpty()) {
            throw StorageDeleteException(failedKeys.joinToString(", "))
        }

        logger.info("\n\t[INFO] [storage_service][delete_objects] Successful deletion\n\tcount={}", objectKeys.size)
    }
}