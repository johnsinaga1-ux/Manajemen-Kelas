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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.AttendanceEntity
import com.example.data.db.StudentEntity
import com.example.domain.AttendanceStatus
import com.example.ui.MainUiState
import com.example.ui.components.ClassroomEmptyState
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatReadableIndonesianDate
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnCrimsonContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnIndigoContainer
import com.example.ui.theme.OnTealContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealInfo

@Composable
fun AttendanceScreen(
  uiState: MainUiState,
  onShiftDate: (Int) -> Unit,
  onSelectDate: (String) -> Unit,
  onSetAttendance: (Long, String, String) -> Unit,
  onMarkAllPresent: () -> Unit,
  onNavigateToStudents: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDatePicker by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var noteTarget by remember { mutableStateOf<Pair<StudentEntity, AttendanceEntity?>?>(null) }

  val attendanceForDate = remember(uiState.attendance, uiState.selectedAttendanceDate) {
    uiState.attendance
      .filter { it.dateIso == uiState.selectedAttendanceDate }
      .associateBy { it.studentId }
  }

  val filteredStudents = remember(uiState.students, searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) uiState.students
    else uiState.students.filter {
      it.fullName.contains(q, ignoreCase = true) || it.nis.contains(q, ignoreCase = true)
    }
  }

  val hCount = attendanceForDate.values.count { it.status == "H" }
  val sCount = attendanceForDate.values.count { it.status == "S" }
  val iCount = attendanceForDate.values.count { it.status == "I" }
  val aCount = attendanceForDate.values.count { it.status == "A" }
  val tCount = attendanceForDate.values.count { it.status == "T" }
  val unrecordedCount = (uiState.students.size - attendanceForDate.size).coerceAtLeast(0)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(
              onClick = { onShiftDate(-1) },
            ) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Hari Sebelumnya")
            }
            Surface(
              onClick = { showDatePicker = true },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.weight(1f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.CalendarToday,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = formatReadableIndonesianDate(uiState.selectedAttendanceDate),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = "${uiState.selectedAttendanceDate} • Sem. ${uiState.settings.activeSemester}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                  )
                }
              }
            }
            IconButton(
              onClick = { onShiftDate(1) },
            ) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Hari Berikutnya")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StatusPill("H: $hCount", EmeraldContainer, OnEmeraldContainer)
            StatusPill("T: $tCount", AmberContainer, OnAmberContainer)
            StatusPill("S: $sCount", TealContainer, OnTealContainer)
            StatusPill("I: $iCount", IndigoContainer, OnIndigoContainer)
            StatusPill("A: $aCount", CrimsonContainer, OnCrimsonContainer)
            if (unrecordedCount > 0) {
              StatusPill("Kosong: $unrecordedCount", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Button(
            onClick = onMarkAllPresent,
            enabled = uiState.students.isNotEmpty(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("mark_all_present_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
          ) {
            Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tandai Semua Hadir (${uiState.students.size} Siswa)")
          }
        }
      }
    }

    if (uiState.students.isEmpty()) {
      item {
        ClassroomEmptyState(
          title = "Daftar Absensi Kosong",
          description = "Tambahkan data siswa terlebih dahulu di tab Siswa sebelum mencatat absensi harian.",
          primaryActionLabel = "Kelola Data Siswa",
          onPrimaryAction = onNavigateToStudents,
          showIllustration = false,
        )
      }
    } else {
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Filter nama atau NIS siswa...") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
        )
      }

      items(filteredStudents, key = { it.id }) { student ->
        val record = attendanceForDate[student.id]
        AttendanceStudentRow(
          student = student,
          record = record,
          onSelectStatus = { status ->
            onSetAttendance(student.id, status.code, record?.note.orEmpty())
          },
          onEditNote = { noteTarget = student to record },
        )
      }
    }
  }

  if (showDatePicker) {
    IsoDatePickerDialog(
      initialDateIso = uiState.selectedAttendanceDate,
      onDismiss = { showDatePicker = false },
      onDateSelected = { onSelectDate(it) },
    )
  }

  noteTarget?.let { (student, existing) ->
    var noteText by remember(student.id, existing?.note) { mutableStateOf(existing?.note.orEmpty()) }
    val currentStatus = existing?.status ?: AttendanceStatus.HADIR.code
    AlertDialog(
      onDismissRequest = { noteTarget = null },
      title = { Text("Keterangan Absensi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "${student.fullName} • ${uiState.selectedAttendanceDate} (Status: $currentStatus)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Catatan (misal: surat dokter / izin keluarga)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSetAttendance(student.id, currentStatus, noteText)
            noteTarget = null
          },
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { noteTarget = null }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun AttendanceStudentRow(
  student: StudentEntity,
  record: AttendanceEntity?,
  onSelectStatus: (AttendanceStatus) -> Unit,
  onEditNote: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = student.fullName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "NIS ${student.nis}" + (if (!record?.note.isNullOrBlank()) " • ${record?.note}" else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = onEditNote) {
          Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = "Tambah catatan absensi ${student.fullName}",
            tint = if (!record?.note.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AttendanceStatus.entries.forEach { st ->
          val selected = record?.status == st.code
          val (bg, fg) = statusColors(st, selected)
          Surface(
            onClick = { onSelectStatus(st) },
            shape = RoundedCornerShape(10.dp),
            color = bg,
            border = if (selected) BorderStroke(1.5.dp, fg) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .weight(1f)
              .height(44.dp),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = st.code,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = fg,
              )
              Text(
                text = st.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = if (selected) 0.95f else 0.7f),
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun statusColors(status: AttendanceStatus, selected: Boolean): Pair<Color, Color> {
  if (!selected) {
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) to MaterialTheme.colorScheme.onSurfaceVariant
  }
  return when (status) {
    AttendanceStatus.HADIR -> EmeraldContainer to EmeraldSuccess
    AttendanceStatus.SAKIT -> TealContainer to TealInfo
    AttendanceStatus.IZIN -> IndigoContainer to IndigoPrimary
    AttendanceStatus.ALPA -> CrimsonContainer to CrimsonError
    AttendanceStatus.TERLAMBAT -> AmberContainer to AmberWarning
  }
}
