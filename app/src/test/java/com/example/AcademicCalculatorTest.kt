package com.example

import com.example.data.backup.BackupSerializer
import com.example.data.db.AttendanceEntity
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import com.example.domain.AcademicCalculator
import com.example.domain.ReportFilter
import com.example.domain.ReportFilterMode
import com.example.domain.SemesterOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicCalculatorTest {

  private val defaultCategories = listOf(
    GradeCategoryEntity(id = 1, name = "Tugas", shortCode = "Tugas", weightPercent = 20.0, sortOrder = 0),
    GradeCategoryEntity(id = 2, name = "Ulangan Harian", shortCode = "UH", weightPercent = 30.0, sortOrder = 1),
    GradeCategoryEntity(id = 3, name = "UTS", shortCode = "UTS", weightPercent = 25.0, sortOrder = 2),
    GradeCategoryEntity(id = 4, name = "UAS", shortCode = "UAS", weightPercent = 25.0, sortOrder = 3),
  )

  @Test
  fun `weighted final score and attendance percentage use 2 decimal precision without NaN`() {
    val settings = ClassSettingsEntity(kkm = 75.0)
    val students = listOf(
      StudentEntity(id = 1, nis = "1001", fullName = "Budi Santoso", gender = "L", parentContact = "08123"),
      StudentEntity(id = 2, nis = "1002", fullName = "Ani Wijaya", gender = "P", parentContact = "08124"),
    )
    val attendance = listOf(
      AttendanceEntity(id = 1, studentId = 1, dateIso = "2026-11-01", semester = "Ganjil", status = "H"),
      AttendanceEntity(id = 2, studentId = 1, dateIso = "2026-11-02", semester = "Ganjil", status = "A"),
      AttendanceEntity(id = 3, studentId = 1, dateIso = "2026-11-03", semester = "Ganjil", status = "H"),
      AttendanceEntity(id = 4, studentId = 2, dateIso = "2026-11-01", semester = "Ganjil", status = "H"),
      AttendanceEntity(id = 5, studentId = 2, dateIso = "2026-11-02", semester = "Ganjil", status = "H"),
      AttendanceEntity(id = 6, studentId = 2, dateIso = "2026-11-03", semester = "Ganjil", status = "T"),
    )
    val grades = listOf(
      // Ani: Tugas=90, UH=80, UTS=85, UAS=92 => 18 + 24 + 21.25 + 23 = 86.25 (B, Tuntas)
      GradeEntryEntity(id = 1, studentId = 2, categoryId = 1, title = "T1", score = 90.0, dateIso = "2026-11-01", semester = "Ganjil"),
      GradeEntryEntity(id = 2, studentId = 2, categoryId = 2, title = "UH1", score = 80.0, dateIso = "2026-11-02", semester = "Ganjil"),
      GradeEntryEntity(id = 3, studentId = 2, categoryId = 3, title = "UTS", score = 85.0, dateIso = "2026-11-03", semester = "Ganjil"),
      GradeEntryEntity(id = 4, studentId = 2, categoryId = 4, title = "UAS", score = 92.0, dateIso = "2026-11-04", semester = "Ganjil"),
      // Budi: only Tugas=60, rest missing => 12.00 (D, Remedial)
      GradeEntryEntity(id = 5, studentId = 1, categoryId = 1, title = "T1", score = 60.0, dateIso = "2026-11-01", semester = "Ganjil"),
    )

    val report = AcademicCalculator.generateReport(
      settings = settings,
      students = students,
      categories = defaultCategories,
      attendance = attendance,
      grades = grades,
      behaviors = emptyList(),
      filter = ReportFilter(mode = ReportFilterMode.SEMESTER, semester = SemesterOption.GANJIL),
    )

    // Sorted A-Z: Ani first, Budi second
    assertEquals("Ani Wijaya", report.studentRows[0].fullName)
    assertEquals("Budi Santoso", report.studentRows[1].fullName)

    val ani = report.studentRows[0]
    assertEquals(100.0, ani.attendancePercent, 0.001)
    assertEquals(86.25, ani.finalScore!!, 0.001)
    assertEquals("B", ani.predicate)
    assertTrue(ani.isPassing == true)

    val budi = report.studentRows[1]
    assertEquals(66.67, budi.attendancePercent, 0.001)
    assertEquals(12.0, budi.finalScore!!, 0.001)
    assertEquals("D", budi.predicate)
    assertFalse(budi.isPassing == true)

    assertEquals(1, report.priorityFollowUps.size)
    assertEquals("Budi Santoso", report.priorityFollowUps[0].studentName)
  }

  @Test
  fun `invalid weight total blocks final score calculation and flags anomaly`() {
    val invalidCats = listOf(
      GradeCategoryEntity(id = 1, name = "Tugas", shortCode = "Tugas", weightPercent = 20.0),
      GradeCategoryEntity(id = 2, name = "UH", shortCode = "UH", weightPercent = 30.0),
    )
    val students = listOf(StudentEntity(id = 1, nis = "1001", fullName = "Siti", gender = "P", parentContact = ""))
    val report = AcademicCalculator.generateReport(
      settings = ClassSettingsEntity(),
      students = students,
      categories = invalidCats,
      attendance = emptyList(),
      grades = emptyList(),
      behaviors = emptyList(),
      filter = ReportFilter(),
    )

    assertFalse(report.isWeightValid)
    assertNull(report.studentRows.first().finalScore)
    assertEquals("-", report.studentRows.first().predicate)
    assertTrue(report.anomalies.any { it.title.contains("Bobot") })
  }

  @Test
  fun `raw academic parser detects anomalies and calculates accurate report`() {
    val raw = """
      Nama, H, S, I, A, Tugas, UH, UTS, UAS
      Citra, 19, 1, 0, 0, 90, 90, 90, 90
      Doni, 10, 0, 0, 5, 70, 60, 110, -
    """.trimIndent()

    val result = AcademicCalculator.processRawAcademicText(raw, kkm = 75.0)
    assertEquals(2, result.rows.size)
    assertEquals(90.0, result.rows[0].finalScore, 0.001)
    assertEquals("A", result.rows[0].predicate)
    assertTrue(result.rows[0].isPassing)
    assertTrue(result.anomalies.any { it.title.contains("Di Luar Rentang") })
    assertTrue(result.anomalies.any { it.title.contains("Kosong") })
    assertEquals(1, result.priorityFollowUps.size)
    assertEquals("Doni", result.priorityFollowUps[0].studentName)
  }

  @Test
  fun `backup json round trip preserves all data cleanly`() {
    val json = BackupSerializer.toJson(
      exportedAtIso = "2026-11-16T10:00:00",
      settings = ClassSettingsEntity(className = "XII IPS 2", kkm = 78.0),
      students = listOf(StudentEntity(id = 1, nis = "901", fullName = "Rina", gender = "P", parentContact = "0811")),
      categories = defaultCategories,
      attendance = listOf(AttendanceEntity(id = 1, studentId = 1, dateIso = "2026-11-16", semester = "Ganjil", status = "H")),
      grades = listOf(GradeEntryEntity(id = 1, studentId = 1, categoryId = 1, title = "T1", score = 88.5, dateIso = "2026-11-16", semester = "Ganjil")),
      behaviors = listOf(BehaviorLogEntity(id = 1, studentId = 1, dateIso = "2026-11-16", semester = "Ganjil", type = "POSITIF", description = "Aktif diskusi", followUp = "Apresiasi")),
    )
    val parsed = BackupSerializer.fromJson(json)
    assertEquals("XII IPS 2", parsed.settings.className)
    assertEquals(78.0, parsed.settings.kkm, 0.001)
    assertEquals(1, parsed.students.size)
    assertEquals("Rina", parsed.students[0].fullName)
    assertEquals(88.5, parsed.grades[0].score, 0.001)
    assertEquals(1, parsed.behaviorLogs.size)
  }
}
