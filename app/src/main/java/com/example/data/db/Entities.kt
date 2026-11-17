package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "class_settings")
data class ClassSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val className: String = "XI IPA 1",
  val schoolName: String = "SMA Nusantara",
  val academicYear: String = "2026/2027",
  val activeSemester: String = "Ganjil",
  val kkm: Double = 75.0,
  val homeroomTeacherName: String = "",
  val homeroomTeacherNip: String = "",
  val principalName: String = "",
  val principalNip: String = "",
)

@Entity(
  tableName = "students",
  indices = [Index(value = ["nis"], unique = true)],
)
data class StudentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val nis: String,
  val fullName: String,
  val gender: String, // "L" or "P"
  val parentContact: String,
  val homeroomNoteOverride: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "attendance",
  foreignKeys = [
    ForeignKey(
      entity = StudentEntity::class,
      parentColumns = ["id"],
      childColumns = ["studentId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [
    Index(value = ["studentId", "dateIso"], unique = true),
    Index(value = ["dateIso"]),
  ],
)
data class AttendanceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val studentId: Long,
  val dateIso: String, // yyyy-MM-dd
  val semester: String, // "Ganjil" or "Genap"
  val status: String, // "H", "S", "I", "A", "T"
  val note: String = "",
)

@Entity(tableName = "grade_categories")
data class GradeCategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val shortCode: String,
  val weightPercent: Double,
  val sortOrder: Int = 0,
)

@Entity(
  tableName = "grade_entries",
  foreignKeys = [
    ForeignKey(
      entity = StudentEntity::class,
      parentColumns = ["id"],
      childColumns = ["studentId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = GradeCategoryEntity::class,
      parentColumns = ["id"],
      childColumns = ["categoryId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["studentId"]),
    Index(value = ["categoryId"]),
    Index(value = ["dateIso"]),
  ],
)
data class GradeEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val studentId: Long,
  val categoryId: Long,
  val title: String,
  val score: Double,
  val dateIso: String, // yyyy-MM-dd
  val semester: String,
)

@Entity(
  tableName = "behavior_logs",
  foreignKeys = [
    ForeignKey(
      entity = StudentEntity::class,
      parentColumns = ["id"],
      childColumns = ["studentId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [
    Index(value = ["studentId"]),
    Index(value = ["dateIso"]),
  ],
)
data class BehaviorLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val studentId: Long,
  val dateIso: String, // yyyy-MM-dd
  val semester: String,
  val type: String, // "POSITIF" or "NEGATIF"
  val description: String,
  val followUp: String,
)
