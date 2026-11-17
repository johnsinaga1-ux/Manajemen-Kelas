package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupSerializer
import com.example.data.db.AppDatabase
import com.example.data.db.AttendanceEntity
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import com.example.data.repository.ClassRepository
import com.example.domain.AcademicCalculator
import com.example.domain.BulkStudentPreviewRow
import com.example.domain.ClassReportSummary
import com.example.domain.RawImportParseResult
import com.example.domain.ReportFilter
import com.example.domain.ReportFilterMode
import com.example.domain.SemesterOption
import com.example.domain.StudentCsvParseResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MainUiState(
  val isLoading: Boolean = true,
  val settings: ClassSettingsEntity = ClassSettingsEntity(),
  val students: List<StudentEntity> = emptyList(),
  val attendance: List<AttendanceEntity> = emptyList(),
  val categories: List<GradeCategoryEntity> = emptyList(),
  val grades: List<GradeEntryEntity> = emptyList(),
  val behaviors: List<BehaviorLogEntity> = emptyList(),
  val selectedAttendanceDate: String = todayIso(),
  val reportFilter: ReportFilter = ReportFilter(
    monthIso = todayIso().substring(0, 7),
    startDateIso = "${todayIso().substring(0, 7)}-01",
    endDateIso = todayIso(),
  ),
  val report: ClassReportSummary = AcademicCalculator.generateReport(
    settings = ClassSettingsEntity(),
    students = emptyList(),
    categories = emptyList(),
    attendance = emptyList(),
    grades = emptyList(),
    behaviors = emptyList(),
    filter = ReportFilter(),
  ),
) {
  companion object {
    fun todayIso(): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ClassRepository = ClassRepository(AppDatabase.getInstance(application))

  private val _selectedDate = MutableStateFlow(MainUiState.todayIso())
  private val _reportFilter = MutableStateFlow(
    ReportFilter(
      mode = ReportFilterMode.SEMESTER,
      semester = SemesterOption.SEMUA,
      monthIso = MainUiState.todayIso().substring(0, 7),
      startDateIso = "${MainUiState.todayIso().substring(0, 7)}-01",
      endDateIso = MainUiState.todayIso(),
    )
  )

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val messages: SharedFlow<String> = _messages.asSharedFlow()

  init {
    viewModelScope.launch {
      repository.ensureDefaultsInitialized()
    }
  }

  private val baseDataFlow = combine(
    repository.settingsFlow,
    repository.studentsFlow,
    repository.attendanceFlow,
    repository.categoriesFlow,
    repository.gradesFlow,
  ) { settings, students, attendance, categories, grades ->
    BaseBundle(settings, students, attendance, categories, grades)
  }

  val uiState: StateFlow<MainUiState> = combine(
    baseDataFlow,
    repository.behaviorsFlow,
    _selectedDate,
    _reportFilter,
  ) { base, behaviors, selectedDate, filter ->
    val report = AcademicCalculator.generateReport(
      settings = base.settings,
      students = base.students,
      categories = base.categories,
      attendance = base.attendance,
      grades = base.grades,
      behaviors = behaviors,
      filter = filter,
    )
    MainUiState(
      isLoading = false,
      settings = base.settings,
      students = base.students,
      attendance = base.attendance,
      categories = base.categories,
      grades = base.grades,
      behaviors = behaviors,
      selectedAttendanceDate = selectedDate,
      reportFilter = filter,
      report = report,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = MainUiState(),
  )

  fun saveSettings(settings: ClassSettingsEntity) {
    viewModelScope.launch {
      repository.saveSettings(settings)
      _messages.tryEmit("Pengaturan kelas disimpan.")
    }
  }

  fun saveStudent(student: StudentEntity, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveStudent(student)
        .onSuccess {
          _messages.tryEmit(if (student.id == 0L) "Siswa ditambahkan." else "Data siswa diperbarui.")
          onSuccess()
        }
        .onFailure { err ->
          _messages.tryEmit(err.message ?: "Gagal menyimpan data siswa.")
        }
    }
  }

  fun deleteStudent(student: StudentEntity) {
    viewModelScope.launch {
      repository.deleteStudent(student)
      _messages.tryEmit("Data ${student.fullName} dihapus.")
    }
  }

  fun importBulkStudents(rows: List<BulkStudentPreviewRow>, onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val inserted = repository.importBulkStudents(rows)
      _messages.tryEmit("$inserted siswa berhasil diimpor.")
      onComplete(inserted)
    }
  }

  fun importCsvResult(result: StudentCsvParseResult, onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val inserted = repository.importValidatedStudents(result.validEntities)
      val skippedNote = if (result.invalidCount > 0) " (${result.invalidCount} baris dilewati karena tidak valid)" else ""
      _messages.tryEmit("$inserted siswa berhasil diimpor ke database$skippedNote.")
      onComplete(inserted)
    }
  }

  fun updateHomeroomNote(studentId: Long, note: String?) {
    viewModelScope.launch {
      repository.updateHomeroomNote(studentId, note)
      _messages.tryEmit(if (note.isNullOrBlank()) "Catatan dikembalikan ke draf otomatis." else "Catatan wali kelas disimpan.")
    }
  }

  fun setAttendanceDate(dateIso: String) {
    _selectedDate.value = dateIso
  }

  fun shiftAttendanceDate(deltaDays: Int) {
    try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val cal = Calendar.getInstance()
      cal.time = sdf.parse(_selectedDate.value) ?: Date()
      cal.add(Calendar.DAY_OF_YEAR, deltaDays)
      _selectedDate.value = sdf.format(cal.time)
    } catch (_: Exception) {
      _selectedDate.value = MainUiState.todayIso()
    }
  }

  fun setAttendance(studentId: Long, status: String, note: String = "") {
    val date = _selectedDate.value
    val semester = uiState.value.settings.activeSemester
    viewModelScope.launch {
      repository.setAttendance(studentId, date, semester, status, note)
    }
  }

  fun markAllPresentForSelectedDate() {
    val state = uiState.value
    if (state.students.isEmpty()) {
      _messages.tryEmit("Belum ada data siswa.")
      return
    }
    viewModelScope.launch {
      repository.markAllPresentForDate(
        studentIds = state.students.map { it.id },
        dateIso = state.selectedAttendanceDate,
        semester = state.settings.activeSemester,
      )
      _messages.tryEmit("Semua siswa (${state.students.size}) ditandai Hadir pada ${state.selectedAttendanceDate}.")
    }
  }

  fun saveCategories(categories: List<GradeCategoryEntity>, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveCategories(categories)
        .onSuccess {
          val valid = AcademicCalculator.isWeightValid(categories)
          _messages.tryEmit(
            if (valid) "Kategori & bobot penilaian disimpan (100%)."
            else "Kategori disimpan. Perhatian: Total bobot belum 100%."
          )
          onSuccess()
        }
        .onFailure { err ->
          _messages.tryEmit(err.message ?: "Gagal menyimpan kategori.")
        }
    }
  }

  fun saveGradeEntry(entry: GradeEntryEntity, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveGradeEntry(entry)
        .onSuccess {
          _messages.tryEmit("Nilai disimpan.")
          onSuccess()
        }
        .onFailure { err ->
          _messages.tryEmit(err.message ?: "Gagal menyimpan nilai.")
        }
    }
  }

  fun saveBatchGrades(entries: List<GradeEntryEntity>, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveBatchGrades(entries)
        .onSuccess { count ->
          _messages.tryEmit("$count nilai siswa berhasil disimpan.")
          onSuccess()
        }
        .onFailure { err ->
          _messages.tryEmit(err.message ?: "Gagal menyimpan nilai massal.")
        }
    }
  }

  fun deleteGradeEntry(entry: GradeEntryEntity) {
    viewModelScope.launch {
      repository.deleteGradeEntry(entry)
      _messages.tryEmit("Entri nilai dihapus.")
    }
  }

  fun saveBehaviorLog(log: BehaviorLogEntity, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveBehaviorLog(log)
        .onSuccess {
          _messages.tryEmit("Catatan sikap disimpan.")
          onSuccess()
        }
        .onFailure { err ->
          _messages.tryEmit(err.message ?: "Gagal menyimpan catatan sikap.")
        }
    }
  }

  fun deleteBehaviorLog(log: BehaviorLogEntity) {
    viewModelScope.launch {
      repository.deleteBehaviorLog(log)
      _messages.tryEmit("Catatan sikap dihapus.")
    }
  }

  fun updateReportFilter(transform: (ReportFilter) -> ReportFilter) {
    _reportFilter.value = transform(_reportFilter.value)
  }

  fun persistRawImport(parsed: RawImportParseResult, onSuccess: () -> Unit = {}) {
    val state = uiState.value
    viewModelScope.launch {
      repository.persistRawImport(
        parsed = parsed,
        semester = state.settings.activeSemester,
        baseDateIso = MainUiState.todayIso(),
      ).onSuccess { count ->
        _messages.tryEmit("$count siswa dari data mentah disimpan ke database kelas.")
        onSuccess()
      }.onFailure { err ->
        _messages.tryEmit(err.message ?: "Gagal menyimpan data mentah.")
      }
    }
  }

  suspend fun buildBackupJson(): String {
    val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
    return repository.exportBackupJson(nowIso)
  }

  fun restoreBackupJson(json: String) {
    viewModelScope.launch {
      repository.restoreFromBackupJson(json)
        .onSuccess { payload ->
          _messages.tryEmit("Restore berhasil: ${payload.students.size} siswa dipulihkan.")
        }
        .onFailure { err ->
          _messages.tryEmit("Restore gagal: ${err.message ?: "Format JSON tidak valid"}")
        }
    }
  }

  fun buildCsvReport(): String {
    return BackupSerializer.generateReportCsv(uiState.value.report)
  }

  fun emitMessage(msg: String) {
    _messages.tryEmit(msg)
  }

  private data class BaseBundle(
    val settings: ClassSettingsEntity,
    val students: List<StudentEntity>,
    val attendance: List<AttendanceEntity>,
    val categories: List<GradeCategoryEntity>,
    val grades: List<GradeEntryEntity>,
  )
}
