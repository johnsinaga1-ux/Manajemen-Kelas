package com.example.data.backup

import com.example.data.db.AttendanceEntity
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import com.example.domain.AcademicCalculator
import com.example.domain.ClassReportSummary
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ClassBackupPayload(
  val schemaVersion: Int = 1,
  val exportedAtIso: String,
  val settings: BackupSettings,
  val students: List<BackupStudent>,
  val gradeCategories: List<BackupCategory>,
  val attendance: List<BackupAttendance>,
  val grades: List<BackupGrade>,
  val behaviorLogs: List<BackupBehavior>,
)

@JsonClass(generateAdapter = true)
data class BackupSettings(
  val className: String,
  val schoolName: String,
  val academicYear: String,
  val activeSemester: String,
  val kkm: Double,
  val homeroomTeacherName: String,
  val homeroomTeacherNip: String,
  val principalName: String,
  val principalNip: String,
)

@JsonClass(generateAdapter = true)
data class BackupStudent(
  val id: Long,
  val nis: String,
  val fullName: String,
  val gender: String,
  val parentContact: String,
  val homeroomNoteOverride: String? = null,
)

@JsonClass(generateAdapter = true)
data class BackupCategory(
  val id: Long,
  val name: String,
  val shortCode: String,
  val weightPercent: Double,
  val sortOrder: Int,
)

@JsonClass(generateAdapter = true)
data class BackupAttendance(
  val studentId: Long,
  val dateIso: String,
  val semester: String,
  val status: String,
  val note: String = "",
)

@JsonClass(generateAdapter = true)
data class BackupGrade(
  val studentId: Long,
  val categoryId: Long,
  val title: String,
  val score: Double,
  val dateIso: String,
  val semester: String,
)

@JsonClass(generateAdapter = true)
data class BackupBehavior(
  val studentId: Long,
  val dateIso: String,
  val semester: String,
  val type: String,
  val description: String,
  val followUp: String,
)

object BackupSerializer {
  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val adapter = moshi.adapter(ClassBackupPayload::class.java).indent("  ")

  fun toJson(
    exportedAtIso: String,
    settings: ClassSettingsEntity,
    students: List<StudentEntity>,
    categories: List<GradeCategoryEntity>,
    attendance: List<AttendanceEntity>,
    grades: List<GradeEntryEntity>,
    behaviors: List<BehaviorLogEntity>,
  ): String {
    val payload = ClassBackupPayload(
      exportedAtIso = exportedAtIso,
      settings = BackupSettings(
        className = settings.className,
        schoolName = settings.schoolName,
        academicYear = settings.academicYear,
        activeSemester = settings.activeSemester,
        kkm = settings.kkm,
        homeroomTeacherName = settings.homeroomTeacherName,
        homeroomTeacherNip = settings.homeroomTeacherNip,
        principalName = settings.principalName,
        principalNip = settings.principalNip,
      ),
      students = students.map {
        BackupStudent(it.id, it.nis, it.fullName, it.gender, it.parentContact, it.homeroomNoteOverride)
      },
      gradeCategories = categories.map {
        BackupCategory(it.id, it.name, it.shortCode, it.weightPercent, it.sortOrder)
      },
      attendance = attendance.map {
        BackupAttendance(it.studentId, it.dateIso, it.semester, it.status, it.note)
      },
      grades = grades.map {
        BackupGrade(it.studentId, it.categoryId, it.title, it.score, it.dateIso, it.semester)
      },
      behaviorLogs = behaviors.map {
        BackupBehavior(it.studentId, it.dateIso, it.semester, it.type, it.description, it.followUp)
      },
    )
    return adapter.toJson(payload)
  }

  fun fromJson(json: String): ClassBackupPayload {
    return adapter.fromJson(json)
      ?: throw IllegalArgumentException("Format file JSON backup tidak valid.")
  }

  fun generateReportCsv(report: ClassReportSummary): String {
    val sb = StringBuilder()
    // UTF-8 BOM so Microsoft Excel opens Indonesian characters cleanly
    sb.append('\uFEFF')
    val catHeaders = report.categories.joinToString(",") { csvEscape("${it.shortCode} (${AcademicCalculator.formatScore(it.weightPercent)}%)") }
    sb.append("No,NIS/NISN,Nama Lengkap,L/P,H,S,I,A,T,% Hadir")
    if (catHeaders.isNotEmpty()) {
      sb.append(',').append(catHeaders)
    }
    sb.append(",Nilai Akhir,Predikat,Status,Catatan Wali Kelas\n")

    report.studentRows.forEachIndexed { idx, row ->
      sb.append(idx + 1).append(',')
      sb.append(csvEscape(row.nis)).append(',')
      sb.append(csvEscape(row.fullName)).append(',')
      sb.append(csvEscape(row.gender)).append(',')
      sb.append(row.hadirCount).append(',')
      sb.append(row.sakitCount).append(',')
      sb.append(row.izinCount).append(',')
      sb.append(row.alpaCount).append(',')
      sb.append(row.terlambatCount).append(',')
      sb.append(csvEscape(AcademicCalculator.formatPercent(row.attendancePercent)))

      report.categories.forEach { cat ->
        val catSummary = row.categorySummaries.firstOrNull { it.categoryId == cat.id }
        sb.append(',').append(csvEscape(AcademicCalculator.formatScore(catSummary?.averageScore)))
      }

      val statusLabel = when (row.isPassing) {
        true -> "Tuntas"
        false -> "Remedial"
        null -> "Bobot != 100%"
      }
      sb.append(',').append(csvEscape(AcademicCalculator.formatScore(row.finalScore)))
      sb.append(',').append(csvEscape(row.predicate))
      sb.append(',').append(csvEscape(statusLabel))
      sb.append(',').append(csvEscape(row.effectiveHomeroomNote))
      sb.append('\n')
    }
    return sb.toString()
  }

  private fun csvEscape(value: String): String {
    val clean = value.replace("\r\n", " ").replace("\n", " ")
    return if (clean.contains(',') || clean.contains('"') || clean.contains(';')) {
      "\"${clean.replace("\"", "\"\"")}\""
    } else {
      clean
    }
  }
}
