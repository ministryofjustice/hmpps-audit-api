package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class LocalTempAuditFileOutput(private val path: Path) : OutputFile {

  override fun create(blockSizeHint: Long): PositionOutputStream {
    val outputStream = Files.newOutputStream(
      path,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING,
    )

    return object : PositionOutputStream() {
      private var position = 0L

      override fun write(b: Int) {
        outputStream.write(b)
        position++
      }

      override fun write(b: ByteArray, off: Int, len: Int) {
        outputStream.write(b, off, len)
        position += len
      }

      override fun getPos(): Long = position

      override fun close() {
        outputStream.close()
      }
    }
  }

  override fun createOrOverwrite(blockSizeHint: Long): PositionOutputStream = create(blockSizeHint)

  override fun supportsBlockSize(): Boolean = false

  override fun defaultBlockSize(): Long = 0
}
