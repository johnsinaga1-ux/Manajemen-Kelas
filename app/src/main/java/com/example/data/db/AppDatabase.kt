package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    ClassSettingsEntity::class,
    StudentEntity::class,
    AttendanceEntity::class,
    GradeCategoryEntity::class,
    GradeEntryEntity::class,
    BehaviorLogEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun classSettingsDao(): ClassSettingsDao
  abstract fun studentDao(): StudentDao
  abstract fun attendanceDao(): AttendanceDao
  abstract fun gradeCategoryDao(): GradeCategoryDao
  abstract fun gradeEntryDao(): GradeEntryDao
  abstract fun behaviorLogDao(): BehaviorLogDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "walikelas_academic.db",
        ).build().also { instance = it }
      }
    }

    fun defaultCategories(): List<GradeCategoryEntity> = listOf(
      GradeCategoryEntity(name = "Tugas", shortCode = "Tugas", weightPercent = 20.0, sortOrder = 0),
      GradeCategoryEntity(name = "Ulangan Harian", shortCode = "UH", weightPercent = 30.0, sortOrder = 1),
      GradeCategoryEntity(name = "UTS", shortCode = "UTS", weightPercent = 25.0, sortOrder = 2),
      GradeCategoryEntity(name = "UAS", shortCode = "UAS", weightPercent = 25.0, sortOrder = 3),
    )
  }
}
