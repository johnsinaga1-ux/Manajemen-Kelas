package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.backup.PdfPrintHelper
import com.example.ui.MainViewModel
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BehaviorScreen
import com.example.ui.screens.GradesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.theme.WaliKelasTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab(
  val title: String,
  val navLabel: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  STUDENTS("Data Siswa", "Siswa", Icons.Filled.Groups, Icons.Outlined.Groups),
  ATTENDANCE("Absensi Harian", "Absensi", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
  GRADES("Penilaian", "Nilai", Icons.Filled.Grade, Icons.Outlined.Grade),
  BEHAVIOR("Jurnal Sikap", "Jurnal", Icons.Filled.Psychology, Icons.Outlined.Psychology),
  REPORTS("Laporan & Rapor", "Laporan", Icons.Filled.Assessment, Icons.Outlined.Assessment),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WaliKelasTheme {
        WaliKelasApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaliKelasApp(viewModel: MainViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var currentTab by remember { mutableStateOf(MainTab.STUDENTS) }
  var focusedReportStudentId by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(Unit) {
    viewModel.messages.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  // Storage Access Framework launchers for CSV Export, JSON Backup, JSON Restore
  val exportCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val csv = viewModel.buildCsvReport()
        val ok = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
              out.write(csv.toByteArray(Charsets.UTF_8))
            }
          }.isSuccess
        }
        viewModel.emitMessage(if (ok) "File CSV laporan berhasil disimpan." else "Gagal menyimpan file CSV.")
      }
    }
  }

  val exportBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val json = viewModel.buildBackupJson()
        val ok = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
              out.write(json.toByteArray(Charsets.UTF_8))
            }
          }.isSuccess
        }
        viewModel.emitMessage(if (ok) "Backup JSON kelas berhasil disimpan." else "Gagal menyimpan backup JSON.")
      }
    }
  }

  val restoreBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val jsonText = withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
          }.getOrNull()
        }
        if (jsonText != null) {
          viewModel.restoreBackupJson(jsonText)
        } else {
          viewModel.emitMessage("Tidak dapat membaca file backup JSON.")
        }
      }
    }
  }

  val useRail = LocalConfiguration.current.screenWidthDp >= 600

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = currentTab.title,
            style = MaterialTheme.typography.titleLarge,
          )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    bottomBar = {
      if (!useRail) {
        NavigationBar {
          MainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
              selected = selected,
              onClick = { currentTab = tab },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.navLabel,
                )
              },
              label = { Text(tab.navLabel, maxLines = 1) },
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      if (useRail) {
        NavigationRail {
          MainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationRailItem(
              selected = selected,
              onClick = { currentTab = tab },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.navLabel,
                )
              },
              label = { Text(tab.navLabel, maxLines = 1) },
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxSize()) {
        when (currentTab) {
          MainTab.STUDENTS -> StudentsScreen(
            uiState = uiState,
            onSaveStudent = viewModel::saveStudent,
            onDeleteStudent = viewModel::deleteStudent,
            onImportCsvStudents = viewModel::importCsvResult,
            onSaveSettings = viewModel::saveSettings,
            onExportBackupJson = {
              val cleanClass = uiState.settings.className.replace(" ", "_")
              exportBackupLauncher.launch("Backup_WaliKelas_${cleanClass}.json")
            },
            onRestoreBackupJson = {
              restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onOpenStudentReport = { studentId ->
              focusedReportStudentId = studentId
              currentTab = MainTab.REPORTS
            },
          )

          MainTab.ATTENDANCE -> AttendanceScreen(
            uiState = uiState,
            onShiftDate = viewModel::shiftAttendanceDate,
            onSelectDate = viewModel::setAttendanceDate,
            onSetAttendance = viewModel::setAttendance,
            onMarkAllPresent = viewModel::markAllPresentForSelectedDate,
            onNavigateToStudents = { currentTab = MainTab.STUDENTS },
          )

          MainTab.GRADES -> GradesScreen(
            uiState = uiState,
            onSaveCategories = viewModel::saveCategories,
            onSaveSettings = viewModel::saveSettings,
            onSaveGradeEntry = viewModel::saveGradeEntry,
            onSaveBatchGrades = viewModel::saveBatchGrades,
            onDeleteGradeEntry = viewModel::deleteGradeEntry,
            onNavigateToStudents = { currentTab = MainTab.STUDENTS },
          )

          MainTab.BEHAVIOR -> BehaviorScreen(
            uiState = uiState,
            onSaveBehavior = viewModel::saveBehaviorLog,
            onDeleteBehavior = viewModel::deleteBehaviorLog,
            onNavigateToStudents = { currentTab = MainTab.STUDENTS },
          )

          MainTab.REPORTS -> ReportsScreen(
            uiState = uiState,
            focusedStudentId = focusedReportStudentId,
            onClearFocusedStudent = { focusedReportStudentId = null },
            onUpdateFilter = viewModel::updateReportFilter,
            onUpdateHomeroomNote = viewModel::updateHomeroomNote,
            onExportCsv = {
              val cleanClass = uiState.settings.className.replace(" ", "_")
              exportCsvLauncher.launch("Laporan_${cleanClass}.csv")
            },
            onPrintClassPdf = { periodLabel ->
              PdfPrintHelper.printClassReport(context, uiState.report, periodLabel)
            },
            onPrintMiniRaporPdf = { row, periodLabel ->
              PdfPrintHelper.printMiniRapor(context, uiState.report, row, periodLabel)
            },
            onPersistRawImport = viewModel::persistRawImport,
          )
        }
      }
    }
  }
}
