package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.StudentEntity
import com.example.domain.BehaviorType
import com.example.ui.MainUiState
import com.example.ui.components.ClassroomEmptyState
import com.example.ui.components.DateChipButton
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.OnEmeraldContainer

@Composable
fun BehaviorScreen(
  uiState: MainUiState,
  onSaveBehavior: (BehaviorLogEntity, () -> Unit) -> Unit,
  onDeleteBehavior: (BehaviorLogEntity) -> Unit,
  onNavigateToStudents: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var typeFilter by remember { mutableStateOf("ALL") } // ALL, POSITIF, NEGATIF
  var searchQuery by remember { mutableStateOf("") }
  var editingLog by remember { mutableStateOf<BehaviorLogEntity?>(null) }
  var showDialog by remember { mutableStateOf(false) }

  val studentMap = remember(uiState.students) { uiState.students.associateBy { it.id } }

  val filteredLogs = remember(uiState.behaviors, typeFilter, searchQuery, studentMap) {
    val q = searchQuery.trim()
    uiState.behaviors.filter { log ->
      val matchesType = typeFilter == "ALL" || log.type == typeFilter
      val studentName = studentMap[log.studentId]?.fullName.orEmpty()
      val matchesQuery = q.isBlank() ||
        studentName.contains(q, ignoreCase = true) ||
        log.description.contains(q, ignoreCase = true) ||
        log.followUp.contains(q, ignoreCase = true)
      matchesType && matchesQuery
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      if (uiState.students.isNotEmpty()) {
        FloatingActionButton(
          onClick = {
            editingLog = null
            showDialog = true
          },
          modifier = Modifier.testTag("add_behavior_fab"),
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
          Icon(Icons.Outlined.Add, contentDescription = "Catat Sikap & Kejadian")
        }
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
      if (uiState.students.isEmpty()) {
        item {
          ClassroomEmptyState(
            title = "Jurnal Sikap Belum Tersedia",
            description = "Tambahkan data siswa terlebih dahulu untuk mulai mencatat jurnal perilaku positif/negatif dan tindak lanjutnya.",
            primaryActionLabel = "Ke Data Siswa",
            onPrimaryAction = onNavigateToStudents,
            showIllustration = false,
          )
        }
      } else {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Cari nama siswa atau deskripsi kejadian...") },
              leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = typeFilter == "ALL",
                onClick = { typeFilter = "ALL" },
                label = { Text("Semua (${uiState.behaviors.size})") },
              )
              FilterChip(
                selected = typeFilter == BehaviorType.POSITIF.code,
                onClick = { typeFilter = BehaviorType.POSITIF.code },
                label = { Text("Positif (${uiState.behaviors.count { it.type == BehaviorType.POSITIF.code }})") },
              )
              FilterChip(
                selected = typeFilter == BehaviorType.NEGATIF.code,
                onClick = { typeFilter = BehaviorType.NEGATIF.code },
                label = { Text("Negatif (${uiState.behaviors.count { it.type == BehaviorType.NEGATIF.code }})") },
              )
            }
          }
        }

        if (filteredLogs.isEmpty()) {
          item {
            ClassroomEmptyState(
              title = "Belum Ada Catatan Kejadian",
              description = "Catat perilaku positif maupun kejadian yang memerlukan pembinaan beserta tanggal dan tindak lanjutnya.",
              primaryActionLabel = "Catat Sikap",
              onPrimaryAction = {
                editingLog = null
                showDialog = true
              },
              showIllustration = false,
            )
          }
        } else {
          items(filteredLogs, key = { it.id }) { log ->
            val student = studentMap[log.studentId]
            BehaviorLogCard(
              log = log,
              studentName = student?.fullName ?: "Siswa",
              nis = student?.nis.orEmpty(),
              onEdit = {
                editingLog = log
                showDialog = true
              },
              onDelete = { onDeleteBehavior(log) },
            )
          }
        }
      }
    }
  }

  if (showDialog && uiState.students.isNotEmpty()) {
    BehaviorLogDialog(
      initial = editingLog,
      students = uiState.students,
      activeSemester = uiState.settings.activeSemester,
      onDismiss = { showDialog = false },
      onSave = { log ->
        onSaveBehavior(log) { showDialog = false }
      },
    )
  }
}

@Composable
private fun BehaviorLogCard(
  log: BehaviorLogEntity,
  studentName: String,
  nis: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val isPositive = log.type == BehaviorType.POSITIF.code
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = if (isPositive) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbDown,
          contentDescription = null,
          tint = if (isPositive) EmeraldSuccess else CrimsonError,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = studentName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${log.dateIso} • Sem. ${log.semester}" + (if (nis.isNotBlank()) " • NIS $nis" else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = if (isPositive) "Positif" else "Negatif",
          containerColor = if (isPositive) EmeraldContainer else CrimsonContainer,
          contentColor = if (isPositive) OnEmeraldContainer else OnCrimsonContainer,
        )
        IconButton(onClick = onEdit) {
          Icon(Icons.Outlined.Edit, contentDescription = "Edit catatan", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus catatan", tint = CrimsonError)
        }
      }
      Text(
        text = log.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (log.followUp.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Tindak Lanjut: ${log.followUp}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BehaviorLogDialog(
  initial: BehaviorLogEntity?,
  students: List<StudentEntity>,
  activeSemester: String,
  onDismiss: () -> Unit,
  onSave: (BehaviorLogEntity) -> Unit,
) {
  var selectedStudent by remember {
    mutableStateOf(students.firstOrNull { it.id == initial?.studentId } ?: students.first())
  }
  var type by remember { mutableStateOf(initial?.type ?: BehaviorType.POSITIF.code) }
  var dateIso by remember { mutableStateOf(initial?.dateIso ?: MainUiState.todayIso()) }
  var semester by remember { mutableStateOf(initial?.semester ?: activeSemester) }
  var description by remember { mutableStateOf(initial?.description.orEmpty()) }
  var followUp by remember { mutableStateOf(initial?.followUp.orEmpty()) }
  var studentExpanded by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Catat Sikap & Kejadian" else "Edit Catatan Sikap") },
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = type == BehaviorType.POSITIF.code,
            onClick = { type = BehaviorType.POSITIF.code },
            label = { Text("Perilaku Positif") },
          )
          FilterChip(
            selected = type == BehaviorType.NEGATIF.code,
            onClick = { type = BehaviorType.NEGATIF.code },
            label = { Text("Perilaku Negatif") },
          )
        }

        DateChipButton(
          label = "Tanggal",
          dateIso = dateIso,
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = description,
          onValueChange = {
            description = it
            errorText = null
          },
          label = { Text("Deskripsi Kejadian / Sikap *") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = followUp,
          onValueChange = { followUp = it },
          label = { Text("Tindak Lanjut Wali Kelas") },
          minLines = 2,
          maxLines = 3,
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
          if (description.isBlank()) {
            errorText = "Deskripsi kejadian wajib diisi."
            return@Button
          }
          onSave(
            (initial ?: BehaviorLogEntity(
              studentId = selectedStudent.id,
              dateIso = dateIso,
              semester = semester,
              type = type,
              description = description.trim(),
              followUp = followUp.trim(),
            )).copy(
              studentId = selectedStudent.id,
              dateIso = dateIso,
              semester = semester,
              type = type,
              description = description.trim(),
              followUp = followUp.trim(),
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
