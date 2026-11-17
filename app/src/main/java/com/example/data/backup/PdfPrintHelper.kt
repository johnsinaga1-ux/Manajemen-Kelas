package com.example.data.backup

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.AcademicCalculator
import com.example.domain.BehaviorType
import com.example.domain.ClassReportSummary
import com.example.domain.StudentReportRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfPrintHelper {

  fun printClassReport(
    context: Context,
    report: ClassReportSummary,
    periodLabel: String,
  ) {
    val html = buildClassReportHtml(report, periodLabel)
    printHtml(context, "Laporan_Kelas_${report.settings.className}", html)
  }

  fun printMiniRapor(
    context: Context,
    report: ClassReportSummary,
    studentRow: StudentReportRow,
    periodLabel: String,
  ) {
    val html = buildMiniRaporHtml(report, studentRow, periodLabel)
    printHtml(context, "Rapor_Mini_${studentRow.fullName}", html)
  }

  private fun printHtml(context: Context, jobName: String, html: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

      override fun onPageFinished(view: WebView?, url: String?) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = webView.createPrintDocumentAdapter(jobName.replace(" ", "_"))
        printManager.print(
          jobName,
          printAdapter,
          PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build(),
        )
      }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  private fun buildClassReportHtml(report: ClassReportSummary, periodLabel: String): String {
    val s = report.settings
    val dateToday = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    val catHeaders = report.categories.joinToString("") {
      "<th>${esc(it.shortCode)}<br/><small>(${AcademicCalculator.formatScore(it.weightPercent)}%)</small></th>"
    }

    val rowsHtml = report.studentRows.mapIndexed { idx, row ->
      val catCells = report.categories.joinToString("") { cat ->
        val cs = row.categorySummaries.firstOrNull { it.categoryId == cat.id }
        "<td class=\"num\">${esc(AcademicCalculator.formatScore(cs?.averageScore))}</td>"
      }
      val statusText = when (row.isPassing) {
        true -> "<span class=\"badge pass\">Tuntas</span>"
        false -> "<span class=\"badge fail\">Remedial</span>"
        null -> "-"
      }
      """
      <tr>
        <td class="num">${idx + 1}</td>
        <td>${esc(row.nis)}</td>
        <td>${esc(row.fullName)}</td>
        <td class="num">${row.hadirCount}</td>
        <td class="num">${row.sakitCount}</td>
        <td class="num">${row.izinCount}</td>
        <td class="num">${row.alpaCount}</td>
        <td class="num">${row.terlambatCount}</td>
        <td class="num">${esc(AcademicCalculator.formatPercent(row.attendancePercent))}</td>
        $catCells
        <td class="num"><strong>${esc(AcademicCalculator.formatScore(row.finalScore))}</strong></td>
        <td class="center">${esc(row.predicate)}</td>
        <td class="center">$statusText</td>
      </tr>
      """.trimIndent()
    }.joinToString("\n")

    val priorityHtml = if (report.priorityFollowUps.isEmpty()) {
      "<p class=\"muted\">Tidak ada siswa yang memerlukan tindak lanjut khusus pada periode ini.</p>"
    } else {
      "<ul>" + report.priorityFollowUps.joinToString("") { item ->
        "<li><strong>${esc(item.studentName)}</strong> (${esc(item.nis)}): ${esc(item.reasons.joinToString(", "))}. <em>Tindakan: ${esc(item.recommendation)}</em></li>"
      } + "</ul>"
    }

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8" />
        <style>
          @page { size: A4 landscape; margin: 14mm; }
          body { font-family: sans-serif; color: #1A1F29; font-size: 11px; line-height: 1.45; }
          .header { border-bottom: 2px solid #1B3556; padding-bottom: 8px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: flex-end; }
          .header h1 { margin: 0; font-size: 16px; color: #1B3556; }
          .header p { margin: 2px 0 0; color: #5A6373; }
          .stats { display: flex; gap: 10px; margin-bottom: 12px; }
          .stat-box { flex: 1; border: 1px solid #D6D3C9; border-radius: 6px; padding: 8px 10px; background: #FAF9F5; }
          .stat-label { font-size: 9px; text-transform: uppercase; color: #5A6373; }
          .stat-val { font-size: 14px; font-weight: bold; color: #1B3556; margin-top: 2px; }
          table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
          th, td { border: 1px solid #CBD5E1; padding: 5px 6px; }
          th { background: #1B3556; color: #FFFFFF; font-size: 10px; text-align: center; }
          td.num { text-align: right; font-variant-numeric: tabular-nums; }
          td.center { text-align: center; }
          tr:nth-child(even) { background: #F8FAFC; }
          .badge { padding: 2px 6px; border-radius: 4px; font-size: 9px; font-weight: bold; }
          .pass { background: #D4F2E7; color: #094030; }
          .fail { background: #FEE2E2; color: #5C0B0B; }
          .section-title { font-size: 12px; font-weight: bold; color: #1B3556; margin: 10px 0 6px; }
          .muted { color: #5A6373; }
          .signatures { display: flex; justify-content: space-between; margin-top: 28px; page-break-inside: avoid; }
          .sig-col { width: 38%; text-align: center; }
          .sig-space { height: 56px; }
          .sig-line { border-top: 1px solid #1A1F29; padding-top: 4px; font-weight: bold; }
        </style>
      </head>
      <body>
        <div class="header">
          <div>
            <h1>LAPORAN REKAPITULASI AKADEMIK & KEHADIRAN</h1>
            <p>${esc(s.schoolName)} • Kelas ${esc(s.className)} • Tahun Ajaran ${esc(s.academicYear)} • Filter: ${esc(periodLabel)}</p>
          </div>
          <div>KKM: <strong>${esc(AcademicCalculator.formatScore(s.kkm))}</strong></div>
        </div>

        <div class="stats">
          <div class="stat-box"><div class="stat-label">Total Siswa</div><div class="stat-val">${report.totalStudents}</div></div>
          <div class="stat-box"><div class="stat-label">Rata-rata Kehadiran</div><div class="stat-val">${esc(AcademicCalculator.formatPercent(report.averageAttendancePercent))}</div></div>
          <div class="stat-box"><div class="stat-label">Rata-rata Nilai Kelas</div><div class="stat-val">${esc(AcademicCalculator.formatScore(report.averageFinalScore))}</div></div>
          <div class="stat-box"><div class="stat-label">Ketuntasan KKM</div><div class="stat-val">${esc(AcademicCalculator.formatPercent(report.passingRatePercent))}</div></div>
          <div class="stat-box"><div class="stat-label">Tertinggi / Terendah</div><div class="stat-val">${esc(AcademicCalculator.formatScore(report.highestScore))} / ${esc(AcademicCalculator.formatScore(report.lowestScore))}</div></div>
        </div>

        <table>
          <thead>
            <tr>
              <th>No</th><th>NIS</th><th>Nama Lengkap</th>
              <th>H</th><th>S</th><th>I</th><th>A</th><th>T</th><th>% Hadir</th>
              $catHeaders
              <th>Nilai Akhir</th><th>Pred</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            $rowsHtml
          </tbody>
        </table>

        <div class="section-title">Prioritas Tindak Lanjut</div>
        $priorityHtml

        <div class="signatures">
          <div class="sig-col">
            <div>Mengetahui,<br/>Kepala Sekolah</div>
            <div class="sig-space"></div>
            <div class="sig-line">${esc(s.principalName.ifBlank { "(........................................)" })}</div>
            <div>NIP. ${esc(s.principalNip.ifBlank { "............................" })}</div>
          </div>
          <div class="sig-col">
            <div>${esc(dateToday)}<br/>Wali Kelas ${esc(s.className)}</div>
            <div class="sig-space"></div>
            <div class="sig-line">${esc(s.homeroomTeacherName.ifBlank { "(........................................)" })}</div>
            <div>NIP. ${esc(s.homeroomTeacherNip.ifBlank { "............................" })}</div>
          </div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }

  private fun buildMiniRaporHtml(
    report: ClassReportSummary,
    row: StudentReportRow,
    periodLabel: String,
  ): String {
    val s = report.settings
    val dateToday = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    val gradeRows = row.categorySummaries.joinToString("") { cat ->
      """
      <tr>
        <td>${esc(cat.categoryName)}</td>
        <td class="num">${esc(AcademicCalculator.formatScore(cat.weightPercent))}%</td>
        <td class="num">${cat.entryCount}</td>
        <td class="num"><strong>${esc(AcademicCalculator.formatScore(cat.averageScore))}</strong></td>
      </tr>
      """.trimIndent()
    }
    val behaviorHtml = if (row.behaviorLogs.isEmpty()) {
      "<p class=\"muted\">Belum ada catatan sikap khusus pada periode ini.</p>"
    } else {
      "<ul>" + row.behaviorLogs.joinToString("") { b ->
        val label = if (b.type == BehaviorType.POSITIF.code) "Positif" else "Perlu Perhatian"
        "<li><strong>[${esc(b.dateIso)}] ($label)</strong>: ${esc(b.description)}" +
          (if (b.followUp.isNotBlank()) " — <em>Tindak lanjut: ${esc(b.followUp)}</em>" else "") +
          "</li>"
      } + "</ul>"
    }
    val statusText = when (row.isPassing) {
      true -> "TUNTAS (Memenuhi KKM ${AcademicCalculator.formatScore(s.kkm)})"
      false -> "REMEDIAL (Di bawah KKM ${AcademicCalculator.formatScore(s.kkm)})"
      null -> "Bobot kategori belum 100%"
    }

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8" />
        <style>
          @page { size: A4 portrait; margin: 16mm; }
          body { font-family: sans-serif; color: #1A1F29; font-size: 12px; line-height: 1.5; }
          .banner { border-bottom: 2px solid #1B3556; padding-bottom: 10px; margin-bottom: 14px; }
          .banner h1 { margin: 0; font-size: 17px; color: #1B3556; }
          .banner p { margin: 2px 0 0; color: #5A6373; }
          .meta-grid { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
          .meta-grid td { padding: 4px 6px; border: none; }
          table.data { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
          table.data th, table.data td { border: 1px solid #CBD5E1; padding: 6px 8px; }
          table.data th { background: #1B3556; color: #FFFFFF; text-align: left; }
          .num { text-align: right; font-variant-numeric: tabular-nums; }
          .note-box { border: 1px solid #D6D3C9; border-radius: 6px; padding: 10px 12px; background: #FAF9F5; margin-bottom: 14px; }
          .muted { color: #5A6373; }
          .signatures { display: flex; justify-content: space-between; margin-top: 32px; page-break-inside: avoid; }
          .sig-col { width: 42%; text-align: center; }
          .sig-space { height: 60px; }
          .sig-line { border-top: 1px solid #1A1F29; padding-top: 4px; font-weight: bold; }
        </style>
      </head>
      <body>
        <div class="banner">
          <h1>RAPOR MINI SISWA</h1>
          <p>${esc(s.schoolName)} • Kelas ${esc(s.className)} • TA ${esc(s.academicYear)} • Periode: ${esc(periodLabel)}</p>
        </div>

        <table class="meta-grid">
          <tr><td><strong>Nama Lengkap</strong></td><td>: ${esc(row.fullName)}</td><td><strong>NIS / NISN</strong></td><td>: ${esc(row.nis)}</td></tr>
          <tr><td><strong>Jenis Kelamin</strong></td><td>: ${if (row.gender == "P") "Perempuan" else "Laki-laki"}</td><td><strong>Kontak Orang Tua</strong></td><td>: ${esc(row.parentContact.ifBlank { "-" })}</td></tr>
        </table>

        <h3>A. Rekapitulasi Kehadiran</h3>
        <table class="data">
          <thead>
            <tr><th>Hadir (H)</th><th>Terlambat (T)</th><th>Sakit (S)</th><th>Izin (I)</th><th>Alpa (A)</th><th>% Kehadiran</th></tr>
          </thead>
          <tbody>
            <tr>
              <td class="num">${row.hadirCount}</td>
              <td class="num">${row.terlambatCount}</td>
              <td class="num">${row.sakitCount}</td>
              <td class="num">${row.izinCount}</td>
              <td class="num">${row.alpaCount}</td>
              <td class="num"><strong>${esc(AcademicCalculator.formatPercent(row.attendancePercent))}</strong></td>
            </tr>
          </tbody>
        </table>

        <h3>B. Rincian Nilai Akademik</h3>
        <table class="data">
          <thead>
            <tr><th>Kategori Penilaian</th><th class="num">Bobot</th><th class="num">Jumlah Nilai</th><th class="num">Rata-rata</th></tr>
          </thead>
          <tbody>
            $gradeRows
            <tr>
              <td colspan="3"><strong>Nilai Akhir (Predikat ${esc(row.predicate)}) — ${esc(statusText)}</strong></td>
              <td class="num"><strong>${esc(AcademicCalculator.formatScore(row.finalScore))}</strong></td>
            </tr>
          </tbody>
        </table>

        <h3>C. Catatan Sikap & Kejadian</h3>
        $behaviorHtml

        <h3>D. Catatan Wali Kelas</h3>
        <div class="note-box">${esc(row.effectiveHomeroomNote)}</div>

        <div class="signatures">
          <div class="sig-col">
            <div>Mengetahui,<br/>Kepala Sekolah</div>
            <div class="sig-space"></div>
            <div class="sig-line">${esc(s.principalName.ifBlank { "(........................................)" })}</div>
            <div>NIP. ${esc(s.principalNip.ifBlank { "............................" })}</div>
          </div>
          <div class="sig-col">
            <div>${esc(dateToday)}<br/>Wali Kelas ${esc(s.className)}</div>
            <div class="sig-space"></div>
            <div class="sig-line">${esc(s.homeroomTeacherName.ifBlank { "(........................................)" })}</div>
            <div>NIP. ${esc(s.homeroomTeacherNip.ifBlank { "............................" })}</div>
          </div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }

  private fun esc(text: String): String =
    text.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
}
