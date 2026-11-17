package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.backup.BackupSerializer
import com.example.data.backup.ClassBackupPayload
import com.example.data.db.AppDatabase
import com.example.data.db.AttendanceEntity
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import com.example.domain.BulkStudentPreviewRow
import com.example.domain.RawImportParseResult
import com.example.domain.StudentCsvParseResult
import com.example.domain.StudentCsvParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClassRepository(private val db: AppDatabase) {

  private val settingsDao = db.classSettingsDao()
  private val studentDao = db.studentDao()
  private val attendanceDao = db.attendanceDao()
  private val categoryDao = db.gradeCategoryDao()
  private val gradeDao = db.gradeEntryDao()
  private val behaviorDao = db.behaviorLogDao()

  val settingsFlow: Flow<ClassSettingsEntity> = settingsDao.observeSettings().map {
    it ?: ClassSettingsEntity()
  }
  val studentsFlow: Flow<List<StudentEntity>> = studentDao.observeAll()
  val attendanceFlow: Flow<List<AttendanceEntity>> = attendanceDao.observeAll()
  val categoriesFlow: Flow<List<GradeCategoryEntity>> = categoryDao.observeAll()
  val gradesFlow: Flow<List<GradeEntryEntity>> = gradeDao.observeAll()
  val behaviorsFlow: Flow<List<BehaviorLogEntity>> = behaviorDao.observeAll()

  suspend fun ensureDefaultsInitialized() {
    if (settingsDao.getSettingsOnce() == null) {
      settingsDao.upsert(ClassSettingsEntity())
    }
    if (categoryDao.getAllOnce().isEmpty()) {
      categoryDao.insertAll(AppDatabase.defaultCategories())
    }
  }

  suspend fun saveSettings(settings: ClassSettingsEntity) {
    settingsDao.upsert(settings.copy(id = 1))
  }

  suspend fun saveStudent(student: StudentEntity): Result<Unit> = runCatching {
    val cleanNis = student.nis.trim()
    val cleanName = student.fullName.trim()
    require(cleanNis.isNotBlank()) { "NIS/NISN tidak boleh kosong." }
    require(cleanName.isNotBlank()) { "Nama lengkap tidak boleh kosong." }
    val existing = studentDao.findByNis(cleanNis)
    if (existing != null && existing.id != student.id) {
      error("NIS/NISN $cleanNis sudah terdaftar atas nama ${existing.fullName}.")
    }
    val clean = student.copy(
      nis = cleanNis,
      fullName = cleanName,
      parentContact = student.parentContact.trim(),
    )
    if (clean.id == 0L) studentDao.insert(clean) else studentDao.update(clean)
  }

  suspend fun deleteStudent(student: StudentEntity) {
    studentDao.delete(student)
  }

  suspend fun updateHomeroomNote(studentId: Long, note: String?) {
    studentDao.updateHomeroomNote(studentId, note?.trim()?.ifBlank { null })
  }

  suspend fun importValidatedStudents(entities: List<StudentEntity>): Int {
    if (entities.isEmpty()) return 0
    val insertedIds = studentDao.insertAllIgnore(entities)
    return insertedIds.count { it > 0L }
  }

  suspend fun importStudentsFromCsv(rawCsv: String): Result<Pair<StudentCsvParseResult, Int>> = runCatching {
    val existingNis = studentDao.getAllOnce().map { it.nis }.toSet()
    val parsed = StudentCsvParser.parse(rawCsv, existingNis)
    val inserted = importValidatedStudents(parsed.validEntities)
    parsed to inserted
  }

  suspend fun importBulkStudents(rows: List<BulkStudentPreviewRow>): Int {
    val validRows = rows.filter { it.isValid }
    if (validRows.isEmpty()) return 0
    val entities = validRows.map {
      StudentEntity(
        nis = it.nis.trim(),
        fullName = it.fullName.trim(),
        gender = it.gender,
        parentContact = it.parentContact.trim(),
      )
    }
    return importValidatedStudents(entities)
  }

  suspend fun setAttendance(
    studentId: Long,
    dateIso: String,
    semester: String,
    status: String,
    note: String = "",
  ) {
    val existing = attendanceDao.findByStudentAndDate(studentId, dateIso)
    val entity = AttendanceEntity(
      id = existing?.id ?: 0L,
      studentId = studentId,
      dateIso = dateIso,
      semester = semester,
      status = status,
      note = note.trim(),
    )
    attendanceDao.upsert(entity)
  }

  suspend fun markAllPresentForDate(
    studentIds: List<Long>,
    dateIso: String,
    semester: String,
  ) {
    if (studentIds.isEmpty()) return
    db.withTransaction {
      for (studentId in studentIds) {
        val existing = attendanceDao.findByStudentAndDate(studentId, dateIso)
        if (existing == null) {
          attendanceDao.upsert(
            AttendanceEntity(
              studentId = studentId,
              dateIso = dateIso,
              semester = semester,
              status = "H",
              note = "",
            )
          )
        } else if (existing.status != "H") {
          attendanceDao.upsert(existing.copy(status = "H"))
        }
      }
    }
  }

  suspend fun saveCategories(categories: List<GradeCategoryEntity>): Result<Unit> = runCatching {
    require(categories.isNotEmpty()) { "Minimal harus ada 1 kategori nilai." }
    categories.forEach {
      require(it.name.isNotBlank()) { "Nama kategori tidak boleh kosong." }
      require(it.weightPercent in 0.0..100.0) { "Bobot ${it.name} harus antara 0% - 100%." }
    }
    db.withTransaction {
      val existing = categoryDao.getAllOnce()
      val keptIds = categories.mapNotNull { if (it.id > 0L) it.id else null }.toSet()
      existing.filter { it.id !in keptIds }.forEach { categoryDao.delete(it) }
      categories.forEachIndexed { index, cat ->
        val clean = cat.copy(
          name = cat.name.trim(),
          shortCode = cat.shortCode.trim().ifBlank { cat.name.trim().take(5) },
          sortOrder = index,
        )
        if (clean.id == 0L) categoryDao.insert(clean) else categoryDao.update(clean)
      }
    }
  }

  suspend fun saveGradeEntry(entry: GradeEntryEntity): Result<Unit> = runCatching {
    require(entry.title.isNotBlank()) { "Judul penilaian tidak boleh kosong." }
    require(entry.score in 0.0..100.0) { "Nilai harus di antara 0 sampai 100." }
    val clean = entry.copy(title = entry.title.trim())
    if (clean.id == 0L) gradeDao.insert(clean) else gradeDao.update(clean)
  }

  suspend fun saveBatchGrades(entries: List<GradeEntryEntity>): Result<Int> = runCatching {
    val valid = entries.filter { it.score in 0.0..100.0 && it.title.isNotBlank() }
    if (valid.isNotEmpty()) {
      gradeDao.insertAll(valid)
    }
    valid.size
  }

  suspend fun deleteGradeEntry(entry: GradeEntryEntity) {
    gradeDao.delete(entry)
  }

  suspend fun saveBehaviorLog(log: BehaviorLogEntity): Result<Unit> = runCatching {
    require(log.description.isNotBlank()) { "Deskripsi kejadian tidak boleh kosong." }
    val clean = log.copy(
      description = log.description.trim(),
      followUp = log.followUp.trim(),
    )
    if (clean.id == 0L) behaviorDao.insert(clean) else behaviorDao.update(clean)
  }

  suspend fun deleteBehaviorLog(log: BehaviorLogEntity) {
    behaviorDao.delete(log)
  }

  suspend fun persistRawImport(
    parsed: RawImportParseResult,
    semester: String,
    baseDateIso: String,
  ): Result<Int> = runCatching {
    ensureDefaultsInitialized()
    val categories = categoryDao.getAllOnce().sortedBy { it.sortOrder }
    val catTugas = categories.firstOrNull { it.shortCode.equals("Tugas", true) || it.name.contains("Tugas", true) }
    val catUh = categories.firstOrNull { it.shortCode.equals("UH", true) || it.name.contains("Ulangan", true) }
    val catUts = categories.firstOrNull { it.shortCode.equals("UTS", true) || it.name.contains("UTS", true) }
    val catUas = categories.firstOrNull { it.shortCode.equals("UAS", true) || it.name.contains("UAS", true) }

    var savedCount = 0
    db.withTransaction {
      for (row in parsed.rows) {
        val existing = studentDao.findByNis(row.nis)
        val studentId = existing?.id ?: studentDao.insert(
          StudentEntity(
            nis = row.nis,
            fullName = row.name,
            gender = "L",
            parentContact = "",
          )
        )
        // Store synthetic summary attendance dates if counts > 0 and no attendance exists yet
        val statuses = buildList {
          repeat(row.h) { add("H") }
          repeat(row.s) { add("S") }
          repeat(row.i) { add("I") }
          repeat(row.a) { add("A") }
          repeat(row.t) { add("T") }
        }
        statuses.forEachIndexed { idx, st ->
          val syntheticDate = shiftDateString(baseDateIso, -idx)
          if (attendanceDao.findByStudentAndDate(studentId, syntheticDate) == null) {
            attendanceDao.upsert(
              AttendanceEntity(
                studentId = studentId,
                dateIso = syntheticDate,
                semester = semester,
                status = st,
                note = "Impor data mentah",
              )
            )
          }
        }
        val toInsert = mutableListOf<GradeEntryEntity>()
        row.tugas?.let { score ->
          catTugas?.let { toInsert += GradeEntryEntity(studentId = studentId, categoryId = it.id, title = "Tugas (Impor)", score = score, dateIso = baseDateIso, semester = semester) }
        }
        row.uh?.let { score ->
          catUh?.let { toInsert += GradeEntryEntity(studentId = studentId, categoryId = it.id, title = "UH (Impor)", score = score, dateIso = baseDateIso, semester = semester) }
        }
        row.uts?.let { score ->
          catUts?.let { toInsert += GradeEntryEntity(studentId = studentId, categoryId = it.id, title = "UTS (Impor)", score = score, dateIso = baseDateIso, semester = semester) }
        }
        row.uas?.let { score ->
          catUas?.let { toInsert += GradeEntryEntity(studentId = studentId, categoryId = it.id, title = "UAS (Impor)", score = score, dateIso = baseDateIso, semester = semester) }
        }
        if (toInsert.isNotEmpty()) gradeDao.insertAll(toInsert)
        savedCount++
      }
    }
    savedCount
  }

  suspend fun exportBackupJson(exportedAtIso: String): String {
    ensureDefaultsInitialized()
    val settings = settingsDao.getSettingsOnce() ?: ClassSettingsEntity()
    val students = studentDao.getAllOnce()
    val categories = categoryDao.getAllOnce()
    val attendance = attendanceDao.getAllOnce()
    val grades = gradeDao.getAllOnce()
    val behaviors = behaviorDao.getAllOnce()
    return BackupSerializer.toJson(
      exportedAtIso = exportedAtIso,
      settings = settings,
      students = students,
      categories = categories,
      attendance = attendance,
      grades = grades,
      behaviors = behaviors,
    )
  }

  suspend fun restoreFromBackupJson(json: String): Result<ClassBackupPayload> = runCatching {
    val payload = BackupSerializer.fromJson(json)
    db.withTransaction {
      attendanceDao.deleteAll()
      gradeDao.deleteAll()
      behaviorDao.deleteAll()
      studentDao.deleteAll()
      categoryDao.deleteAll()

      settingsDao.upsert(
        ClassSettingsEntity(
          id = 1,
          className = payload.settings.className,
          schoolName = payload.settings.schoolName,
          academicYear = payload.settings.academicYear,
          activeSemester = payload.settings.activeSemester,
          kkm = payload.settings.kkm,
          homeroomTeacherName = payload.settings.homeroomTeacherName,
          homeroomTeacherNip = payload.settings.homeroomTeacherNip,
          principalName = payload.settings.principalName,
          principalNip = payload.settings.principalNip,
        )
      )

      val categoryIdMap = mutableMapOf<Long, Long>()
      val cats = payload.gradeCategories.ifEmpty {
        AppDatabase.defaultCategories().map {
          com.example.data.backup.BackupCategory(it.id, it.name, it.shortCode, it.weightPercent, it.sortOrder)
        }
      }
      cats.forEach { cat ->
        val newId = categoryDao.insert(
          GradeCategoryEntity(
            name = cat.name,
            shortCode = cat.shortCode,
            weightPercent = cat.weightPercent,
            sortOrder = cat.sortOrder,
          )
        )
        categoryIdMap[cat.id] = newId
      }

      val studentIdMap = mutableMapOf<Long, Long>()
      payload.students.forEach { st ->
        val newId = studentDao.insert(
          StudentEntity(
            nis = st.nis,
            fullName = st.fullName,
            gender = st.gender,
            parentContact = st.parentContact,
            homeroomNoteOverride = st.homeroomNoteOverride,
          )
        )
        studentIdMap[st.id] = newId
      }

      val restoredAttendance = payload.attendance.mapNotNull { att ->
        val mappedStudentId = studentIdMap[att.studentId] ?: return@mapNotNull null
        AttendanceEntity(
          studentId = mappedStudentId,
          dateIso = att.dateIso,
          semester = att.semester,
          status = att.status,
          note = att.note,
        )
      }
      if (restoredAttendance.isNotEmpty()) attendanceDao.upsertAll(restoredAttendance)

      val restoredGrades = payload.grades.mapNotNull { g ->
        val mappedStudentId = studentIdMap[g.studentId] ?: return@mapNotNull null
        val mappedCatId = categoryIdMap[g.categoryId] ?: return@mapNotNull null
        GradeEntryEntity(
          studentId = mappedStudentId,
          categoryId = mappedCatId,
          title = g.title,
          score = g.score.coerceIn(0.0, 100.0),
          dateIso = g.dateIso,
          semester = g.semester,
        )
      }
      if (restoredGrades.isNotEmpty()) gradeDao.insertAll(restoredGrades)

      val restoredBehaviors = payload.behaviorLogs.mapNotNull { b ->
        val mappedStudentId = studentIdMap[b.studentId] ?: return@mapNotNull null
        BehaviorLogEntity(
          studentId = mappedStudentId,
          dateIso = b.dateIso,
          semester = b.semester,
          type = b.type,
          description = b.description,
          followUp = b.followUp,
        )
      }
      if (restoredBehaviors.isNotEmpty()) behaviorDao.insertAll(restoredBehaviors)
    }
    payload
  }

  private fun shiftDateString(dateIso: String, deltaDays: Int): String {
    return try {
      val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
      val cal = java.util.Calendar.getInstance()
      cal.time = sdf.parse(dateIso) ?: java.util.Date()
      cal.add(java.util.Calendar.DAY_OF_YEAR, deltaDays)
      sdf.format(cal.time)
    } catch (_: Exception) {
      dateIso
    }
  }
}
