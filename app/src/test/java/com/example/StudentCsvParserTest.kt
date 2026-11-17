package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.ClassRepository
import com.example.domain.StudentCsvParser
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudentCsvParserTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: ClassRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ClassRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `parses UTF-8 BOM, RFC-4180 quotes, and reordered headers into StudentEntity`() {
    val csv = "\uFEFF" + """
      No;Nama Lengkap;NISN;Jenis Kelamin;No HP
      1;"Santoso, Budi";2026001;Laki-laki;0812-3456-7890
      2;"Rahma ""Siti"" Putri";2026002;Perempuan;+62 813 9988 7766
    """.trimIndent()

    val result = StudentCsvParser.parse(csv)
    assertTrue(result.usedHeaderMapping)
    assertEquals("Titik Koma (;)", result.delimiterLabel)
    assertEquals(2, result.validEntities.size)
    assertEquals(0, result.invalidCount)

    val first = result.validEntities[0]
    assertEquals("2026001", first.nis)
    assertEquals("Santoso, Budi", first.fullName)
    assertEquals("L", first.gender)
    assertEquals("0812-3456-7890", first.parentContact)

    val second = result.validEntities[1]
    assertEquals("2026002", second.nis)
    assertEquals("Rahma \"Siti\" Putri", second.fullName)
    assertEquals("P", second.gender)
  }

  @Test
  fun `validates duplicate NIS, invalid gender, invalid contact, and missing fields`() {
    val csv = """
      NIS,Nama Lengkap,L/P,Kontak Orang Tua
      1001,Ahmad Fauzi,L,08123456789
      1001,Duplikat Di File,L,08123456780
      1002,Sudah Ada Di DB,P,08123456781
      ,Tanpa NIS,L,08123456782
      1003,Gender Salah,X,08123456783
      1004,Kontak Salah,P,abc
    """.trimIndent()

    val result = StudentCsvParser.parse(csv, existingNisSet = setOf("1002"))
    assertEquals(6, result.totalDataRows)
    assertEquals(1, result.validEntities.size)
    assertEquals(5, result.invalidCount)

    assertTrue(result.previewRows[0].isValid)
    assertNotNull(result.previewRows[0].entity)

    assertFalse(result.previewRows[1].isValid)
    assertNull(result.previewRows[1].entity)
    assertTrue(result.previewRows[1].errorMessage!!.contains("duplikat", ignoreCase = true))

    assertTrue(result.previewRows[2].errorMessage!!.contains("sudah terdaftar", ignoreCase = true))
    assertTrue(result.previewRows[3].errorMessage!!.contains("wajib diisi", ignoreCase = true))
    assertTrue(result.previewRows[4].errorMessage!!.contains("Jenis kelamin", ignoreCase = true))
    assertTrue(result.previewRows[5].errorMessage!!.contains("Kontak orang tua", ignoreCase = true))
  }

  @Test
  fun `imports validated CSV student entities directly into Room database`() = runTest {
    val csv = """
      NIS	Nama Lengkap	L/P	Kontak
      2026101	Dewi Lestari	P	081211122233
      2026102	Rizky Pratama	L	081344455566
      	Invalid Row	L	0812
    """.trimIndent()

    val outcome = repository.importStudentsFromCsv(csv).getOrThrow()
    val (parseResult, insertedCount) = outcome

    assertEquals(2, parseResult.validEntities.size)
    assertEquals(1, parseResult.invalidCount)
    assertEquals(2, insertedCount)

    val stored = db.studentDao().getAllOnce()
    assertEquals(2, stored.size)
    assertEquals("Dewi Lestari", stored[0].fullName)
    assertEquals("2026101", stored[0].nis)
    assertEquals("P", stored[0].gender)
    assertEquals("Rizky Pratama", stored[1].fullName)
    assertEquals("2026102", stored[1].nis)
  }
}
