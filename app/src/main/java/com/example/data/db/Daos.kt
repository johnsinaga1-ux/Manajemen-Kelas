package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSettingsDao {
  @Query("SELECT * FROM class_settings WHERE id = 1")
  fun observeSettings(): Flow<ClassSettingsEntity?>

  @Query("SELECT * FROM class_settings WHERE id = 1")
  suspend fun getSettingsOnce(): ClassSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(settings: ClassSettingsEntity)
}

@Dao
interface StudentDao {
  @Query("SELECT * FROM students ORDER BY fullName COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students ORDER BY fullName COLLATE NOCASE ASC")
  suspend fun getAllOnce(): List<StudentEntity>

  @Query("SELECT * FROM students WHERE nis = :nis LIMIT 1")
  suspend fun findByNis(nis: String): StudentEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(student: StudentEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAllIgnore(students: List<StudentEntity>): List<Long>

  @Update
  suspend fun update(student: StudentEntity)

  @Delete
  suspend fun delete(student: StudentEntity)

  @Query("UPDATE students SET homeroomNoteOverride = :note WHERE id = :studentId")
  suspend fun updateHomeroomNote(studentId: Long, note: String?)

  @Query("DELETE FROM students")
  suspend fun deleteAll()
}

@Dao
interface AttendanceDao {
  @Query("SELECT * FROM attendance ORDER BY dateIso DESC")
  fun observeAll(): Flow<List<AttendanceEntity>>

  @Query("SELECT * FROM attendance ORDER BY dateIso DESC")
  suspend fun getAllOnce(): List<AttendanceEntity>

  @Query("SELECT * FROM attendance WHERE dateIso = :dateIso")
  fun observeByDate(dateIso: String): Flow<List<AttendanceEntity>>

  @Query("SELECT * FROM attendance WHERE studentId = :studentId AND dateIso = :dateIso LIMIT 1")
  suspend fun findByStudentAndDate(studentId: Long, dateIso: String): AttendanceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: AttendanceEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(entities: List<AttendanceEntity>)

  @Query("DELETE FROM attendance WHERE studentId = :studentId AND dateIso = :dateIso")
  suspend fun deleteByStudentAndDate(studentId: Long, dateIso: String)

  @Query("DELETE FROM attendance")
  suspend fun deleteAll()
}

@Dao
interface GradeCategoryDao {
  @Query("SELECT * FROM grade_categories ORDER BY sortOrder ASC, id ASC")
  fun observeAll(): Flow<List<GradeCategoryEntity>>

  @Query("SELECT * FROM grade_categories ORDER BY sortOrder ASC, id ASC")
  suspend fun getAllOnce(): List<GradeCategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(category: GradeCategoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(categories: List<GradeCategoryEntity>)

  @Update
  suspend fun update(category: GradeCategoryEntity)

  @Delete
  suspend fun delete(category: GradeCategoryEntity)

  @Query("DELETE FROM grade_categories")
  suspend fun deleteAll()
}

@Dao
interface GradeEntryDao {
  @Query("SELECT * FROM grade_entries ORDER BY dateIso DESC, id DESC")
  fun observeAll(): Flow<List<GradeEntryEntity>>

  @Query("SELECT * FROM grade_entries ORDER BY dateIso DESC, id DESC")
  suspend fun getAllOnce(): List<GradeEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: GradeEntryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(entries: List<GradeEntryEntity>)

  @Update
  suspend fun update(entry: GradeEntryEntity)

  @Delete
  suspend fun delete(entry: GradeEntryEntity)

  @Query("DELETE FROM grade_entries")
  suspend fun deleteAll()
}

@Dao
interface BehaviorLogDao {
  @Query("SELECT * FROM behavior_logs ORDER BY dateIso DESC, id DESC")
  fun observeAll(): Flow<List<BehaviorLogEntity>>

  @Query("SELECT * FROM behavior_logs ORDER BY dateIso DESC, id DESC")
  suspend fun getAllOnce(): List<BehaviorLogEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(log: BehaviorLogEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(logs: List<BehaviorLogEntity>)

  @Update
  suspend fun update(log: BehaviorLogEntity)

  @Delete
  suspend fun delete(log: BehaviorLogEntity)

  @Query("DELETE FROM behavior_logs")
  suspend fun deleteAll()
}
