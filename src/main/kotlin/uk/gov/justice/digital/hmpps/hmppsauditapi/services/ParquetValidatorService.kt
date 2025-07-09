package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.io.File
import java.time.LocalDate

@Service
class ParquetValidatorService(
  private val s3Client: S3Client,
  @Value("\${aws.s3.auditBucketName}") private val bucketName: String,
) {
  private val results = mutableListOf<String>()

  fun getResults(): List<String> = results.toList()

  fun clearResults() {
    results.clear()
  }

  fun validateAsync() = CoroutineScope(Dispatchers.IO).launch {
    var continuationToken: String? = null

    do {
      val requestBuilder = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix("")
        .maxKeys(1000)

      if (continuationToken != null) {
        requestBuilder.continuationToken(continuationToken)
      }

      val response = s3Client.listObjectsV2(requestBuilder.build())

      response.contents()
        .filter { it.key().endsWith(".parquet") }
        .filter { obj ->
          val parts = obj.key().split("/")
          val year = parts.find { it.startsWith("year=") }?.substringAfter("=")?.toIntOrNull() ?: return@filter false
          val month = parts.find { it.startsWith("month=") }?.substringAfter("=")?.toIntOrNull() ?: return@filter false
          val day = parts.find { it.startsWith("day=") }?.substringAfter("=")?.toIntOrNull() ?: return@filter false
          val fileDate = LocalDate.of(year, month, day)
          fileDate >= LocalDate.of(2025, 5, 26)
        }
        .forEach { obj ->
          val key = obj.key()
          val tempFile = File.createTempFile("parquet-", ".parquet")
          try {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(), tempFile.toPath())
            val hadoopPath = org.apache.hadoop.fs.Path(tempFile.toURI().toString())
            AvroParquetReader.builder<GenericRecord>(hadoopPath)
              .withConf(Configuration())
              .build().use { reader ->
                reader.read()
                results.add("OK: $key")
              }
          } catch (ex: Exception) {
            results.add("Corrupt: $key — ${ex.message}")
          } finally {
            tempFile.delete()
          }
        }

      continuationToken = if (response.isTruncated) response.nextContinuationToken() else null
    } while (continuationToken != null)
  }
}
