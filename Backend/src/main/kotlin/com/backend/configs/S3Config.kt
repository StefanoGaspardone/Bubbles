package com.backend.configs

import com.backend.properties.S3Properties
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.SetBucketPolicyArgs
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class S3Config(private val properties: S3Properties, private val environment: Environment) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun s3Client(): MinioClient {
        val client = MinioClient.builder()
            .endpoint(properties.url)
            .credentials(properties.accessKey, properties.secretKey)
            .region(properties.region)
            .build()

        val isDev = environment.activeProfiles.isEmpty() || environment.activeProfiles.any { it in listOf("dev", "local") }
        if(isDev) {
            ensureBucketExists(client)
            applyPublicReadPolicy(client)
        } else {
            logger.info("\n\t[INFO] [s3_config] Skipping automatic bucket creation and policy setup for non-dev environment")
        }

        return client
    }

    private fun ensureBucketExists(client: MinioClient) {
        try {
            val exists = client.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(properties.bucket)
                    .build()
            )

            if(!exists) {
                try {
                    client.makeBucket(
                        MakeBucketArgs.builder()
                            .bucket(properties.bucket)
                            .build()
                    )
                    logger.info("\n\t[INFO] [s3_config][ensure_bucket_exists] Created S3 bucket\n\tbucket={}", properties.bucket)
                } catch(e: Exception) {
                    logger.info("\n\t[INFO] [s3_config][ensure_bucket_exists] Bucket already exists or could not be created: {}", e.message)
                }
            }
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [s3_config][ensure_bucket_exists] Error ensuring S3 bucket exists: {}", e.message)
            throw e
        }
    }

    private fun applyPublicReadPolicy(client: MinioClient) {
        try {
            client.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(properties.bucket)
                    .config(publicReadPolicy(properties.bucket))
                    .build()
            )
            logger.info("\n\t[INFO] [s3_config][apply_public_read_policy] Public read policy applied to public/*\n\tbucket={}", properties.bucket)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [s3_config][apply_public_read_policy] Error applying bucket policy: {}", e.message)
            throw e
        }
    }

    private fun publicReadPolicy(bucket: String): String = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": { "AWS": ["*"] },
              "Action": ["s3:GetObject"],
              "Resource": ["arn:aws:s3:::$bucket/public/*"]
            }
          ]
        }
    """.trimIndent()
}