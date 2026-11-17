package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import com.example.domain.AcademicCalculator
import com.example.ui.MainUiState
import com.example.ui.components.ClassroomEmptyState
import com.example.ui.components.DateChipButton
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.WeightValidationBanner
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.OnEmeraldContainer
import kotlin.math.abs

@Composable
fun GradesScreen(
  uiState: MainUiState,
  onSaveCategories: (List<GradeCategoryEntity>, () -> Unit) -> Unit,
  onSaveSettings: (ClassSettingsEntity) -> Unit,
  onSaveGradeEntry: (GradeEntryEntity, () -> Unit) -> Unit,
  onSaveBatchGrades: (List<GradeEntryEntity>, () -> Unit) -> Unit,
  onDeleteGradeEntry: (GradeEntryEntity) -> Unit,
  onNavigateToStudents: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
  var showWeightDialog by remember { mutableStateOf(false) }
  var showSingleGradeDialog by remember { mutableStateOf(false) }
  var editingGradeEntry by remember { mutableStateOf<GradeEntryEntity?>(null) }
  var showBatchGradeDialog by remember { mutableStateOf(false) }

  val studentMap = remember(uiState.students) { uiState.students.associateBy { it.id } }
  val categoryMap = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

  val filteredGrades = remember(uiState.grades, selectedCategoryId) {
    if (selectedCategoryId == null) uiState.grades
    else uiState.grades.filter { it.categoryId == selectedCategoryId }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      WeightValidationBanner(
        totalWeight = uiState.report.totalWeightPercent,
        isValid = uiState.report.isWeightValid,
        onConfigureClick = { showWeightDialog = true },
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { showBatchGradeDialog = true },
          enabled = uiState.students.isNotEmpty() && uiState.categories.isNotEmpty(),
          modifier = Modifier
            .weight(1f)
            .testTag("batch_grade_button"),
        ) {
          Icon(Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Input Massal", maxLines = 1)
        }
        OutlinedButton(
          onClick = {
            editingGradeEntry = null
            showSingleGradeDialog = true
          },
          enabled = uiState.students.isNotEmpty() && uiState.categories.isNotEmpty(),
          modifier = Modifier
            .weight(1f)
            .testTag("single_grade_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Nilai Satuan", maxLines = 1)
        }
      }
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedCategoryId == null,
          onClick = { selectedCategoryId = null },
          label = { Text("Semua (${uiState.grades.size})") },
        )
        uiState.categories.forEach { cat ->
          val count = uiState.grades.count { it.categoryId == cat.id }
          FilterChip(
            selected = selectedCategoryId == cat.id,
            onClick = { selectedCategoryId = cat.id },
            label = {
              Text("${cat.shortCode} ${AcademicCalculator.formatScore(cat.weightPercent)}% ($count)")
            },
          )
        }
      }
    }

    if (uiState.students.isEmpty()) {
      item {
        ClassroomEmptyState(
          title = "Belum Ada Siswa",
          description = "Tambahkan data siswa terlebih dahulu untuk mulai menginput nilai Tugas, UH, UTS, dan UAS.",
          primaryActionLabel = "Ke Data Siswa",
          onPrimaryAction = onNavigateToStudents,
          showIllustration = false,
        )
      }
    } else {
      // Student summary preview card
      if (uiState.report.studentRows.isNotEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Ringkasan Nilai Akhir (KKM ${AcademicCalculator.formatScore(uiState.settings.kkm)})",
                style = MaterialTheme.typography.titleSmall,
              )
              uiState.report.studentRows.forEach { row ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = row.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = AcademicCalculator.formatScore(row.finalScore),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                    color = when (row.isPassing) {
                      true -> EmeraldSuccess
                      false -> CrimsonError
                      null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  StatusPill(
                    text = when (row.isPassing) {
                      true -> "${row.predicate} • Tuntas"
                      false -> "${row.predicate} • Remedial"
                      null -> "-"
                    },
                    containerColor = when (row.isPassing) {
                      true -> EmeraldContainer
                      false -> CrimsonContainer
                      null -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when (row.isPassing) {
                      true -> OnEmeraldContainer
                      false -> OnCrimsonContainer
                      null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                }
              }
            }
          }
        }
      }

      if (filteredGrades.isEmpty()) {
        item {
          ClassroomEmptyState(
            title = "Belum Ada Catatan Nilai",
            description = "Gunakan tombol Input Massal untuk mengisi satu penilaian untuk seluruh kelas, atau Nilai Satuan untuk satu siswa.",
            showIllustration = false,
          )
        }
      } else {
        item {
          Text(
            text = "Riwayat Entri Nilai (${filteredGrades.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(filteredGrades, key = { it.id }) { entry ->
          val student = studentMap[entry.studentId]
          val cat = categoryMap[entry.categoryId]
          GradeEntryRowCard(
            entry = entry,
            studentName = student?.fullName ?: "Siswa",
            categoryName = cat?.shortCode ?: "-",
            kkm = uiState.settings.kkm,
            onEdit = {
              editingGradeEntry = entry
              showSingleGradeDialog = true
            },
            onDelete = { onDeleteGradeEntry(entry) },
          )
        }
      }
    }
  }

  if (showWeightDialog) {
    CategoryWeightConfigDialog(
      initialCategories = uiState.categories,
      initialKkm = uiState.settings.kkm,
      onDismiss = { showWeightDialog = false },
      onSave = { cats, newKkm ->
        onSaveSettings(uiState.settings.copy(kkm = newKkm))
        onSaveCategories(cats) { showWeightDialog = false }
      },
    )
  }

  if (showSingleGradeDialog) {
    SingleGradeDialog(
      initial = editingGradeEntry,
      students = uiState.students,
      categories = uiState.categories,
      activeSemester = uiState.settings.activeSemester,
      onDismiss = { showSingleGradeDialog = false },
      onSave = { entry ->
        onSaveGradeEntry(entry) { showSingleGradeDialog = false }
      },
    )
  }

  if (showBatchGradeDialog) {
    BatchGradeDialog(
      students = uiState.students,
      categories = uiState.categories,
      activeSemester = uiState.settings.activeSemester,
      onDismiss = { showBatchGradeDialog = false },
      onSaveBatch = { entries ->
        onSaveBatchGrades(entries) { showBatchGradeDialog = false }
      },
    )
  }
}

@Composable
private fun GradeEntryRowCard(
  entry: GradeEntryEntity,
  studentName: String,
  categoryName: String,
  kkm: Double,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = studentName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$categoryName • ${entry.title} • ${entry.dateIso} (${entry.semester})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = AcademicCalculator.formatScore(entry.score),
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = JetBrainsMono,
          fontWeight = FontWeight.Bold,
        ),
        color = if (entry.score >= kkm) EmeraldSuccess else CrimsonError,
      )
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit nilai", tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus nilai", tint = CrimsonError)
      }
    }
  }
}

@Composable
private fun CategoryWeightConfigDialog(
  initialCategories: List<GradeCategoryEntity>,
  initialKkm: Double,
  onDismiss: () -> Unit,
  onSave: (List<GradeCategoryEntity>, Double) -> Unit,
) {
  var rows by remember { mutableStateOf(initialCategories) }
  var kkmText by remember { mutableStateOf(initialKkm.toInt().toString()) }
  val totalWeight = rows.sumOf { it.weightPercent }
  val isExact100 = abs(totalWeight - 100.0) < 0.01

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Atur Bobot Penilaian & KKM") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = kkmText,
          onValueChange = { kkmText = it },
          label = { Text("KKM (Kriteria Ketuntasan Minimal)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          text = "Total Bobot: ${AcademicCalculator.formatScore(totalWeight)}% ${if (isExact100) "(Valid)" else "(Harus tepat 100%)"}",
          style = MaterialTheme.typography.labelLarge,
          color = if (isExact100) EmeraldSuccess else CrimsonError,
        )
        rows.forEachIndexed { idx, cat ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = cat.name,
              onValueChange = { newName ->
                rows = rows.toMutableList().also {
                  it[idx] = cat.copy(name = newName, shortCode = newName.take(5))
                }
              },
              label = { Text("Kategori") },
              singleLine = true,
              modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
              value = if (cat.weightPercent == 0.0) "" else cat.weightPercent.toInt().toString(),
              onValueChange = { text ->
                val num = text.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                rows = rows.toMutableList().also { it[idx] = cat.copy(weightPercent = num) }
              },
              label = { Text("Bobot %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.9f),
            )
            if (rows.size > 1) {
              IconButton(
                onClick = {
                  rows = rows.toMutableList().also { it.removeAt(idx) }
                },
              ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus kategori", tint = CrimsonError)
              }
            }
          }
        }
        TextButton(
          onClick = {
            rows = rows + GradeCategoryEntity(
              name = "Kategori ${rows.size + 1}",
              shortCode = "Kat${rows.size + 1}",
              weightPercent = 0.0,
              sortOrder = rows.size,
            )
          },
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tambah Kategori")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val kkm = kkmText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 75.0
          onSave(rows, kkm)
        },
        enabled = isExact100 && rows.all { it.name.isNotBlank() },
      ) {
        Text("Simpan (100%)")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleGradeDialog(
  initial: GradeEntryEntity?,
  students: List<StudentEntity>,
  categories: List<GradeCategoryEntity>,
  activeSemester: String,
  onDismiss: () -> Unit,
  onSave: (GradeEntryEntity) -> Unit,
) {
  var selectedStudent by remember {
    mutableStateOf(students.firstOrNull { it.id == initial?.studentId } ?: students.first())
  }
  var selectedCategory by remember {
    mutableStateOf(categories.firstOrNull { it.id == initial?.categoryId } ?: categories.first())
  }
  var title by remember { mutableStateOf(initial?.title ?: selectedCategory.name) }
  var scoreText by remember { mutableStateOf(initial?.score?.toString().orEmpty()) }
  var dateIso by remember { mutableStateOf(initial?.dateIso ?: MainUiState.todayIso()) }
  var semester by remember { mutableStateOf(initial?.semester ?: activeSemester) }
  var studentExpanded by remember { mutableStateOf(false) }
  var catExpanded by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Input Nilai Siswa" else "Edit Nilai") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ExposedDropdownMenuBox(
          expanded = studentExpanded,
          onExpandedChange = { studentExpanded = !studentExpanded },
        ) {
          OutlinedTextField(
            value = selectedStudent.fullName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Siswa") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = studentExpanded,
            onDismissRequest = { studentExpanded = false },
          ) {
            students.forEach { st ->
              DropdownMenuItem(
                text = { Text("${st.fullName} (${st.nis})") },
                onClick = {
                  selectedStudent = st
                  studentExpanded = false
                },
              )
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = !catExpanded },
        ) {
          OutlinedTextField(
            value = "${selectedCategory.name} (${AcademicCalculator.formatScore(selectedCategory.weightPercent)}%)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori Nilai") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false },
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text("${cat.name} (${AcademicCalculator.formatScore(cat.weightPercent)}%)") },
                onClick = {
                  selectedCategory = cat
                  if (initial == null) title = cat.name
                  catExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Judul Penilaian (misal: Tugas 1 / UH Bab 2)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = scoreText,
          onValueChange = {
            scoreText = it
            errorText = null
          },
          label = { Text("Nilai (0 – 100)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        DateChipButton(
          label = "Tanggal",
          dateIso = dateIso,
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = semester == "Ganjil",
            onClick = { semester = "Ganjil" },
            label = { Text("Ganjil") },
          )
          FilterChip(
            selected = semester == "Genap",
            onClick = { semester = "Genap" },
            label = { Text("Genap") },
          )
        }

        errorText?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val score = scoreText.replace(",", ".").toDoubleOrNull()
          if (score == null || score < 0.0 || score > 100.0) {
            errorText = "Nilai harus angka valid antara 0 sampai 100."
            return@Button
          }
          if (title.isBlank()) {
            errorText = "Judul penilaian wajib diisi."
            return@Button
          }
          onSave(
            (initial ?: GradeEntryEntity(
              studentId = selectedStudent.id,
              categoryId = selectedCategory.id,
              title = title.trim(),
              score = score,
              dateIso = dateIso,
              semester = semester,
            )).copy(
              studentId = selectedStudent.id,
              categoryId = selectedCategory.id,
              title = title.trim(),
              score = AcademicCalculator.round2(score),
              dateIso = dateIso,
              semester = semester,
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

  if (showDatePicker) {
    IsoDatePickerDialog(
      initialDateIso = dateIso,
      onDismiss = { showDatePicker = false },
      onDateSelected = { dateIso = it },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchGradeDialog(
  students: List<StudentEntity>,
  categories: List<GradeCategoryEntity>,
  activeSemester: String,
  onDismiss: () -> Unit,
  onSaveBatch: (List<GradeEntryEntity>) -> Unit,
) {
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var title by remember { mutableStateOf("${categories.first().name} 1") }
  var dateIso by remember { mutableStateOf(MainUiState.todayIso()) }
  var catExpanded by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  val scoreInputs = remember { mutableStateMapOf<Long, String>() }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Input Nilai Massal Kelas") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = !catExpanded },
        ) {
          OutlinedTextField(
            value = "${selectedCategory.name} (${AcademicCalculator.formatScore(selectedCategory.weightPercent)}%)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false },
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  selectedCategory = cat
                  title = "${cat.name} 1"
                  catExpanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Judul Penilaian") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        DateChipButton(
          label = "Tanggal",
          dateIso = dateIso,
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Isi nilai 0–100 untuk siswa yang dinilai (kosongkan jika belum ada):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        students.forEach { student ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = student.fullName,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
              value = scoreInputs[student.id].orEmpty(),
              onValueChange = {
                scoreInputs[student.id] = it
                errorText = null
              },
              placeholder = { Text("0-100") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )
          }
        }
        errorText?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            errorText = "Judul penilaian wajib diisi."
            return@Button
          }
          val entries = mutableListOf<GradeEntryEntity>()
          for (student in students) {
            val raw = scoreInputs[student.id]?.trim().orEmpty()
            if (raw.isBlank()) continue
            val num = raw.replace(",", ".").toDoubleOrNull()
            if (num == null || num < 0.0 || num > 100.0) {
              errorText = "Nilai ${student.fullName} ($raw) harus antara 0 sampai 100."
              return@Button
            }
            entries += GradeEntryEntity(
              studentId = student.id,
              categoryId = selectedCategory.id,
              title = title.trim(),
              score = AcademicCalculator.round2(num),
              dateIso = dateIso,
              semester = activeSemester,
            )
          }
          if (entries.isEmpty()) {
            errorText = "Minimal isi satu nilai siswa."
            return@Button
          }
          onSaveBatch(entries)
        },
      ) {
        Text("Simpan Semua")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )

  if (showDatePicker) {
    IsoDatePickerDialog(
      initialDateIso = dateIso,
      onDismiss = { showDatePicker = false },
      onDateSelected = { dateIso = it },
    )
  }
}
