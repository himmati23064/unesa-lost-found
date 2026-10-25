package com.example.data

import android.util.Log
import com.example.engine.AdaptiveMatchingEngine
import com.example.model.MatchResult
import com.example.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository untuk mengelola data laporan barang hilang dan ditemukan
 * menggunakan Firebase Firestore dengan Coroutines suspend functions dan Flow.
 * Dilengkapi dengan fallback cache cerdas berisikan data awal kampus UNESA.
 */
class ReportRepository(
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }
) {
    private val TAG = "ReportRepository"
    private val REPORTS_COLLECTION = "reports"
    private val MATCHES_COLLECTION = "matches"

    // Seed data kampus UNESA (Ketintang & Lidah Wetan) untuk memastikan aplikasi
    // langsung interaktif dan siap dites evaluasi SUS sebelum / sesudah Firebase di-bind.
    private val _localReports = MutableStateFlow<List<Report>>(getInitialUnesaSeedReports())
    val localReports = _localReports.asStateFlow()

    private val _localMatches = MutableStateFlow<List<MatchResult>>(getInitialUnesaSeedMatches())
    val localMatches = _localMatches.asStateFlow()

    /**
     * Menambah laporan baru ke Firestore (suspend function).
     */
    suspend fun addReport(report: Report): Result<String> {
        return try {
            val reportId = if (report.id.isBlank()) UUID.randomUUID().toString() else report.id
            val finalReport = report.copy(id = reportId)

            // Simpan ke in-memory cache segera
            _localReports.value = listOf(finalReport) + _localReports.value.filter { it.id != reportId }

            // Coba simpan ke Firebase Firestore
            firestore?.let { db ->
                try {
                    db.collection(REPORTS_COLLECTION)
                        .document(reportId)
                        .set(finalReport)
                        .await()
                    Log.d(TAG, "Berhasil menyimpan report ke Firestore: $reportId")
                } catch (fe: Exception) {
                    Log.w(TAG, "Gagal simpan ke Firestore (offline/unconfigured), tersimpan lokal: ${fe.message}")
                }
            }

            // Jalankan pencocokan cerdas multimodal terhadap laporan bertipe kebalikan
            val matches = evaluateMatchesForReport(finalReport)
            for (match in matches) {
                if (match.isHighConfidence) {
                    saveMatch(match)
                }
            }

            Result.success(reportId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saat addReport", e)
            Result.failure(e)
        }
    }

    /**
     * Mengambil daftar laporan dari Firestore menggunakan Coroutines suspend function.
     */
    suspend fun getReports(typeFilter: String? = null): Result<List<Report>> {
        return try {
            val db = firestore
            if (db != null) {
                try {
                    var query: Query = db.collection(REPORTS_COLLECTION)
                        .orderBy("timestamp", Query.Direction.DESCENDING)

                    if (!typeFilter.isNullOrBlank()) {
                        query = query.whereEqualTo("type", typeFilter)
                    }

                    val snapshot = query.get().await()
                    val remoteReports = snapshot.toObjects(Report::class.java)
                    if (remoteReports.isNotEmpty()) {
                        _localReports.value = remoteReports
                        return Result.success(remoteReports)
                    }
                } catch (fe: Exception) {
                    Log.w(TAG, "Firestore query gagal, menggunakan data lokal: ${fe.message}")
                }
            }

            // Fallback ke data lokal
            val currentList = _localReports.value
            val filtered = if (!typeFilter.isNullOrBlank()) {
                currentList.filter { it.type.equals(typeFilter, ignoreCase = true) }
            } else {
                currentList
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aliran data laporan secara real-time via Firestore snapshotListener.
     */
    fun getReportsRealtime(): Flow<List<Report>> = callbackFlow {
        val db = firestore
        if (db == null) {
            // Gunakan local stream jika Firestore belum tersedia
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).run {
                _localReports.collect { trySend(it) }
            }
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection(REPORTS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed", error)
                    trySend(_localReports.value)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.toObjects(Report::class.java)
                    if (reports.isNotEmpty()) {
                        _localReports.value = reports
                        trySend(reports)
                    } else {
                        trySend(_localReports.value)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mengubah status laporan (misal: "Resolved" / "Selesai").
     */
    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> {
        return try {
            _localReports.value = _localReports.value.map {
                if (it.id == reportId) it.copy(status = newStatus) else it
            }

            firestore?.let { db ->
                try {
                    db.collection(REPORTS_COLLECTION)
                        .document(reportId)
                        .update("status", newStatus)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Update status Firestore gagal: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menyimpan hasil Match ke Firestore dan lokal.
     */
    suspend fun saveMatch(match: MatchResult): Result<Unit> {
        return try {
            val exists = _localMatches.value.any { it.id == match.id }
            if (!exists) {
                _localMatches.value = listOf(match) + _localMatches.value
            }

            firestore?.let { db ->
                try {
                    db.collection(MATCHES_COLLECTION)
                        .document(match.id)
                        .set(match)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal simpan match ke Firestore: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluasi kecocokan antara satu laporan baru dengan seluruh laporan tipe lawannya.
     */
    fun evaluateMatchesForReport(targetReport: Report): List<MatchResult> {
        val oppositeReports = _localReports.value.filter {
            it.isLost != targetReport.isLost && it.isOpen
        }

        val foundMatches = mutableListOf<MatchResult>()
        for (other in oppositeReports) {
            val (lost, found) = if (targetReport.isLost) Pair(targetReport, other) else Pair(other, targetReport)
            val match = AdaptiveMatchingEngine.calculateMatch(lost, found)
            if (match != null && match.confidenceScore >= 50.0) {
                foundMatches.add(match)
            }
        }
        return foundMatches.sortedByDescending { it.confidenceScore }
    }

    /**
     * Menjalankan evaluasi ulang menyeluruh untuk demo dan testing algoritma.
     */
    fun runFullAdaptiveMatchingEvaluation(): List<MatchResult> {
        val lostList = _localReports.value.filter { it.isLost && it.isOpen }
        val foundList = _localReports.value.filter { it.isFound && it.isOpen }

        val newMatches = mutableListOf<MatchResult>()
        for (lost in lostList) {
            for (found in foundList) {
                val match = AdaptiveMatchingEngine.calculateMatch(lost, found)
                if (match != null && match.confidenceScore >= 50.0) {
                    newMatches.add(match)
                }
            }
        }
        val sorted = newMatches.sortedByDescending { it.confidenceScore }
        _localMatches.value = sorted
        return sorted
    }

    private fun getInitialUnesaSeedReports(): List<Report> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L

        return listOf(
            Report(
                id = "unesa_rep_001",
                type = Report.TYPE_LOST,
                title = "KTM Mahasiswa Teknik Informatika (NIM 23064)",
                category = Report.CATEGORY_KTM,
                description = "KTM hilang atas nama Himmati dengan tali lanyard biru FT UNESA, ada stiker barcode perpus di belakang kartu.",
                imageUrl = "https://images.unsplash.com/photo-1578852612716-854e51480047?w=600",
                latitude = -7.3155,
                longitude = 112.7275,
                locationName = "Gedung E1 Fakultas Teknik",
                campusZone = Report.CAMPUS_KETINTANG,
                timestamp = now - (2 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_23064",
                userName = "Himmati",
                userContact = "081234567890",
                userNim = "23064"
            ),
            Report(
                id = "unesa_rep_002",
                type = Report.TYPE_FOUND,
                title = "Kartu Tanda Mahasiswa (KTM) FT UNESA",
                category = Report.CATEGORY_KTM,
                description = "Ditemukan KTM UNESA Fakultas Teknik warna biru di dekat meja laboratorium komputer E1 lantai 2.",
                imageUrl = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=600",
                latitude = -7.3154,
                longitude = 112.7276,
                locationName = "Lab Komputer Gedung E1 FT",
                campusZone = Report.CAMPUS_KETINTANG,
                timestamp = now - (1 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_security_01",
                userName = "Pos Satpam FT UNESA",
                userContact = "085799988811",
                userNim = "Staff Satpam"
            ),
            Report(
                id = "unesa_rep_003",
                type = Report.TYPE_LOST,
                title = "Kunci Motor Honda Vario dengan Gantungan Boneka",
                category = Report.CATEGORY_KEYS,
                description = "Kunci motor kontak warna hitam dengan gantungan boneka beruang rajut warna krem, remote alarm ada sedikit lecet.",
                imageUrl = "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600",
                latitude = -7.3015,
                longitude = 112.6738,
                locationName = "Parkiran Perpustakaan Pusat",
                campusZone = Report.CAMPUS_LIDAH_WETAN,
                timestamp = now - (5 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_mhs_02",
                userName = "Dina Rahma",
                userContact = "081987654321",
                userNim = "22051"
            ),
            Report(
                id = "unesa_rep_004",
                type = Report.TYPE_FOUND,
                title = "Kunci Motor Honda dengan Gantungan Rajut",
                category = Report.CATEGORY_KEYS,
                description = "Ditemukan kunci sepeda motor Honda di trotoar depan Perpustakaan Kampus Lidah Wetan. Gantungan boneka rajut.",
                imageUrl = "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600",
                latitude = -7.3017,
                longitude = 112.6740,
                locationName = "Halaman Perpustakaan Pusat",
                campusZone = Report.CAMPUS_LIDAH_WETAN,
                timestamp = now - (3 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_perpus_01",
                userName = "Petugas Loket Perpus",
                userContact = "082133445566",
                userNim = "Staf Perpus"
            ),
            Report(
                id = "unesa_rep_005",
                type = Report.TYPE_LOST,
                title = "Earphone TWS Samsung Galaxy Buds Putih",
                category = Report.CATEGORY_ELECTRONICS,
                description = "Tertinggal case dan earbuds putih di bangku dekat gazebo Danau UNESA setelah kegiatan praktikum sore.",
                imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600",
                latitude = -7.3002,
                longitude = 112.6725,
                locationName = "Gazebo Danau Unesa",
                campusZone = Report.CAMPUS_LIDAH_WETAN,
                timestamp = now - (14 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_mhs_03",
                userName = "Bagas Arya",
                userContact = "083812345678",
                userNim = "21045"
            ),
            Report(
                id = "unesa_rep_006",
                type = Report.TYPE_FOUND,
                title = "Flashdisk SanDisk 64GB Merah Hitam",
                category = Report.CATEGORY_ELECTRONICS,
                description = "Tertinggal di PC Dosen Gedung FEB Ketintang ruang sidang. Ada stiker inisial A.R.",
                imageUrl = "https://images.unsplash.com/photo-1618761714954-0b8cd0026356?w=600",
                latitude = -7.3168,
                longitude = 112.7290,
                locationName = "Gedung G FEB Ketintang",
                campusZone = Report.CAMPUS_KETINTANG,
                timestamp = now - (20 * hour),
                status = Report.STATUS_OPEN,
                userId = "user_feb_01",
                userName = "Admin FEB",
                userContact = "081255443322",
                userNim = "Tendik FEB"
            )
        )
    }

    private fun getInitialUnesaSeedMatches(): List<MatchResult> {
        val reports = getInitialUnesaSeedReports()
        val lostKtm = reports[0]
        val foundKtm = reports[1]
        val lostKey = reports[2]
        val foundKey = reports[3]

        val match1 = AdaptiveMatchingEngine.calculateMatch(lostKtm, foundKtm)?.copy(
            confidenceScore = 92.5,
            status = MatchResult.STATUS_PENDING
        )

        val match2 = AdaptiveMatchingEngine.calculateMatch(lostKey, foundKey)?.copy(
            confidenceScore = 87.0,
            status = MatchResult.STATUS_PENDING
        )

        return listOfNotNull(match1, match2)
    }
}
