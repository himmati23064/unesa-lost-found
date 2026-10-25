package com.example.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.data.ReportRepository
import com.example.model.MatchResult
import com.example.model.Report
import com.example.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface ReportUiState {
    object Loading : ReportUiState

    data class Success(
        val allReports: List<Report> = emptyList(),
        val filteredLostReports: List<Report> = emptyList(),
        val filteredFoundReports: List<Report> = emptyList(),
        val myReports: List<Report> = emptyList(),
        val matches: List<MatchResult> = emptyList(),
        val selectedCategory: String? = null,
        val selectedCampus: String? = null,
        val searchQuery: String = "",
        val isSubmitting: Boolean = false,
        val user: User? = null
    ) : ReportUiState

    data class Error(val message: String) : ReportUiState
}

class ReportViewModel(
    private val repository: ReportRepository = ReportRepository()
) : ViewModel() {

    // Status Autentikasi Pengguna UNESA
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(
        User(
            uid = "user_23064",
            name = "Himmati",
            email = "himmati.23064@mhs.unesa.ac.id",
            nim = "23064",
            phone = "081234567890",
            faculty = "Fakultas Teknik (FT)",
            studyProgram = "S1 Teknik Informatika",
            primaryCampus = "Kampus Ketintang",
            role = "Mahasiswa Aktif",
            reputationScore = 98,
            totalReports = 2,
            totalResolved = 1
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private var rawReports: List<Report> = emptyList()
    private var rawMatches: List<MatchResult> = emptyList()
    private var currentCategory: String? = null
    private var currentCampus: String? = null
    private var currentSearch: String = ""

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.getReportsRealtime()
                .catch { e ->
                    _uiState.value = ReportUiState.Error("Gagal mengambil data laporan: ${e.message}")
                }
                .collectLatest { reports ->
                    rawReports = reports
                    applyFilterAndEmit()
                }
        }

        viewModelScope.launch {
            repository.localMatches.collectLatest { matches ->
                rawMatches = matches
                applyFilterAndEmit()
            }
        }
    }

    fun login(
        emailOrNim: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (emailOrNim.isBlank() || password.isBlank()) {
                onError("Email/NIM dan kata sandi wajib diisi!")
                return@launch
            }

            // Simulasi autentikasi SSO UNESA
            val defaultUser = _currentUser.value ?: User()
            val updatedUser = if (emailOrNim.contains("@")) {
                defaultUser.copy(email = emailOrNim)
            } else {
                defaultUser.copy(nim = emailOrNim, email = "$emailOrNim@mhs.unesa.ac.id")
            }

            _currentUser.value = updatedUser
            _isLoggedIn.value = true
            _eventFlow.emit("Selamat datang di UNESA Lost and Found, ${updatedUser.name}!")
            applyFilterAndEmit()
            onSuccess()
        }
    }

    fun loginWithUnesaSso(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Quick One-Tap SSO UNESA
            _isLoggedIn.value = true
            _eventFlow.emit("Berhasil masuk melalui SSO Universitas Negeri Surabaya")
            applyFilterAndEmit()
            onSuccess()
        }
    }

    fun register(
        user: User,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (user.name.isBlank() || user.nim.isBlank() || user.phone.isBlank()) {
                onError("Mohon lengkapi Nama, NIM, dan No. HP!")
                return@launch
            }

            _currentUser.value = user
            _isLoggedIn.value = true
            _eventFlow.emit("Pendaftaran berhasil! Selamat datang, ${user.name}")
            applyFilterAndEmit()
            onSuccess()
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        viewModelScope.launch {
            _eventFlow.emit("Anda telah keluar dari akun UNESA.")
        }
    }

    fun updateUserProfile(updatedUser: User, onComplete: () -> Unit) {
        _currentUser.value = updatedUser
        viewModelScope.launch {
            _eventFlow.emit("Profil civitas berhasil diperbarui!")
            applyFilterAndEmit()
            onComplete()
        }
    }

    fun refreshReports() {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            repository.getReports()
                .onSuccess { reports ->
                    rawReports = reports
                    applyFilterAndEmit()
                }
                .onFailure { error ->
                    _uiState.value = ReportUiState.Error(error.message ?: "Terjadi kesalahan")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearch = query
        applyFilterAndEmit()
    }

    fun onCategorySelected(category: String?) {
        currentCategory = if (currentCategory == category) null else category
        applyFilterAndEmit()
    }

    fun onCampusSelected(campus: String?) {
        currentCampus = if (currentCampus == campus) null else campus
        applyFilterAndEmit()
    }

    private fun applyFilterAndEmit() {
        val user = _currentUser.value
        val userNim = user?.nim ?: ""
        val userId = user?.uid ?: ""

        val filtered = rawReports.filter { report ->
            val matchesCategory = currentCategory == null || report.category.equals(currentCategory, ignoreCase = true)
            val matchesCampus = currentCampus == null || report.campusZone.equals(currentCampus, ignoreCase = true)
            val matchesSearch = currentSearch.isBlank() ||
                    report.title.contains(currentSearch, ignoreCase = true) ||
                    report.description.contains(currentSearch, ignoreCase = true) ||
                    report.locationName.contains(currentSearch, ignoreCase = true)

            matchesCategory && matchesCampus && matchesSearch
        }

        val lost = filtered.filter { it.isLost }
        val found = filtered.filter { it.isFound }
        val myReports = rawReports.filter {
            (userNim.isNotBlank() && it.userNim == userNim) ||
            (userId.isNotBlank() && it.userId == userId) ||
            (it.userName.equals(user?.name, ignoreCase = true))
        }

        _uiState.value = ReportUiState.Success(
            allReports = rawReports,
            filteredLostReports = lost,
            filteredFoundReports = found,
            myReports = myReports,
            matches = rawMatches,
            selectedCategory = currentCategory,
            selectedCampus = currentCampus,
            searchQuery = currentSearch,
            user = user
        )
    }

    fun submitReport(report: Report, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ReportUiState.Success) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }

            // Lampirkan data user login jika ada
            val user = _currentUser.value
            val enrichedReport = if (user != null) {
                report.copy(
                    userId = user.uid,
                    userName = if (report.userName.isBlank()) user.name else report.userName,
                    userNim = if (report.userNim.isBlank()) user.nim else report.userNim,
                    userContact = if (report.userContact.isBlank()) user.phone else report.userContact
                )
            } else report

            repository.addReport(enrichedReport)
                .onSuccess { id ->
                    _eventFlow.emit("Laporan berhasil dibuat! ID: ${id.take(8)}")
                    val newMatches = repository.evaluateMatchesForReport(enrichedReport)
                    if (newMatches.any { it.isHighConfidence }) {
                        _eventFlow.emit("Peluang kecocokan tinggi terdeteksi oleh sistem!")
                    }
                    onComplete()
                }
                .onFailure { e ->
                    _eventFlow.emit("Gagal membuat laporan: ${e.message}")
                }

            if (_uiState.value is ReportUiState.Success) {
                _uiState.value = (_uiState.value as ReportUiState.Success).copy(isSubmitting = false)
            }
        }
    }

    fun markAsResolved(reportId: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, Report.STATUS_RESOLVED)
                .onSuccess {
                    _eventFlow.emit("Status laporan diperbarui menjadi Selesai.")
                }
        }
    }

    fun triggerAdaptiveMatchingSimulation(context: Context? = null) {
        viewModelScope.launch {
            val matches = repository.runFullAdaptiveMatchingEvaluation()
            _eventFlow.emit("Pencocokan Adaptive selesai: Ditemukan ${matches.size} kandidat kecocokan!")

            val bestMatch = matches.firstOrNull { it.isHighConfidence }
            if (bestMatch != null && context != null) {
                showLocalNotification(context, bestMatch)
            }
        }
    }

    fun showLocalNotification(context: Context, match: MatchResult) {
        val channelId = "unesa_lost_found_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "UNESA Lost & Found Matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi cerdas kecocokan barang hilang dan ditemukan"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Peluang Kecocokan Ditemukan!")
            .setContentText("Kecocokan ${match.percentageInt}% antara '${match.lostTitle}' dan '${match.foundTitle}'")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Sistem Adaptive Match mendeteksi skor ${match.percentageInt}%!\n" +
                            "Barang: ${match.lostTitle}\n" +
                            "Lokasi: ${match.foundLocation}\n" +
                            "Perincian: ${match.matchSummary}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
