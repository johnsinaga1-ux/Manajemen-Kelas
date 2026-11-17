package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.AcademicCalculator
import com.example.domain.AnomalySeverity
import com.example.domain.BehaviorType
import com.example.domain.RawImportParseResult
import com.example.domain.ReportFilter
import com.example.domain.ReportFilterMode
import com.example.domain.SemesterOption
import com.example.domain.StudentReportRow
import com.example.ui.MainUiState
import com.example.ui.components.ClassroomEmptyState
import com.example.ui.components.DateChipButton
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.StatusPill
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  uiState: MainUiState,
  focusedStudentId: Long?,
  onClearFocusedStudent: () -> Unit,
  onUpdateFilter: ((ReportFilter) -> ReportFilter) -> Unit,
  onUpdateHomeroomNote: (Long, String?) -> Unit,
  onExportCsv: () -> Unit,
  onPrintClassPdf: (String) -> Unit,
  onPrintMiniRaporPdf: (StudentReportRow, String) -> Unit,
  onPersistRawImport: (RawImportParseResult, () -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }
  var selectedRaporStudentId by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(focusedStudentId) {
    if (focusedStudentId != null) {
      selectedRaporStudentId = focusedStudentId
      selectedSubTab = 2
      onClearFocusedStudent()
    }
  }

  val periodLabel = remember(uiState.reportFilter) { buildPeriodLabel(uiState.reportFilter, uiState) }

  Column(modifier = modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = selectedSubTab) {
      listOf("Ringkasan", "Rekap A-Z", "Rapor Mini", "Olah Mentah").forEachIndexed { idx, label ->
        Tab(
          selected = selectedSubTab == idx,
          onClick = { selectedSubTab = idx },
          text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
      }
    }

    when (selectedSubTab) {
      0 -> SummaryReportTab(
        uiState = uiState,
        periodLabel = periodLabel,
        onUpdateFilter = onUpdateFilter,
        onExportCsv = onExportCsv,
        onPrintClassPdf = { onPrintClassPdf(periodLabel) },
        onOpenStudentRapor = { studentId ->
          selectedRaporStudentId = studentId
          selectedSubTab = 2
        },
      )
      1 -> RecapTableTab(
        uiState = uiState,
        periodLabel = periodLabel,
        onExportCsv = onExportCsv,
        onPrintClassPdf = { onPrintClassPdf(periodLabel) },
        onSelectStudentRow = { studentId ->
          selectedRaporStudentId = studentId
          selectedSubTab = 2
        },
      )
      2 -> MiniRaporTab(
        uiState = uiState,
        selectedStudentId = selectedRaporStudentId,
        onSelectStudentId = { selectedRaporStudentId = it },
        periodLabel = periodLabel,
        onUpdateHomeroomNote = onUpdateHomeroomNote,
        onPrintMiniRapor = { row -> onPrintMiniRaporPdf(row, periodLabel) },
      )
      3 -> RawDataProcessorTab(
        kkm = uiState.settings.kkm,
        onPersistRawImport = onPersistRawImport,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFilterHeaderCard(
  uiState: MainUiState,
  onUpdateFilter: ((ReportFilter) -> ReportFilter) -> Unit,
  onExportCsv: () -> Unit,
  onPrintClassPdf: () -> Unit,
) {
  val filter = uiState.reportFilter
  var showStartPicker by remember { mutableStateOf(false) }
  var showEndPicker by remember { mutableStateOf(false) }
  var studentMenuExpanded by remember { mutableStateOf(false) }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onExportCsv,
          enabled = uiState.report.studentRows.isNotEmpty(),
          modifier = Modifier
            .weight(1f)
            .testTag("export_csv_button"),
        ) {
          Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export CSV")
        }
        Button(
          onClick = onPrintClassPdf,
          enabled = uiState.report.studentRows.isNotEmpty(),
          modifier = Modifier
            .weight(1f)
            .testTag("print_pdf_button"),
        ) {
          Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Cetak / PDF")
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ReportFilterMode.entries.forEach { mode ->
          FilterChip(
            selected = filter.mode == mode,
            onClick = { onUpdateFilter { it.copy(mode = mode) } },
            label = { Text(mode.label) },
          )
        }
      }

      when (filter.mode) {
        ReportFilterMode.SEMESTER -> {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SemesterOption.entries.forEach { sem ->
              FilterChip(
                selected = filter.semester == sem,
                onClick = { onUpdateFilter { it.copy(semester = sem) } },
                label = { Text(sem.value) },
              )
            }
          }
        }
        ReportFilterMode.MONTH -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(onClick = {
              onUpdateFilter { it.copy(monthIso = shiftMonthIso(it.monthIso, -1)) }
            }) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Bulan Sebelumnya")
            }
            Text(
              text = formatMonthIndonesian(filter.monthIso),
              style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = {
              onUpdateFilter { it.copy(monthIso = shiftMonthIso(it.monthIso, 1)) }
            }) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Bulan Berikutnya")
            }
          }
        }
        ReportFilterMode.DATE_RANGE -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DateChipButton(
              label = "Dari",
              dateIso = filter.startDateIso,
              onClick = { showStartPicker = true },
              modifier = Modifier.weight(1f),
            )
            DateChipButton(
              label = "Sampai",
              dateIso = filter.endDateIso,
              onClick = { showEndPicker = true },
              modifier = Modifier.weight(1f),
            )
          }
        }
        ReportFilterMode.STUDENT -> {
          val selectedStudent = uiState.students.firstOrNull { it.id == filter.studentId }
          ExposedDropdownMenuBox(
            expanded = studentMenuExpanded,
            onExpandedChange = { studentMenuExpanded = !studentMenuExpanded },
          ) {
            OutlinedTextField(
              value = selectedStudent?.fullName ?: "Semua Siswa",
              onValueChange = {},
              readOnly = true,
              label = { Text("Pilih Siswa") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = studentMenuExpanded,
              onDismissRequest = { studentMenuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("Semua Siswa") },
                onClick = {
                  onUpdateFilter { it.copy(studentId = null) }
                  studentMenuExpanded = false
                },
              )
              uiState.students.forEach { st ->
                DropdownMenuItem(
                  text = { Text("${st.fullName} (${st.nis})") },
                  onClick = {
                    onUpdateFilter { it.copy(studentId = st.id) }
                    studentMenuExpanded = false
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showStartPicker) {
    IsoDatePickerDialog(
      initialDateIso = filter.startDateIso,
      onDismiss = { showStartPicker = false },
      onDateSelected = { picked -> onUpdateFilter { it.copy(startDateIso = picked) } },
    )
  }
  if (showEndPicker) {
    IsoDatePickerDialog(
      initialDateIso = filter.endDateIso,
      onDismiss = { showEndPicker = false },
      onDateSelected = { picked -> onUpdateFilter { it.copy(endDateIso = picked) } },
    )
  }
}

@Composable
private fun SummaryReportTab(
  uiState: MainUiState,
  periodLabel: String,
  onUpdateFilter: ((ReportFilter) -> ReportFilter) -> Unit,
  onExportCsv: () -> Unit,
  onPrintClassPdf: () -> Unit,
  onOpenStudentRapor: (Long) -> Unit,
) {
  val report = uiState.report
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      ReportFilterHeaderCard(
        uiState = uiState,
        onUpdateFilter = onUpdateFilter,
        onExportCsv = onExportCsv,
        onPrintClassPdf = onPrintClassPdf,
      )
    }

    if (report.totalStudents == 0) {
      item {
        ClassroomEmptyState(
          title = "Belum Ada Data Laporan",
          description = "Tambahkan data siswa atau gunakan tab Olah Mentah untuk menganalisis salinan data Excel/CSV secara instan.",
          showIllustration = false,
        )
      }
    } else {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricKpiCard(
              label = "Total Siswa",
              value = "${report.totalStudents}",
              subtitle = "${report.totalEffectiveDays} hari efektif",
              icon = Icons.Outlined.Groups,
              accentColor = IndigoPrimary,
              modifier = Modifier.weight(1f),
            )
            MetricKpiCard(
              label = "Kehadiran Kelas",
              value = AcademicCalculator.formatPercent(report.averageAttendancePercent),
              subtitle = "Filter: $periodLabel",
              icon = Icons.Outlined.VerifiedUser,
              accentColor = EmeraldSuccess,
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricKpiCard(
              label = "Rata-rata Nilai",
              value = if (report.isWeightValid) AcademicCalculator.formatScore(report.averageFinalScore) else "-",
              subtitle = "Max ${AcademicCalculator.formatScore(report.highestScore)} • Min ${AcademicCalculator.formatScore(report.lowestScore)}",
              icon = Icons.Outlined.Analytics,
              accentColor = IndigoPrimary,
              modifier = Modifier.weight(1f),
            )
            MetricKpiCard(
              label = "Ketuntasan KKM",
              value = if (report.isWeightValid) AcademicCalculator.formatPercent(report.passingRatePercent) else "-",
              subtitle = "${report.passingCount}/${report.totalStudents} tuntas (KKM ${AcademicCalculator.formatScore(report.settings.kkm)})",
              icon = Icons.Outlined.School,
              accentColor = if (report.passingRatePercent >= 75.0) EmeraldSuccess else AmberWarning,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Anomaly verification card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (report.anomalies.isEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = if (report.anomalies.isEmpty()) EmeraldSuccess else AmberWarning,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Verifikasi Data & Anomali (${report.anomalies.size})",
                style = MaterialTheme.typography.titleSmall,
              )
            }
            if (report.anomalies.isEmpty()) {
              Text(
                text = "Seluruh data siswa, kehadiran, dan bobot penilaian terverifikasi lengkap tanpa anomali.",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldSuccess,
              )
            } else {
              report.anomalies.take(6).forEach { anomaly ->
                val isErr = anomaly.severity == AnomalySeverity.ERROR
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isErr) CrimsonContainer else AmberContainer,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                      text = anomaly.title,
                      style = MaterialTheme.typography.labelLarge,
                      color = if (isErr) OnCrimsonContainer else OnAmberContainer,
                    )
                    Text(
                      text = anomaly.detail,
                      style = MaterialTheme.typography.bodySmall,
                      color = if (isErr) OnCrimsonContainer else OnAmberContainer,
                    )
                  }
                }
              }
              if (report.anomalies.size > 6) {
                Text(
                  text = "+ ${report.anomalies.size - 6} catatan verifikasi lainnya.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // Priority follow up section
      item {
        Text(
          text = "Prioritas Tindak Lanjut (${report.priorityFollowUps.size} Siswa)",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (report.priorityFollowUps.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Semua siswa berada di atas batas minimal kehadiran (85%) dan KKM (${AcademicCalculator.formatScore(report.settings.kkm)}).",
              style = MaterialTheme.typography.bodyMedium,
              color = OnEmeraldContainer,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      } else {
        items(report.priorityFollowUps, key = { "${it.studentId}_${it.studentName}" }) { item ->
          Card(
            onClick = { if (item.studentId > 0L) onOpenStudentRapor(item.studentId) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.35f)),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${item.studentName} (${item.nis})",
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.weight(1f),
                )
                StatusPill(
                  text = "Perlu Tindakan",
                  containerColor = CrimsonContainer,
                  contentColor = OnCrimsonContainer,
                )
              }
              Text(
                text = item.reasons.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = CrimsonError,
              )
              Text(
                text = "Rekomendasi: ${item.recommendation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RecapTableTab(
  uiState: MainUiState,
  periodLabel: String,
  onExportCsv: () -> Unit,
  onPrintClassPdf: () -> Unit,
  onSelectStudentRow: (Long) -> Unit,
) {
  val report = uiState.report
  if (report.studentRows.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      ClassroomEmptyState(
        title = "Tabel Rekapitulasi Kosong",
        description = "Belum ada data siswa untuk direkapitulasi pada filter $periodLabel.",
        showIllustration = false,
      )
    }
    return
  }

  val hScroll = rememberScrollState()
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Tabel Rekapitulasi Utama (A–Z)", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Ketuk baris siswa untuk membuka Rapor Mini • $periodLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(onClick = onExportCsv) {
            Icon(Icons.Outlined.Download, contentDescription = "Export CSV")
          }
          IconButton(onClick = onPrintClassPdf) {
            Icon(Icons.Outlined.Print, contentDescription = "Cetak PDF")
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.horizontalScroll(hScroll)) {
          // Header
          Row(
            modifier = Modifier
              .background(MaterialTheme.colorScheme.primary)
              .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TableHeaderCell("No", 36.dp)
            TableHeaderCell("Nama Siswa", 160.dp, TextAlign.Start)
            TableHeaderCell("H", 38.dp)
            TableHeaderCell("S", 38.dp)
            TableHeaderCell("I", 38.dp)
            TableHeaderCell("A", 38.dp)
            TableHeaderCell("T", 38.dp)
            TableHeaderCell("% Hadir", 76.dp)
            report.categories.forEach { cat ->
              TableHeaderCell("${cat.shortCode}\n(${cat.weightPercent.toInt()}%)", 66.dp)
            }
            TableHeaderCell("Akhir", 68.dp)
            TableHeaderCell("Pred", 46.dp)
            TableHeaderCell("Status", 82.dp)
          }

          report.studentRows.forEachIndexed { idx, row ->
            Row(
              modifier = Modifier
                .clickable { onSelectStudentRow(row.studentId) }
                .background(
                  if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surface
                )
                .padding(vertical = 9.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TableBodyCell("${idx + 1}", 36.dp, mono = true)
              TableBodyCell(row.fullName, 160.dp, align = TextAlign.Start, bold = true)
              TableBodyCell("${row.hadirCount}", 38.dp, mono = true)
              TableBodyCell("${row.sakitCount}", 38.dp, mono = true)
              TableBodyCell("${row.izinCount}", 38.dp, mono = true)
              TableBodyCell("${row.alpaCount}", 38.dp, mono = true, highlightError = row.alpaCount > 3)
              TableBodyCell("${row.terlambatCount}", 38.dp, mono = true)
              TableBodyCell(
                AcademicCalculator.formatPercent(row.attendancePercent),
                76.dp,
                mono = true,
                highlightError = row.totalEffectiveDays > 0 && row.attendancePercent < 85.0,
              )
              report.categories.forEach { cat ->
                val cs = row.categorySummaries.firstOrNull { it.categoryId == cat.id }
                TableBodyCell(AcademicCalculator.formatScore(cs?.averageScore), 66.dp, mono = true)
              }
              TableBodyCell(
                AcademicCalculator.formatScore(row.finalScore),
                68.dp,
                mono = true,
                bold = true,
                highlightError = row.isPassing == false,
              )
              TableBodyCell(row.predicate, 46.dp, bold = true)
              Box(modifier = Modifier.width(82.dp), contentAlignment = Alignment.Center) {
                StatusPill(
                  text = when (row.isPassing) {
                    true -> "Tuntas"
                    false -> "Remedial"
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          }
        }
      }
    }
  }
}

@Composable
private fun TableHeaderCell(
  text: String,
  width: Dp,
  align: TextAlign = TextAlign.Center,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    color = MaterialTheme.colorScheme.onPrimary,
    textAlign = align,
    modifier = Modifier
      .width(width)
      .padding(horizontal = 4.dp),
  )
}

@Composable
private fun TableBodyCell(
  text: String,
  width: Dp,
  align: TextAlign = TextAlign.Center,
  mono: Boolean = false,
  bold: Boolean = false,
  highlightError: Boolean = false,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall.copy(
      fontFamily = if (mono) JetBrainsMono else MaterialTheme.typography.bodySmall.fontFamily,
      fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    ),
    color = if (highlightError) CrimsonError else MaterialTheme.colorScheme.onSurface,
    textAlign = align,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier
      .width(width)
      .padding(horizontal = 4.dp),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniRaporTab(
  uiState: MainUiState,
  selectedStudentId: Long?,
  onSelectStudentId: (Long) -> Unit,
  periodLabel: String,
  onUpdateHomeroomNote: (Long, String?) -> Unit,
  onPrintMiniRapor: (StudentReportRow) -> Unit,
) {
  val rows = uiState.report.studentRows
  if (rows.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      ClassroomEmptyState(
        title = "Belum Ada Rapor Mini",
        description = "Tambahkan siswa terlebih dahulu untuk melihat Rapor Mini dan draf catatan wali kelas.",
        showIllustration = false,
      )
    }
    return
  }

  val activeRow = rows.firstOrNull { it.studentId == selectedStudentId } ?: rows.first()
  var studentMenuExpanded by remember { mutableStateOf(false) }
  var showEditNoteDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ExposedDropdownMenuBox(
          expanded = studentMenuExpanded,
          onExpandedChange = { studentMenuExpanded = !studentMenuExpanded },
          modifier = Modifier.weight(1f),
        ) {
          OutlinedTextField(
            value = "${activeRow.fullName} (${activeRow.nis})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Pilih Siswa") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = studentMenuExpanded,
            onDismissRequest = { studentMenuExpanded = false },
          ) {
            rows.forEach { r ->
              DropdownMenuItem(
                text = { Text("${r.fullName} (${r.nis})") },
                onClick = {
                  onSelectStudentId(r.studentId)
                  studentMenuExpanded = false
                },
              )
            }
          }
        }
        Button(
          onClick = { onPrintMiniRapor(activeRow) },
          modifier = Modifier.testTag("print_mini_rapor_button"),
        ) {
          Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("PDF")
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("RAPOR MINI SISWA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
              Text(activeRow.fullName, style = MaterialTheme.typography.headlineMedium)
              Text(
                text = "NIS ${activeRow.nis} • ${if (activeRow.gender == "P") "Perempuan" else "Laki-laki"} • $periodLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (activeRow.parentContact.isNotBlank()) {
                Text(
                  text = "Kontak Orang Tua: ${activeRow.parentContact}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            StatusPill(
              text = when (activeRow.isPassing) {
                true -> "Tuntas (${activeRow.predicate})"
                false -> "Remedial (${activeRow.predicate})"
                null -> "Bobot != 100%"
              },
              containerColor = when (activeRow.isPassing) {
                true -> EmeraldContainer
                false -> CrimsonContainer
                null -> AmberContainer
              },
              contentColor = when (activeRow.isPassing) {
                true -> OnEmeraldContainer
                false -> OnCrimsonContainer
                null -> OnAmberContainer
              },
            )
          }

          HorizontalDivider()

          Text("A. Rekapitulasi Kehadiran", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StatusPill("H: ${activeRow.hadirCount}", EmeraldContainer, OnEmeraldContainer)
            StatusPill("T: ${activeRow.terlambatCount}", AmberContainer, OnAmberContainer)
            StatusPill("S: ${activeRow.sakitCount}", IndigoContainer, OnIndigoContainer)
            StatusPill("I: ${activeRow.izinCount}", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill("A: ${activeRow.alpaCount}", CrimsonContainer, OnCrimsonContainer)
          }
          Text(
            text = "Persentase Kehadiran: ${AcademicCalculator.formatPercent(activeRow.attendancePercent)} (${activeRow.totalEffectiveDays} hari efektif)",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
          )

          HorizontalDivider()

          Text("B. Rincian Nilai Akademik", style = MaterialTheme.typography.titleSmall)
          activeRow.categorySummaries.forEach { cat ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${cat.categoryName} (${AcademicCalculator.formatScore(cat.weightPercent)}%)",
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = AcademicCalculator.formatScore(cat.averageScore),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold),
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Nilai Akhir (Predikat ${activeRow.predicate})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = AcademicCalculator.formatScore(activeRow.finalScore),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }

          HorizontalDivider()

          Text("C. Catatan Sikap (${activeRow.behaviorLogs.size})", style = MaterialTheme.typography.titleSmall)
          if (activeRow.behaviorLogs.isEmpty()) {
            Text(
              text = "Belum ada catatan kejadian pada periode ini.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            activeRow.behaviorLogs.forEach { log ->
              val pos = log.type == BehaviorType.POSITIF.code
              Text(
                text = "• [${log.dateIso}] (${if (pos) "Positif" else "Negatif"}) ${log.description}" +
                  if (log.followUp.isNotBlank()) " — Tindak lanjut: ${log.followUp}" else "",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }

          HorizontalDivider()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("D. Catatan Wali Kelas", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { showEditNoteDialog = true }) {
              Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit")
            }
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = activeRow.effectiveHomeroomNote,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
    }
  }

  if (showEditNoteDialog) {
    var noteDraft by remember(activeRow.studentId) {
      mutableStateOf(activeRow.effectiveHomeroomNote)
    }
    AlertDialog(
      onDismissRequest = { showEditNoteDialog = false },
      title = { Text("Catatan Wali Kelas") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = noteDraft,
            onValueChange = { noteDraft = it },
            label = { Text("Evaluasi Personal Siswa") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
          )
          TextButton(
            onClick = {
              onUpdateHomeroomNote(activeRow.studentId, null)
              showEditNoteDialog = false
            },
          ) {
            Text("Gunakan Draf Otomatis Sistem")
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateHomeroomNote(activeRow.studentId, noteDraft)
            showEditNoteDialog = false
          },
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNoteDialog = false }) { Text("Batal") }
      },
    )
  }
}

@Composable
private fun RawDataProcessorTab(
  kkm: Double,
  onPersistRawImport: (RawImportParseResult, () -> Unit) -> Unit,
) {
  var rawText by remember { mutableStateOf("") }
  val parsed = remember(rawText, kkm) {
    AcademicCalculator.processRawAcademicText(rawText, kkm)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pengolah Data Mentah (Excel / CSV)", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "Tempel data mentah dengan urutan kolom: Nama, H, S, I, A, Tugas (20%), UH (30%), UTS (25%), UAS (25%). Mendukung pemisah Tab (salin dari Excel), koma, atau titik koma.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            label = { Text("Tempel Data Mentah Kelas di Sini") },
            placeholder = { Text("Nama, H, S, I, A, Tugas, UH, UTS, UAS\nAhmad, 18, 1, 0, 1, 82, 78, 80, 84") },
            minLines = 5,
            maxLines = 9,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("raw_academic_input"),
          )
          if (parsed.rows.isNotEmpty()) {
            Button(
              onClick = {
                onPersistRawImport(parsed) {
                  rawText = ""
                }
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Simpan ${parsed.rows.size} Siswa ke Database Kelas")
            }
          }
        }
      }
    }

    if (parsed.anomalies.isNotEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = AmberContainer),
          border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = AmberWarning)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Verifikasi Anomali Data (${parsed.anomalies.size})", style = MaterialTheme.typography.titleSmall)
            }
            parsed.anomalies.forEach { a ->
              Text("• ${a.title}: ${a.detail}", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    if (parsed.rows.isNotEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Ringkasan Eksekutif", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "Total: ${parsed.rows.size} siswa • Rata-rata Hadir: ${AcademicCalculator.formatPercent(parsed.averageAttendancePercent)} • Rata-rata Nilai: ${AcademicCalculator.formatScore(parsed.averageFinalScore)} • Ketuntasan: ${AcademicCalculator.formatPercent(parsed.passingRatePercent)}",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      item {
        Text("2. Rekapitulasi A–Z & Catatan Personal (${parsed.rows.size})", style = MaterialTheme.typography.titleSmall)
      }

      items(parsed.rows, key = { "${it.nis}_${it.name}" }) { r ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(r.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
              StatusPill(
                text = "${AcademicCalculator.formatScore(r.finalScore)} (${r.predicate}) • ${if (r.isPassing) "Tuntas" else "Remedial"}",
                containerColor = if (r.isPassing) EmeraldContainer else CrimsonContainer,
                contentColor = if (r.isPassing) OnEmeraldContainer else OnCrimsonContainer,
              )
            }
            Text(
              text = "H:${r.h} S:${r.s} I:${r.i} A:${r.a} (${AcademicCalculator.formatPercent(r.attendancePercent)}) | Tugas:${AcademicCalculator.formatScore(r.tugas)} UH:${AcademicCalculator.formatScore(r.uh)} UTS:${AcademicCalculator.formatScore(r.uts)} UAS:${AcademicCalculator.formatScore(r.uas)}",
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Catatan: ${r.homeroomNote}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
}

private fun buildPeriodLabel(filter: ReportFilter, uiState: MainUiState): String {
  return when (filter.mode) {
    ReportFilterMode.SEMESTER -> filter.semester.label
    ReportFilterMode.MONTH -> formatMonthIndonesian(filter.monthIso)
    ReportFilterMode.DATE_RANGE -> "${filter.startDateIso} s.d. ${filter.endDateIso}"
    ReportFilterMode.STUDENT -> {
      val name = uiState.students.firstOrNull { it.id == filter.studentId }?.fullName ?: "Semua Siswa"
      "Siswa: $name"
    }
  }
}

private fun shiftMonthIso(monthIso: String, deltaMonths: Int): String {
  return try {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    val cal = Calendar.getInstance()
    cal.time = sdf.parse(monthIso) ?: Calendar.getInstance().time
    cal.add(Calendar.MONTH, deltaMonths)
    sdf.format(cal.time)
  } catch (_: Exception) {
    SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
  }
}

private fun formatMonthIndonesian(monthIso: String): String {
  return try {
    val inFmt = SimpleDateFormat("yyyy-MM", Locale.US)
    val outFmt = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    val date = inFmt.parse(monthIso) ?: return monthIso
    outFmt.format(date)
  } catch (_: Exception) {
    monthIso
  }
}
