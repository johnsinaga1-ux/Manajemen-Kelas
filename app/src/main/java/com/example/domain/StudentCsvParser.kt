package com.example.domain

import com.example.data.db.StudentEntity
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale

data class ParsedStudentCsvRow(
  val lineNumber: Int,
  val nis: String,
  val fullName: String,
  val gender: String,
  val parentContact: String,
  val entity: StudentEntity?,
  val errors: List<String>,
) {
  val isValid: Boolean get() = errors.isEmpty() && entity != null
  val errorMessage: String? get() = errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
}

data class StudentCsvParseResult(
  val totalDataRows: Int,
  val validEntities: List<StudentEntity>,
  val previewRows: List<ParsedStudentCsvRow>,
  val invalidCount: Int,
  val delimiterLabel: String,
  val usedHeaderMapping: Boolean,
)

object StudentCsvParser {

  private data class ColumnMapping(
    val nisIdx: Int,
    val nameIdx: Int,
    val genderIdx: Int,
    val contactIdx: Int,
    val hasHeader: Boolean,
  )

  fun parse(
    inputStream: InputStream,
    existingNisSet: Set<String> = emptySet(),
    charset: Charset = Charsets.UTF_8,
  ): StudentCsvParseResult {
    val content = inputStream.bufferedReader(charset).use { it.readText() }
    return parse(content, existingNisSet)
  }

  fun parse(
    rawCsv: String,
    existingNisSet: Set<String> = emptySet(),
  ): StudentCsvParseResult {
    val cleaned = rawCsv.removePrefix("\uFEFF").trim()
    if (cleaned.isBlank()) {
      return StudentCsvParseResult(
        totalDataRows = 0,
        validEntities = emptyList(),
        previewRows = emptyList(),
        invalidCount = 0,
        delimiterLabel = ",",
        usedHeaderMapping = false,
      )
    }

    val delimiter = detectDelimiter(cleaned)
    val records = splitCsvRecords(cleaned, delimiter)
      .filter { row -> row.any { it.isNotBlank() } }

    if (records.isEmpty()) {
      return StudentCsvParseResult(
        totalDataRows = 0,
        validEntities = emptyList(),
        previewRows = emptyList(),
        invalidCount = 0,
        delimiterLabel = delimiterDisplayName(delimiter),
        usedHeaderMapping = false,
      )
    }

    val mapping = detectColumnMapping(records.first())
    val dataStartIndex = if (mapping.hasHeader) 1 else 0

    val existingNormalized = existingNisSet.map { it.trim().lowercase(Locale.US) }.toSet()
    val seenInCsv = mutableSetOf<String>()

    val previewRows = mutableListOf<ParsedStudentCsvRow>()
    val validEntities = mutableListOf<StudentEntity>()

    for (idx in dataStartIndex until records.size) {
      val cols = records[idx]
      val lineNumber = idx + 1

      val rawNis = cols.getOrNull(mapping.nisIdx).orEmpty().trim()
      val rawName = collapseSpaces(cols.getOrNull(mapping.nameIdx).orEmpty())
      val rawGender = cols.getOrNull(mapping.genderIdx).orEmpty().trim()
      val rawContact = cols.getOrNull(mapping.contactIdx).orEmpty().trim()

      val errors = mutableListOf<String>()

      // Validate NIS
      val normalizedNisKey = rawNis.lowercase(Locale.US)
      when {
        rawNis.isBlank() -> errors += "NIS/NISN wajib diisi"
        rawNis.length > 30 -> errors += "NIS terlalu panjang (maks. 30 karakter)"
        !rawNis.matches(Regex("^[A-Za-z0-9._/-]+$")) -> errors += "Format NIS mengandung karakter tidak valid"
        normalizedNisKey in existingNormalized -> errors += "NIS \"$rawNis\" sudah terdaftar di kelas"
        !seenInCsv.add(normalizedNisKey) -> errors += "NIS \"$rawNis\" duplikat di dalam file CSV"
      }

      // Validate Full Name
      when {
        rawName.isBlank() -> errors += "Nama lengkap wajib diisi"
        rawName.length < 2 -> errors += "Nama lengkap minimal 2 karakter"
      }

      // Validate & Normalize Gender
      val normalizedGender = normalizeGender(rawGender)
      if (normalizedGender == null) {
        errors += "Jenis kelamin \"$rawGender\" tidak valid (gunakan L atau P)"
      }

      // Validate Parent Contact (optional, but must be valid phone format if present)
      if (rawContact.isNotBlank()) {
        val digitCount = rawContact.count { it.isDigit() }
        if (!rawContact.matches(Regex("^[+0-9\\s().-]{6,22}$")) || digitCount < 6) {
          errors += "Kontak orang tua \"$rawContact\" tidak valid"
        }
      }

      val finalGender = normalizedGender ?: "L"
      val entity = if (errors.isEmpty()) {
        StudentEntity(
          id = 0L,
          nis = rawNis,
          fullName = rawName,
          gender = finalGender,
          parentContact = rawContact,
        )
      } else {
        null
      }

      if (entity != null) {
        validEntities += entity
      }

      previewRows += ParsedStudentCsvRow(
        lineNumber = lineNumber,
        nis = rawNis,
        fullName = rawName,
        gender = finalGender,
        parentContact = rawContact,
        entity = entity,
        errors = errors,
      )
    }

    return StudentCsvParseResult(
      totalDataRows = previewRows.size,
      validEntities = validEntities,
      previewRows = previewRows,
      invalidCount = previewRows.count { !it.isValid },
      delimiterLabel = delimiterDisplayName(delimiter),
      usedHeaderMapping = mapping.hasHeader,
    )
  }

  private fun detectColumnMapping(firstRow: List<String>): ColumnMapping {
    val lowered = firstRow.map {
      it.trim().lowercase(Locale.US).replace("_", " ").replace(Regex("\\s+"), " ")
    }

    val nisIdx = lowered.indexOfFirst {
      it == "nis" || it == "nisn" || it == "nis/nisn" || it == "no induk" || it == "nomor induk" || it == "student id"
    }
    val nameIdx = lowered.indexOfFirst {
      it == "nama" || it == "nama lengkap" || it == "nama siswa" || it == "full name" || it == "name"
    }
    val genderIdx = lowered.indexOfFirst {
      it == "l/p" || it == "jk" || it == "jenis kelamin" || it == "kelamin" || it == "gender" || it == "sex"
    }
    val contactIdx = lowered.indexOfFirst {
      it.contains("kontak") || it.contains("telepon") || it.contains("telp") ||
        it.contains("hp") || it.contains("phone") || it.contains("wa") || it.contains("ortu")
    }

    val isHeader = nisIdx >= 0 || nameIdx >= 0 || genderIdx >= 0 || contactIdx >= 0
    return if (isHeader) {
      ColumnMapping(
        nisIdx = if (nisIdx >= 0) nisIdx else 0,
        nameIdx = if (nameIdx >= 0) nameIdx else 1,
        genderIdx = if (genderIdx >= 0) genderIdx else 2,
        contactIdx = if (contactIdx >= 0) contactIdx else 3,
        hasHeader = true,
      )
    } else {
      ColumnMapping(
        nisIdx = 0,
        nameIdx = 1,
        genderIdx = 2,
        contactIdx = 3,
        hasHeader = false,
      )
    }
  }

  private fun normalizeGender(raw: String): String? {
    if (raw.isBlank()) return "L"
    return when (raw.trim().uppercase(Locale.US)) {
      "L", "LK", "LAKI", "LAKI-LAKI", "LAKI LAKI", "M", "MALE", "PRIA" -> "L"
      "P", "PR", "PEREMPUAN", "W", "WANITA", "F", "FEMALE" -> "P"
      else -> null
    }
  }

  private fun collapseSpaces(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

  private fun detectDelimiter(raw: String): Char {
    val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    var inQuotes = false
    var tabs = 0
    var semis = 0
    var commas = 0
    for (ch in firstLine) {
      when {
        ch == '"' -> inQuotes = !inQuotes
        !inQuotes && ch == '\t' -> tabs++
        !inQuotes && ch == ';' -> semis++
        !inQuotes && ch == ',' -> commas++
      }
    }
    return when {
      tabs >= semis && tabs >= commas && tabs > 0 -> '\t'
      semis >= commas && semis > 0 -> ';'
      else -> ','
    }
  }

  private fun delimiterDisplayName(delimiter: Char): String = when (delimiter) {
    '\t' -> "Tab (TSV/Excel)"
    ';' -> "Titik Koma (;)"
    else -> "Koma (,)"
  }

  private fun splitCsvRecords(raw: String, delimiter: Char): List<List<String>> {
    val records = mutableListOf<List<String>>()
    var currentRow = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < raw.length) {
      val ch = raw[i]
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < raw.length && raw[i + 1] == '"') {
            currentField.append('"')
            i += 2
            continue
          } else {
            inQuotes = false
          }
        } else {
          currentField.append(ch)
        }
      } else {
        when (ch) {
          '"' -> inQuotes = true
          delimiter -> {
            currentRow += currentField.toString()
            currentField.clear()
          }
          '\r' -> {
            if (i + 1 < raw.length && raw[i + 1] == '\n') {
              i++
            }
            currentRow += currentField.toString()
            currentField.clear()
            records += currentRow
            currentRow = mutableListOf()
          }
          '\n' -> {
            currentRow += currentField.toString()
            currentField.clear()
            records += currentRow
            currentRow = mutableListOf()
          }
          else -> currentField.append(ch)
        }
      }
      i++
    }

    if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow += currentField.toString()
      records += currentRow
    }

    return records
  }
}
