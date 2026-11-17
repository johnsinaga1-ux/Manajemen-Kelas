package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.StudentEntity
import com.example.domain.AcademicCalculator
import com.example.domain.StudentCsvParseResult
import com.example.domain.StudentCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.MainUiState
import com.example.ui.components.ClassroomEmptyState
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnIndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.OnTealContainer

@Composable
fun StudentsScreen(
  uiState: MainUiState,
  onSaveStudent: (StudentEntity, () -> Unit) -> Unit,
  onDeleteStudent: (StudentEntity) -> Unit,
  onImportCsvStudents: (StudentCsvParseResult, (Int) -> Unit) -> Unit,
  onSaveSettings: (ClassSettingsEntity) -> Unit,
  onExportBackupJson: () -> Unit,
  onRestoreBackupJson: () -> Unit,
  onOpenStudentReport: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var genderFilter by remember { mutableStateOf("ALL") } // ALL, L, P
  var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
  var showStudentDialog by remember { mutableStateOf(false) }
  var showBulkImportDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }

  val filteredStudents = remember(uiState.students, searchQuery, genderFilter) {
    uiState.students.filter { student ->
      val matchesGender = genderFilter == "ALL" || student.gender.equals(genderFilter, ignoreCase = true)
      val q = searchQuery.trim()
      val matchesQuery = q.isBlank() ||
        student.fullName.contains(q, ignoreCase = true) ||
        student.nis.contains(q, ignoreCase = true) ||
        student.parentContact.contains(q, ignoreCase = true)
      matchesGender && matchesQuery
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingStudent = null
          showStudentDialog = true
        },
        modifier = Modifier.testTag("add_student_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(Icons.Outlined.Add, contentDescription = "Tambah Siswa")
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        ClassIdentityHeaderCard(
          settings = uiState.settings,
          studentCount = uiState.students.size,
          onOpenSettings = { showSettingsDialog = true },
          onOpenBulkImport = { showBulkImportDialog = true },
          onExportBackupJson = onExportBackupJson,
          onRestoreBackupJson = onRestoreBackupJson,
        )
      }

      if (uiState.students.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("search_student_input"),
              placeholder = { Text("Cari nama siswa, NIS/NISN, atau kontak...") },
              leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilterChip(
                selected = genderFilter == "ALL",
                onClick = { genderFilter = "ALL" },
                label = { Text("Semua (${uiState.students.size})") },
              )
              FilterChip(
                selected = genderFilter == "L",
                onClick = { genderFilter = "L" },
                label = { Text("Laki-laki (${uiState.students.count { it.gender == "L" }})") },
              )
              FilterChip(
                selected = genderFilter == "P",
                onClick = { genderFilter = "P" },
                label = { Text("Perempuan (${uiState.students.count { it.gender == "P" }})") },
              )
            }
          }
        }
      }

      if (uiState.students.isEmpty()) {
        item {
          ClassroomEmptyState(
            title = "Belum Ada Data Siswa",
            description = "Tambahkan siswa satu per satu atau tempel langsung daftar siswa dari Excel/CSV untuk mulai mencatat absensi dan penilaian.",
            primaryActionLabel = "Tambah Siswa",
            onPrimaryAction = {
              editingStudent = null
              showStudentDialog = true
            },
            secondaryActionLabel = "Impor Excel/CSV",
            onSecondaryAction = { showBulkImportDialog = true },
          )
        }
      } else if (filteredStudents.isEmpty()) {
        item {
          ClassroomEmptyState(
            title = "Siswa Tidak Ditemukan",
            description = "Tidak ada siswa yang cocok dengan kata kunci \"$searchQuery\".",
            showIllustration = false,
          )
        }
      } else {
        items(filteredStudents, key = { it.id }) { student ->
          StudentItemCard(
            student = student,
            onEdit = {
              editingStudent = student
              showStudentDialog = true
            },
            onDelete = { studentToDelete = student },
            onOpenReport = { onOpenStudentReport(student.id) },
          )
        }
      }
    }
  }

  if (showStudentDialog) {
    StudentFormDialog(
      initial = editingStudent,
      onDismiss = { showStudentDialog = false },
      onSave = { entity ->
        onSaveStudent(entity) { showStudentDialog = false }
      },
    )
  }

  if (showBulkImportDialog) {
    BulkImportStudentsDialog(
      existingNisSet = uiState.students.map { it.nis }.toSet(),
      onDismiss = { showBulkImportDialog = false },
      onConfirmImport = { parseResult ->
        onImportCsvStudents(parseResult) { showBulkImportDialog = false }
      },
    )
  }

  if (showSettingsDialog) {
    ClassSettingsDialog(
      initial = uiState.settings,
      onDismiss = { showSettingsDialog = false },
      onSave = { updated ->
        onSaveSettings(updated)
        showSettingsDialog = false
      },
    )
  }

  studentToDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { studentToDelete = null },
      title = { Text("Hapus Data Siswa?") },
      text = {
        Text("Data ${target.fullName} (NIS: ${target.nis}) beserta seluruh riwayat absensi, nilai, dan catatan sikapnya akan dihapus permanen.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteStudent(target)
            studentToDelete = null
          },
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { studentToDelete = null }) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun ClassIdentityHeaderCard(
  settings: ClassSettingsEntity,
  studentCount: Int,
  onOpenSettings: () -> Unit,
  onOpenBulkImport: () -> Unit,
  onExportBackupJson: () -> Unit,
  onRestoreBackupJson: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${settings.className} • ${settings.schoolName}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "TA ${settings.academicYear} • Semester ${settings.activeSemester} • KKM ${AcademicCalculator.formatScore(settings.kkm)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
          )
        }
        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier.testTag("class_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Pengaturan Kelas",
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
          modifier = Modifier.weight(1f),
          onClick = onOpenBulkImport,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.UploadFile,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Impor CSV",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimary,
              maxLines = 1,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
          modifier = Modifier.weight(1f),
          onClick = onExportBackupJson,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Backup,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Backup",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimary,
              maxLines = 1,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
          modifier = Modifier.weight(1f),
          onClick = onRestoreBackupJson,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Restore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Restore",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimary,
              maxLines = 1,
            )
          }
        }
      }
      if (studentCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Total Siswa Terdaftar: $studentCount siswa",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@Composable
private fun StudentItemCard(
  student: StudentEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onOpenReport: () -> Unit,
) {
  val initials = student.fullName
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "S" }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(if (student.gender == "P") TealContainer else IndigoContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initials,
          style = MaterialTheme.typography.titleSmall,
          color = if (student.gender == "P") OnTealContainer else OnIndigoContainer,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = student.fullName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          StatusPill(
            text = if (student.gender == "P") "P" else "L",
            containerColor = if (student.gender == "P") TealContainer else IndigoContainer,
            contentColor = if (student.gender == "P") OnTealContainer else OnIndigoContainer,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "NIS: ${student.nis}",
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (student.parentContact.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Phone,
              contentDescription = null,
              modifier = Modifier.size(13.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = student.parentContact,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
      IconButton(onClick = onOpenReport) {
        Icon(
          imageVector = Icons.Outlined.Description,
          contentDescription = "Lihat Rapor Mini ${student.fullName}",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      IconButton(onClick = onEdit) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "Edit ${student.fullName}",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus ${student.fullName}",
          tint = CrimsonError,
        )
      }
    }
  }
}

@Composable
private fun StudentFormDialog(
  initial: StudentEntity?,
  onDismiss: () -> Unit,
  onSave: (StudentEntity) -> Unit,
) {
  var nis by remember { mutableStateOf(initial?.nis.orEmpty()) }
  var fullName by remember { mutableStateOf(initial?.fullName.orEmpty()) }
  var gender by remember { mutableStateOf(initial?.gender ?: "L") }
  var parentContact by remember { mutableStateOf(initial?.parentContact.orEmpty()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Tambah Siswa Baru" else "Edit Data Siswa") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = nis,
          onValueChange = {
            nis = it
            errorText = null
          },
          label = { Text("NIS / NISN *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("student_nis_input"),
        )
        OutlinedTextField(
          value = fullName,
          onValueChange = {
            fullName = it
            errorText = null
          },
          label = { Text("Nama Lengkap *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("student_name_input"),
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Jenis Kelamin:", style = MaterialTheme.typography.labelLarge)
          FilterChip(
            selected = gender == "L",
            onClick = { gender = "L" },
            label = { Text("Laki-laki (L)") },
          )
          FilterChip(
            selected = gender == "P",
            onClick = { gender = "P" },
            label = { Text("Perempuan (P)") },
          )
        }
        OutlinedTextField(
          value = parentContact,
          onValueChange = { parentContact = it },
          label = { Text("Kontak Orang Tua (HP / WA)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        errorText?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (nis.isBlank() || fullName.isBlank()) {
            errorText = "NIS dan Nama Lengkap wajib diisi."
            return@Button
          }
          onSave(
            (initial ?: StudentEntity(nis = "", fullName = "", gender = "L", parentContact = "")).copy(
              nis = nis.trim(),
              fullName = fullName.trim(),
              gender = gender,
              parentContact = parentContact.trim(),
            )
          )
        },
        modifier = Modifier.testTag("save_student_button"),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

@Composable
private fun BulkImportStudentsDialog(
  existingNisSet: Set<String>,
  onDismiss: () -> Unit,
  onConfirmImport: (StudentCsvParseResult) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var rawText by remember { mutableStateOf("") }
  var fileError by remember { mutableStateOf<String?>(null) }

  val csvFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val loaded = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
          }.getOrNull()
        }
        if (loaded != null) {
          rawText = loaded
          fileError = null
        } else {
          fileError = "Gagal membaca file CSV yang dipilih."
        }
      }
    }
  }

  val parseResult = remember(rawText, existingNisSet) {
    StudentCsvParser.parse(rawText, existingNisSet)
  }
  val validCount = parseResult.validEntities.size
  val invalidCount = parseResult.invalidCount

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Impor Siswa dari CSV / Excel") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Pilih file .csv atau tempel langsung data dari Excel/CSV.\nHeader otomatis dikenali (NIS/NISN, Nama Lengkap, L/P, Kontak Orang Tua).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = {
            csvFilePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_csv_file_button"),
        ) {
          Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Pilih File .CSV dari Perangkat")
        }
        fileError?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
          value = rawText,
          onValueChange = {
            rawText = it
            fileError = null
          },
          label = { Text("Atau Tempel Konten CSV di Sini") },
          placeholder = {
            Text("NIS, Nama Lengkap, L/P, Kontak Orang Tua\n2026001, Ahmad Fauzi, L, 08123456789\n2026002, Siti Rahma, P, 08129876543")
          },
          minLines = 5,
          maxLines = 8,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("csv_import_textfield"),
        )
        if (parseResult.previewRows.isNotEmpty()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            StatusPill(
              text = "Siap Impor: $validCount",
              containerColor = EmeraldContainer,
              contentColor = OnEmeraldContainer,
            )
            if (invalidCount > 0) {
              StatusPill(
                text = "Dilewati: $invalidCount",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
            StatusPill(
              text = parseResult.delimiterLabel,
              containerColor = IndigoContainer,
              contentColor = OnIndigoContainer,
            )
          }
          parseResult.previewRows.take(6).forEach { row ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "Baris ${row.lineNumber}: ${row.nis.ifBlank { "-" }} • ${row.fullName.ifBlank { "-" }} (${row.gender})" +
                    if (row.parentContact.isNotBlank()) " • ${row.parentContact}" else "",
                  style = MaterialTheme.typography.labelMedium,
                )
                row.errorMessage?.let { err ->
                  Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
          if (parseResult.previewRows.size > 6) {
            Text(
              text = "+ ${parseResult.previewRows.size - 6} baris lainnya...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmImport(parseResult) },
        enabled = validCount > 0,
        modifier = Modifier.testTag("confirm_csv_import_button"),
      ) {
        Text("Impor ($validCount)")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

@Composable
private fun ClassSettingsDialog(
  initial: ClassSettingsEntity,
  onDismiss: () -> Unit,
  onSave: (ClassSettingsEntity) -> Unit,
) {
  var className by remember { mutableStateOf(initial.className) }
  var schoolName by remember { mutableStateOf(initial.schoolName) }
  var academicYear by remember { mutableStateOf(initial.academicYear) }
  var activeSemester by remember { mutableStateOf(initial.activeSemester) }
  var kkmText by remember { mutableStateOf(initial.kkm.toInt().toString()) }
  var teacherName by remember { mutableStateOf(initial.homeroomTeacherName) }
  var teacherNip by remember { mutableStateOf(initial.homeroomTeacherNip) }
  var principalName by remember { mutableStateOf(initial.principalName) }
  var principalNip by remember { mutableStateOf(initial.principalNip) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Pengaturan Kelas & Tanda Tangan") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = className,
          onValueChange = { className = it },
          label = { Text("Nama Kelas") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = schoolName,
          onValueChange = { schoolName = it },
          label = { Text("Nama Sekolah") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = academicYear,
            onValueChange = { academicYear = it },
            label = { Text("Tahun Ajaran") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = kkmText,
            onValueChange = { kkmText = it },
            label = { Text("KKM (0-100)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(0.8f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = activeSemester == "Ganjil",
            onClick = { activeSemester = "Ganjil" },
            label = { Text("Semester Ganjil") },
          )
          FilterChip(
            selected = activeSemester == "Genap",
            onClick = { activeSemester = "Genap" },
            label = { Text("Semester Genap") },
          )
        }
        OutlinedTextField(
          value = teacherName,
          onValueChange = { teacherName = it },
          label = { Text("Nama Wali Kelas (untuk PDF)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = teacherNip,
          onValueChange = { teacherNip = it },
          label = { Text("NIP Wali Kelas") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = principalName,
          onValueChange = { principalName = it },
          label = { Text("Nama Kepala Sekolah (untuk PDF)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = principalNip,
          onValueChange = { principalNip = it },
          label = { Text("NIP Kepala Sekolah") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedKkm = kkmText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 75.0
          onSave(
            initial.copy(
              className = className.trim().ifBlank { "Kelas" },
              schoolName = schoolName.trim().ifBlank { "Sekolah" },
              academicYear = academicYear.trim().ifBlank { "2026/2027" },
              activeSemester = activeSemester,
              kkm = parsedKkm,
              homeroomTeacherName = teacherName.trim(),
              homeroomTeacherNip = teacherNip.trim(),
              principalName = principalName.trim(),
              principalNip = principalNip.trim(),
            )
          )
        },
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
