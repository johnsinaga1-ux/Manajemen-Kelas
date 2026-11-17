package com.example.domain

import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity

enum class AttendanceStatus(val code: String, val label: String) {
  HADIR("H", "Hadir"),
  SAKIT("S", "Sakit"),
  IZIN("I", "Izin"),
  ALPA("A", "Alpa"),
  TERLAMBAT("T", "Terlambat");

  companion object {
    fun fromCode(code: String): AttendanceStatus =
      entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: HADIR
  }
}

enum class BehaviorType(val code: String, val label: String) {
  POSITIF("POSITIF", "Positif"),
  NEGATIF("NEGATIF", "Negatif");

  companion object {
    fun fromCode(code: String): BehaviorType =
      entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: POSITIF
  }
}

enum class SemesterOption(val value: String, val label: String) {
  SEMUA("Semua", "Semua Semester"),
  GANJIL("Ganjil", "Semester Ganjil"),
  GENAP("Genap", "Semester Genap"),
}

enum class ReportFilterMode(val label: String) {
  SEMESTER("Semester"),
  MONTH("Per Bulan"),
  DATE_RANGE("Rentang Tanggal"),
  STUDENT("Per Siswa"),
}

data class ReportFilter(
  val mode: ReportFilterMode = ReportFilterMode.SEMESTER,
  val semester: SemesterOption = SemesterOption.SEMUA,
  val monthIso: String = "", // yyyy-MM
  val startDateIso: String = "", // yyyy-MM-dd
  val endDateIso: String = "", // yyyy-MM-dd
  val studentId: Long? = null,
)

data class CategoryScoreSummary(
  val categoryId: Long,
  val categoryName: String,
  val shortCode: String,
  val weightPercent: Double,
  val averageScore: Double?, // null when no entries in category
  val entryCount: Int,
)

data class StudentReportRow(
  val studentId: Long,
  val nis: String,
  val fullName: String,
  val gender: String,
  val parentContact: String,
  val hadirCount: Int,
  val sakitCount: Int,
  val izinCount: Int,
  val alpaCount: Int,
  val terlambatCount: Int,
  val totalEffectiveDays: Int,
  val attendancePercent: Double,
  val categorySummaries: List<CategoryScoreSummary>,
  val finalScore: Double?, // null if total weight != 100%
  val predicate: String, // A, B, C, D, or -
  val isPassing: Boolean?, // null if finalScore == null
  val behaviorLogs: List<BehaviorLogEntity>,
  val autoHomeroomNote: String,
  val customHomeroomNote: String?,
) {
  val effectiveHomeroomNote: String
    get() = customHomeroomNote?.takeIf { it.isNotBlank() } ?: autoHomeroomNote
}

enum class AnomalySeverity { ERROR, WARNING }

data class DataAnomaly(
  val severity: AnomalySeverity,
  val title: String,
  val detail: String,
)

data class PriorityActionItem(
  val studentId: Long,
  val studentName: String,
  val nis: String,
  val attendancePercent: Double,
  val alpaCount: Int,
  val finalScore: Double?,
  val reasons: List<String>,
  val recommendation: String,
)

data class ClassReportSummary(
  val settings: ClassSettingsEntity,
  val categories: List<GradeCategoryEntity>,
  val totalWeightPercent: Double,
  val isWeightValid: Boolean,
  val totalStudents: Int,
  val totalEffectiveDays: Int,
  val averageAttendancePercent: Double,
  val averageFinalScore: Double,
  val highestScore: Double?,
  val lowestScore: Double?,
  val passingCount: Int,
  val passingRatePercent: Double,
  val studentRows: List<StudentReportRow>,
  val anomalies: List<DataAnomaly>,
  val priorityFollowUps: List<PriorityActionItem>,
)

data class BulkStudentPreviewRow(
  val lineNumber: Int,
  val nis: String,
  val fullName: String,
  val gender: String,
  val parentContact: String,
  val errorMessage: String? = null,
) {
  val isValid: Boolean get() = errorMessage == null
}

data class RawParsedStudentRow(
  val name: String,
  val nis: String,
  val h: Int,
  val s: Int,
  val i: Int,
  val a: Int,
  val t: Int,
  val tugas: Double?,
  val uh: Double?,
  val uts: Double?,
  val uas: Double?,
  val attendancePercent: Double,
  val finalScore: Double,
  val predicate: String,
  val isPassing: Boolean,
  val homeroomNote: String,
)

data class RawImportParseResult(
  val rows: List<RawParsedStudentRow>,
  val anomalies: List<DataAnomaly>,
  val priorityFollowUps: List<PriorityActionItem>,
  val averageAttendancePercent: Double,
  val averageFinalScore: Double,
  val passingRatePercent: Double,
)
