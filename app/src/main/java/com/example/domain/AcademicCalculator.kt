package com.example.domain

import com.example.data.db.AttendanceEntity
import com.example.data.db.BehaviorLogEntity
import com.example.data.db.ClassSettingsEntity
import com.example.data.db.GradeCategoryEntity
import com.example.data.db.GradeEntryEntity
import com.example.data.db.StudentEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

object AcademicCalculator {

  fun round2(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return 0.0
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
  }

  fun formatScore(value: Double?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "-"
    return String.format(Locale.US, "%.2f", round2(value))
  }

  fun formatPercent(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0.00%"
    return String.format(Locale.US, "%.2f%%", round2(value))
  }

  fun predicateFor(score: Double): String = when {
    score >= 90.0 -> "A"
    score >= 80.0 -> "B"
    score >= 70.0 -> "C"
    else -> "D"
  }

  fun isWeightValid(categories: List<GradeCategoryEntity>): Boolean {
    if (categories.isEmpty()) return false
    val total = categories.sumOf { it.weightPercent }
    return abs(total - 100.0) < 0.01
  }

  private fun matchesFilter(
    dateIso: String,
    semester: String,
    filter: ReportFilter,
  ): Boolean {
    return when (filter.mode) {
      ReportFilterMode.SEMESTER -> {
        filter.semester == SemesterOption.SEMUA ||
          semester.equals(filter.semester.value, ignoreCase = true)
      }
      ReportFilterMode.MONTH -> {
        val prefix = filter.monthIso.trim()
        prefix.isBlank() || dateIso.startsWith(prefix)
      }
      ReportFilterMode.DATE_RANGE -> {
        val afterStart = filter.startDateIso.isBlank() || dateIso >= filter.startDateIso
        val beforeEnd = filter.endDateIso.isBlank() || dateIso <= filter.endDateIso
        afterStart && beforeEnd
      }
      ReportFilterMode.STUDENT -> {
        filter.semester == SemesterOption.SEMUA ||
          semester.equals(filter.semester.value, ignoreCase = true)
      }
    }
  }

  fun generateReport(
    settings: ClassSettingsEntity,
    students: List<StudentEntity>,
    categories: List<GradeCategoryEntity>,
    attendance: List<AttendanceEntity>,
    grades: List<GradeEntryEntity>,
    behaviors: List<BehaviorLogEntity>,
    filter: ReportFilter,
  ): ClassReportSummary {
    val sortedCategories = categories.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    val totalWeight = round2(sortedCategories.sumOf { it.weightPercent })
    val weightValid = isWeightValid(sortedCategories)

    val filteredAttendance = attendance.filter { matchesFilter(it.dateIso, it.semester, filter) }
    val filteredGrades = grades.filter { matchesFilter(it.dateIso, it.semester, filter) }
    val filteredBehaviors = behaviors.filter { matchesFilter(it.dateIso, it.semester, filter) }

    val targetStudents = students
      .filter { filter.mode != ReportFilterMode.STUDENT || filter.studentId == null || it.id == filter.studentId }
      .sortedBy { it.fullName.lowercase(Locale.getDefault()) }

    val effectiveDates = filteredAttendance.map { it.dateIso }.distinct()
    val totalEffectiveDays = effectiveDates.size

    val anomalies = mutableListOf<DataAnomaly>()

    if (!weightValid) {
      anomalies += DataAnomaly(
        severity = AnomalySeverity.ERROR,
        title = "Total Bobot Penilaian Tidak 100%",
        detail = "Total bobot kategori saat ini ${formatScore(totalWeight)}%. Nilai Akhir tidak dapat dihitung sampai total bobot tepat 100%.",
      )
    }

    // Check duplicate names across all students
    val duplicateNames = students
      .groupBy { it.fullName.trim().lowercase(Locale.getDefault()) }
      .filter { it.key.isNotBlank() && it.value.size > 1 }
    duplicateNames.forEach { (_, dupes) ->
      anomalies += DataAnomaly(
        severity = AnomalySeverity.WARNING,
        title = "Nama Siswa Ganda Terdeteksi",
        detail = "Nama \"${dupes.first().fullName}\" muncul ${dupes.size} kali (NIS: ${dupes.joinToString { it.nis }}).",
      )
    }

    // Check out-of-range grades
    val outOfRangeGrades = filteredGrades.filter { it.score < 0.0 || it.score > 100.0 || it.score.isNaN() }
    if (outOfRangeGrades.isNotEmpty()) {
      anomalies += DataAnomaly(
        severity = AnomalySeverity.ERROR,
        title = "Nilai Di Luar Rentang 0–100",
        detail = "Terdapat ${outOfRangeGrades.size} entri nilai di luar rentang valid 0–100.",
      )
    }

    val attendanceByStudent = filteredAttendance.groupBy { it.studentId }
    val gradesByStudent = filteredGrades.groupBy { it.studentId }
    val behaviorsByStudent = filteredBehaviors.groupBy { it.studentId }

    val studentRows = targetStudents.map { student ->
      val attRecords = attendanceByStudent[student.id].orEmpty()
      val hCount = attRecords.count { it.status == AttendanceStatus.HADIR.code }
      val sCount = attRecords.count { it.status == AttendanceStatus.SAKIT.code }
      val iCount = attRecords.count { it.status == AttendanceStatus.IZIN.code }
      val aCount = attRecords.count { it.status == AttendanceStatus.ALPA.code }
      val tCount = attRecords.count { it.status == AttendanceStatus.TERLAMBAT.code }

      val denominator = if (totalEffectiveDays > 0) totalEffectiveDays else attRecords.size
      val presentTotal = hCount + tCount
      val attPercent = if (denominator > 0) {
        round2(((presentTotal.toDouble() / denominator.toDouble()) * 100.0).coerceIn(0.0, 100.0))
      } else {
        0.0
      }

      if (totalEffectiveDays > 0 && attRecords.size < totalEffectiveDays) {
        val missingDays = totalEffectiveDays - attRecords.size
        anomalies += DataAnomaly(
          severity = AnomalySeverity.WARNING,
          title = "Absensi Belum Lengkap: ${student.fullName}",
          detail = "Belum ada catatan kehadiran untuk $missingDays dari $totalEffectiveDays hari efektif pada periode filter.",
        )
      }

      val studentGrades = gradesByStudent[student.id].orEmpty()
      val categorySummaries = sortedCategories.map { cat ->
        val entries = studentGrades.filter { it.categoryId == cat.id }
        val avg = if (entries.isEmpty()) null else round2(entries.map { it.score }.average())
        CategoryScoreSummary(
          categoryId = cat.id,
          categoryName = cat.name,
          shortCode = cat.shortCode,
          weightPercent = cat.weightPercent,
          averageScore = avg,
          entryCount = entries.size,
        )
      }

      val missingCats = categorySummaries.filter { it.averageScore == null && it.weightPercent > 0 }
      if (missingCats.isNotEmpty() && studentGrades.isNotEmpty()) {
        anomalies += DataAnomaly(
          severity = AnomalySeverity.WARNING,
          title = "Kategori Nilai Kosong: ${student.fullName}",
          detail = "Kategori ${missingCats.joinToString { it.shortCode }} masih kosong (diperlakukan sebagai 0 dalam perhitungan).",
        )
      } else if (studentGrades.isEmpty() && targetStudents.isNotEmpty()) {
        anomalies += DataAnomaly(
          severity = AnomalySeverity.WARNING,
          title = "Belum Ada Nilai: ${student.fullName}",
          detail = "Siswa belum memiliki data nilai pada periode ini (dihitung 0.00).",
        )
      }

      val finalScore = if (weightValid) {
        round2(
          categorySummaries.sumOf { sum ->
            (sum.averageScore ?: 0.0) * (sum.weightPercent / 100.0)
          }
        )
      } else {
        null
      }

      val predicate = finalScore?.let { predicateFor(it) } ?: "-"
      val isPassing = finalScore?.let { it >= settings.kkm }
      val studentBehaviors = behaviorsByStudent[student.id].orEmpty()

      val autoNote = buildHomeroomNote(
        studentName = student.fullName,
        attendancePercent = attPercent,
        totalEffectiveDays = totalEffectiveDays,
        alpaCount = aCount,
        finalScore = finalScore,
        predicate = predicate,
        kkm = settings.kkm,
        categorySummaries = categorySummaries,
        behaviors = studentBehaviors,
      )

      StudentReportRow(
        studentId = student.id,
        nis = student.nis,
        fullName = student.fullName,
        gender = student.gender,
        parentContact = student.parentContact,
        hadirCount = hCount,
        sakitCount = sCount,
        izinCount = iCount,
        alpaCount = aCount,
        terlambatCount = tCount,
        totalEffectiveDays = totalEffectiveDays,
        attendancePercent = attPercent,
        categorySummaries = categorySummaries,
        finalScore = finalScore,
        predicate = predicate,
        isPassing = isPassing,
        behaviorLogs = studentBehaviors,
        autoHomeroomNote = autoNote,
        customHomeroomNote = student.homeroomNoteOverride,
      )
    }

    val priorityList = studentRows.mapNotNull { row ->
      val reasons = mutableListOf<String>()
      val actions = mutableListOf<String>()

      if (row.alpaCount > 3) {
        reasons += "Alpa ${row.alpaCount} kali (> 3)"
        actions += "Hubungi orang tua/wali dan jadwalkan pembinaan kedisiplinan"
      }
      if (row.totalEffectiveDays > 0 && row.attendancePercent < 85.0) {
        reasons += "Kehadiran ${formatPercent(row.attendancePercent)} (< 85%)"
        actions += "Pantau kehadiran harian dan konfirmasi kendala siswa"
      }
      if (row.finalScore != null && row.finalScore < settings.kkm) {
        val lowestCat = row.categorySummaries.minByOrNull { it.averageScore ?: 0.0 }
        reasons += "Nilai Akhir ${formatScore(row.finalScore)} (< KKM ${formatScore(settings.kkm)})"
        actions += if (lowestCat != null) {
          "Berikan remedial dan pendampingan pada kategori ${lowestCat.categoryName}"
        } else {
          "Jadwalkan program remedial"
        }
      }

      if (reasons.isEmpty()) null
      else PriorityActionItem(
        studentId = row.studentId,
        studentName = row.fullName,
        nis = row.nis,
        attendancePercent = row.attendancePercent,
        alpaCount = row.alpaCount,
        finalScore = row.finalScore,
        reasons = reasons,
        recommendation = actions.distinct().joinToString("; ") + ".",
      )
    }

    val avgAttendance = if (studentRows.isEmpty() || totalEffectiveDays == 0) 0.0
    else round2(studentRows.map { it.attendancePercent }.average())

    val validScores = studentRows.mapNotNull { it.finalScore }
    val avgFinal = if (validScores.isEmpty()) 0.0 else round2(validScores.average())
    val highest = validScores.maxOrNull()?.let { round2(it) }
    val lowest = validScores.minOrNull()?.let { round2(it) }
    val passingCount = studentRows.count { it.isPassing == true }
    val passingRate = if (studentRows.isEmpty() || !weightValid) 0.0
    else round2((passingCount.toDouble() / studentRows.size.toDouble()) * 100.0)

    return ClassReportSummary(
      settings = settings,
      categories = sortedCategories,
      totalWeightPercent = totalWeight,
      isWeightValid = weightValid,
      totalStudents = studentRows.size,
      totalEffectiveDays = totalEffectiveDays,
      averageAttendancePercent = avgAttendance,
      averageFinalScore = avgFinal,
      highestScore = highest,
      lowestScore = lowest,
      passingCount = passingCount,
      passingRatePercent = passingRate,
      studentRows = studentRows,
      anomalies = anomalies,
      priorityFollowUps = priorityList,
    )
  }

  fun buildHomeroomNote(
    studentName: String,
    attendancePercent: Double,
    totalEffectiveDays: Int,
    alpaCount: Int,
    finalScore: Double?,
    predicate: String,
    kkm: Double,
    categorySummaries: List<CategoryScoreSummary>,
    behaviors: List<BehaviorLogEntity>,
  ): String {
    val firstName = studentName.trim().split(Regex("\\s+")).firstOrNull()?.ifBlank { studentName } ?: studentName
    val positiveCount = behaviors.count { it.type == BehaviorType.POSITIF.code }
    val negativeCount = behaviors.count { it.type == BehaviorType.NEGATIF.code }

    val sentence1 = when {
      finalScore == null ->
        "$firstName memiliki catatan kehadiran ${if (totalEffectiveDays > 0) formatPercent(attendancePercent) else "yang sedang dipantau"} dan menunggu kelengkapan bobot nilai."
      finalScore >= 90.0 ->
        "$firstName menunjukkan capaian akademik sangat baik dengan nilai akhir ${formatScore(finalScore)} (Predikat $predicate) dan melampaui KKM."
      finalScore >= kkm ->
        "$firstName mencapai ketuntasan belajar dengan nilai akhir ${formatScore(finalScore)} (Predikat $predicate) dan performa yang stabil."
      else -> {
        val weakCat = categorySummaries.minByOrNull { it.averageScore ?: 0.0 }?.categoryName
        if (weakCat != null) {
          "$firstName memperoleh nilai akhir ${formatScore(finalScore)} (di bawah KKM ${formatScore(kkm)}) sehingga perlu pendampingan khusus pada aspek $weakCat."
        } else {
          "$firstName memperoleh nilai akhir ${formatScore(finalScore)} (di bawah KKM ${formatScore(kkm)}) sehingga perlu mengikuti program remedial."
        }
      }
    }

    val sentence2 = when {
      alpaCount > 3 || (totalEffectiveDays > 0 && attendancePercent < 85.0) ->
        " Tingkat kehadiran (${formatPercent(attendancePercent)}, Alpa $alpaCount kali) perlu ditingkatkan melalui komunikasi aktif dengan orang tua."
      negativeCount > positiveCount && negativeCount > 0 ->
        " Kedisiplinan kehadiran sudah baik (${formatPercent(attendancePercent)}), namun sikap belajar di kelas perlu terus dibina."
      positiveCount > 0 && (totalEffectiveDays == 0 || attendancePercent >= 90.0) ->
        " Kehadiran disiplin serta menunjukkan sikap positif yang patut dipertahankan."
      totalEffectiveDays > 0 && attendancePercent >= 90.0 ->
        " Tingkat kehadiran tercatat sangat baik (${formatPercent(attendancePercent)}) dan konsisten mengikuti kegiatan kelas."
      else ->
        " Pertahankan konsistensi belajar dan keaktifan pada semester berikutnya."
    }

    return sentence1 + sentence2
  }

  fun parseBulkStudents(
    rawText: String,
    existingNisSet: Set<String>,
  ): List<BulkStudentPreviewRow> {
    val lines = rawText
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }

    if (lines.isEmpty()) return emptyList()

    val delimiter = detectDelimiter(lines.first())
    val seenNis = existingNisSet.map { it.trim() }.toMutableSet()
    val results = mutableListOf<BulkStudentPreviewRow>()

    lines.forEachIndexed { idx, line ->
      val cols = splitCsvLine(line, delimiter).map { it.trim() }
      // Skip header line if detected
      if (idx == 0 && cols.any {
          it.equals("nis", ignoreCase = true) ||
            it.equals("nisn", ignoreCase = true) ||
            it.equals("nama", ignoreCase = true) ||
            it.equals("nama lengkap", ignoreCase = true)
        }
      ) {
        return@forEachIndexed
      }

      val nis = cols.getOrNull(0).orEmpty()
      val fullName = cols.getOrNull(1).orEmpty()
      val rawGender = cols.getOrNull(2).orEmpty().uppercase(Locale.getDefault())
      val gender = when {
        rawGender.startsWith("P") || rawGender == "W" || rawGender == "F" -> "P"
        else -> "L"
      }
      val parentContact = cols.getOrNull(3).orEmpty()

      val error = when {
        nis.isBlank() -> "NIS/NISN kosong"
        fullName.isBlank() -> "Nama lengkap kosong"
        !seenNis.add(nis) -> "NIS \"$nis\" duplikat"
        else -> null
      }

      results += BulkStudentPreviewRow(
        lineNumber = idx + 1,
        nis = nis,
        fullName = fullName,
        gender = gender,
        parentContact = parentContact,
        errorMessage = error,
      )
    }
    return results
  }

  fun processRawAcademicText(rawText: String, kkm: Double = 75.0): RawImportParseResult {
    val lines = rawText
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return RawImportParseResult(emptyList(), emptyList(), emptyList(), 0.0, 0.0, 0.0)
    }

    val delimiter = detectDelimiter(lines.first())
    val anomalies = mutableListOf<DataAnomaly>()
    val parsed = mutableListOf<RawParsedStudentRow>()

    var startIndex = 0
    val firstCols = splitCsvLine(lines.first(), delimiter).map { it.trim() }
    val hasHeader = firstCols.any {
      it.equals("nama", ignoreCase = true) ||
        it.equals("nama siswa", ignoreCase = true) ||
        it.equals("tugas", ignoreCase = true)
    }
    if (hasHeader) startIndex = 1

    val headerLower = if (hasHeader) firstCols.map { it.lowercase(Locale.getDefault()) } else emptyList()
    val hasNisCol = headerLower.firstOrNull()?.contains("nis") == true
    val hasTerlambatCol = headerLower.any { it == "t" || it.contains("terlambat") }

    val seenNames = mutableMapOf<String, Int>()

    for (lineIdx in startIndex until lines.size) {
      val cols = splitCsvLine(lines[lineIdx], delimiter).map { it.trim() }
      if (cols.isEmpty() || cols.all { it.isBlank() }) continue

      val rowNum = lineIdx + 1
      var offset = 0
      val nis = if (hasNisCol) {
        offset = 1
        cols.getOrNull(0).orEmpty().ifBlank { "RAW-${100 + lineIdx}" }
      } else {
        "RAW-${100 + lineIdx}"
      }

      val name = cols.getOrNull(offset).orEmpty()
      if (name.isBlank()) {
        anomalies += DataAnomaly(AnomalySeverity.WARNING, "Baris $rowNum Kosong", "Nama siswa kosong, baris dilewati.")
        continue
      }

      val normName = name.lowercase(Locale.getDefault())
      seenNames[normName] = (seenNames[normName] ?: 0) + 1

      fun parseIntSafe(idx: Int, label: String): Int {
        val raw = cols.getOrNull(idx).orEmpty()
        if (raw.isBlank() || raw == "-") return 0
        val parsedInt = raw.toDoubleOrNull()?.toInt()
        if (parsedInt == null || parsedInt < 0) {
          anomalies += DataAnomaly(
            AnomalySeverity.WARNING,
            "Format Absensi ($name)",
            "Kolom $label bernilai \"$raw\" tidak valid, dianggap 0.",
          )
          return 0
        }
        return parsedInt
      }

      fun parseScoreSafe(idx: Int, label: String): Double? {
        val raw = cols.getOrNull(idx).orEmpty()
        if (raw.isBlank() || raw == "-") {
          anomalies += DataAnomaly(
            AnomalySeverity.WARNING,
            "Data Nilai Kosong ($name)",
            "Nilai $label kosong, dihitung sebagai 0.",
          )
          return null
        }
        val num = raw.replace(",", ".").toDoubleOrNull()
        if (num == null) {
          anomalies += DataAnomaly(
            AnomalySeverity.ERROR,
            "Format Nilai Tidak Valid ($name)",
            "Nilai $label \"$raw\" bukan angka, dihitung sebagai 0.",
          )
          return null
        }
        if (num < 0.0 || num > 100.0) {
          anomalies += DataAnomaly(
            AnomalySeverity.ERROR,
            "Nilai Di Luar Rentang 0–100 ($name)",
            "Nilai $label tercatat $num (seharusnya 0–100).",
          )
        }
        return num.coerceIn(0.0, 100.0)
      }

      val h = parseIntSafe(offset + 1, "H")
      val s = parseIntSafe(offset + 2, "S")
      val i = parseIntSafe(offset + 3, "I")
      val a = parseIntSafe(offset + 4, "A")
      val t = if (hasTerlambatCol) {
        offset += 1
        parseIntSafe(offset + 4, "T")
      } else 0

      val tugas = parseScoreSafe(offset + 5, "Tugas")
      val uh = parseScoreSafe(offset + 6, "UH")
      val uts = parseScoreSafe(offset + 7, "UTS")
      val uas = parseScoreSafe(offset + 8, "UAS")

      val totalDays = h + s + i + a + t
      val attPercent = if (totalDays > 0) {
        round2(((h + t).toDouble() / totalDays.toDouble()) * 100.0)
      } else 0.0

      val finalScore = round2(
        (tugas ?: 0.0) * 0.20 +
          (uh ?: 0.0) * 0.30 +
          (uts ?: 0.0) * 0.25 +
          (uas ?: 0.0) * 0.25
      )
      val predicate = predicateFor(finalScore)
      val passing = finalScore >= kkm

      val catSummaries = listOf(
        CategoryScoreSummary(1, "Tugas", "Tugas", 20.0, tugas, if (tugas != null) 1 else 0),
        CategoryScoreSummary(2, "Ulangan Harian", "UH", 30.0, uh, if (uh != null) 1 else 0),
        CategoryScoreSummary(3, "UTS", "UTS", 25.0, uts, if (uts != null) 1 else 0),
        CategoryScoreSummary(4, "UAS", "UAS", 25.0, uas, if (uas != null) 1 else 0),
      )

      val note = buildHomeroomNote(
        studentName = name,
        attendancePercent = attPercent,
        totalEffectiveDays = totalDays,
        alpaCount = a,
        finalScore = finalScore,
        predicate = predicate,
        kkm = kkm,
        categorySummaries = catSummaries,
        behaviors = emptyList(),
      )

      parsed += RawParsedStudentRow(
        name = name,
        nis = nis,
        h = h,
        s = s,
        i = i,
        a = a,
        t = t,
        tugas = tugas,
        uh = uh,
        uts = uts,
        uas = uas,
        attendancePercent = attPercent,
        finalScore = finalScore,
        predicate = predicate,
        isPassing = passing,
        homeroomNote = note,
      )
    }

    seenNames.filter { it.value > 1 }.forEach { (nameKey, count) ->
      anomalies += DataAnomaly(
        severity = AnomalySeverity.WARNING,
        title = "Nama Ganda Pada Data Mentah",
        detail = "Nama \"$nameKey\" muncul $count kali.",
      )
    }

    val sorted = parsed.sortedBy { it.name.lowercase(Locale.getDefault()) }

    val priorities = sorted.mapNotNull { r ->
      val reasons = mutableListOf<String>()
      val acts = mutableListOf<String>()
      if (r.a > 3) {
        reasons += "Alpa ${r.a} kali"
        acts += "Konfirmasi orang tua terkait ketidakhadiran"
      }
      if ((r.h + r.s + r.i + r.a + r.t) > 0 && r.attendancePercent < 85.0) {
        reasons += "Kehadiran ${formatPercent(r.attendancePercent)} (< 85%)"
        acts += "Pembinaan disiplin kehadiran"
      }
      if (r.finalScore < kkm) {
        reasons += "Nilai Akhir ${formatScore(r.finalScore)} (< KKM ${formatScore(kkm)})"
        acts += "Jadwalkan remedial terarah"
      }
      if (reasons.isEmpty()) null
      else PriorityActionItem(
        studentId = 0L,
        studentName = r.name,
        nis = r.nis,
        attendancePercent = r.attendancePercent,
        alpaCount = r.a,
        finalScore = r.finalScore,
        reasons = reasons,
        recommendation = acts.distinct().joinToString("; ") + ".",
      )
    }

    val avgAtt = if (sorted.isEmpty()) 0.0 else round2(sorted.map { it.attendancePercent }.average())
    val avgFinal = if (sorted.isEmpty()) 0.0 else round2(sorted.map { it.finalScore }.average())
    val passRate = if (sorted.isEmpty()) 0.0
    else round2((sorted.count { it.isPassing }.toDouble() / sorted.size.toDouble()) * 100.0)

    return RawImportParseResult(
      rows = sorted,
      anomalies = anomalies,
      priorityFollowUps = priorities,
      averageAttendancePercent = avgAtt,
      averageFinalScore = avgFinal,
      passingRatePercent = passRate,
    )
  }

  private fun detectDelimiter(sampleLine: String): Char {
    val tabCount = sampleLine.count { it == '\t' }
    val semiCount = sampleLine.count { it == ';' }
    val commaCount = sampleLine.count { it == ',' }
    return when {
      tabCount >= semiCount && tabCount >= commaCount && tabCount > 0 -> '\t'
      semiCount >= commaCount && semiCount > 0 -> ';'
      else -> ','
    }
  }

  private fun splitCsvLine(line: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (ch in line) {
      when {
        ch == '"' -> inQuotes = !inQuotes
        ch == delimiter && !inQuotes -> {
          result += current.toString()
          current.clear()
        }
        else -> current.append(ch)
      }
    }
    result += current.toString()
    return result
  }
}
