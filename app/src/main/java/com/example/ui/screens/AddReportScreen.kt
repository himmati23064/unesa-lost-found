package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.model.Report
import com.example.ui.theme.FoundGreen
import com.example.ui.theme.LostRed
import com.example.ui.theme.UnesaBluePrimary
import com.example.viewmodel.ReportViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportScreen(
    viewModel: ReportViewModel,
    initialType: String = Report.TYPE_LOST,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var reportType by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Report.CATEGORY_KTM) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCampus by remember { mutableStateOf(Report.CAMPUS_KETINTANG) }
    var locationName by remember { mutableStateOf("Gedung E1 Fakultas Teknik") }
    var isLocationDropdownExpanded by remember { mutableStateOf(false) }

    var latitude by remember { mutableDoubleStateOf(-7.3155) }
    var longitude by remember { mutableDoubleStateOf(112.7275) }
    var isGpsDetected by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var reporterName by remember { mutableStateOf("") }
    var reporterNim by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    // Launcher untuk Pemilihan Foto Galeri / Kamera
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Launcher Izin Lokasi GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchDeviceLocation(context) { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                isGpsDetected = true
                locationName = "Koordinat GPS: %.4f, %.4f".format(loc.latitude, loc.longitude)
                Toast.makeText(context, "Lokasi GPS berhasil diambil!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk GPS otomatis", Toast.LENGTH_SHORT).show()
        }
    }

    // Spot UNESA Populer
    val popularSpots = when (selectedCampus) {
        Report.CAMPUS_KETINTANG -> listOf(
            "Gedung E1 Fakultas Teknik",
            "Gedung G FEB Ketintang",
            "Gedung C FMIPA",
            "Gedung Rektorat Ketintang",
            "Masjid Baitul Makmur Ketintang",
            "Kantin Teknik & Parkiran Motor",
            "Laboratorium Komputer FT"
        )
        else -> listOf(
            "Perpustakaan Pusat Unesa Lidah",
            "Danau Unesa Lidah Wetan",
            "Gedung Rektorat Unesa Lidah",
            "Graha Unesa",
            "Gedung FIP / Kampus Lidah",
            "Gedung FBS / Gedung T",
            "Gedung FIKK / Gelora Olahraga",
            "Kantin Pusat & Danau"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (reportType == Report.TYPE_LOST) "Lapor Barang Hilang" else "Lapor Temuan Barang",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UnesaBluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pemilihan Jenis Laporan: Hilang vs Ditemukan
            Text(
                text = "Jenis Laporan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tombol Barang Hilang
                val isLostSelected = reportType == Report.TYPE_LOST
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLostSelected) LostRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isLostSelected) androidx.compose.foundation.BorderStroke(2.dp, LostRed) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { reportType = Report.TYPE_LOST }
                        .testTag("tab_lost_type")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Barang Hilang",
                            fontWeight = if (isLostSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLostSelected) LostRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tombol Barang Ditemukan
                val isFoundSelected = reportType == Report.TYPE_FOUND
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFoundSelected) FoundGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isFoundSelected) androidx.compose.foundation.BorderStroke(2.dp, FoundGreen) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { reportType = Report.TYPE_FOUND }
                        .testTag("tab_found_type")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Barang Ditemukan",
                            fontWeight = if (isFoundSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFoundSelected) FoundGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Input Nama Barang (OutlinedTextField)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Barang / Judul Laporan *") },
                placeholder = { Text("Contoh: KTM Informatika, Kunci Honda Vario...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Dropdown Menu Kategori
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori Barang *") },
                    trailingIcon = {
                        IconButton(onClick = { isCategoryDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Kategori")
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Category, contentDescription = null, tint = UnesaBluePrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCategoryDropdownExpanded = true }
                        .testTag("dropdown_category"),
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    Report.ALL_CATEGORIES.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Deskripsi Barang (OutlinedTextField)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Lengkap *") },
                placeholder = { Text("Ciri khas, warna, merk, stiker, nomor identitas, atau detail spesifik...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("input_description"),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            // Foto Barang (Kamera/Galeri Launcher)
            Text(
                text = "Foto Barang (Opsional - Bobot Visual 40%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("btn_pick_photo"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Preview Foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus Foto", tint = Color.White)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Unggah Foto",
                            tint = UnesaBluePrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ketuk untuk Memilih Foto dari Galeri / Kamera",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UnesaBluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Lokasi Kampus UNESA & GPS
            Text(
                text = "Lokasi di Lingkungan Kampus UNESA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Report.ALL_CAMPUSES.forEach { campus ->
                    val isSelected = selectedCampus == campus
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) UnesaBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedCampus = campus
                                locationName = popularSpots.first()
                            }
                    ) {
                        Text(
                            text = campus,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Pilih Gedung / Spot
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Gedung / Lokasi Spesifik *") },
                    trailingIcon = {
                        IconButton(onClick = { isLocationDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih Spot")
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = UnesaBluePrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_location_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = isLocationDropdownExpanded,
                    onDismissRequest = { isLocationDropdownExpanded = false }
                ) {
                    popularSpots.forEach { spot ->
                        DropdownMenuItem(
                            text = { Text(spot) },
                            onClick = {
                                locationName = spot
                                isLocationDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Tombol Deteksi Lokasi GPS
            OutlinedButton(
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            fetchDeviceLocation(context) { loc ->
                                latitude = loc.latitude
                                longitude = loc.longitude
                                isGpsDetected = true
                                locationName = "$locationName (GPS: %.4f, %.4f)".format(loc.latitude, loc.longitude)
                                Toast.makeText(context, "Koordinat GPS tersimpan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_get_gps"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGpsDetected) "GPS Terhubung (%.4f, %.4f)".format(latitude, longitude) else "Ambil Lokasi GPS Perangkat",
                    fontSize = 13.sp
                )
            }

            // Data Kontak Civitas
            Text(
                text = "Identitas Pelapor & Kontak",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reporterName,
                    onValueChange = { reporterName = it },
                    label = { Text("Nama Lengkap") },
                    placeholder = { Text("Nama Mahasiswa/Staff") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reporterNim,
                    onValueChange = { reporterNim = it },
                    label = { Text("NIM / NIP") },
                    placeholder = { Text("23064...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = reporterPhone,
                onValueChange = { reporterPhone = it },
                label = { Text("No. WhatsApp / HP Aktif *") },
                placeholder = { Text("081234567890") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_contact"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Kirim Laporan
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        Toast.makeText(context, "Mohon lengkapi judul dan deskripsi barang!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (reporterPhone.isBlank()) {
                        Toast.makeText(context, "Mohon cantumkan nomor WhatsApp/HP untuk kontak!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    val newReport = Report(
                        type = reportType,
                        title = title.trim(),
                        category = selectedCategory,
                        description = description.trim(),
                        imageUrl = selectedImageUri?.toString() ?: "",
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName,
                        campusZone = selectedCampus,
                        timestamp = System.currentTimeMillis(),
                        status = Report.STATUS_OPEN,
                        userName = reporterName.ifBlank { "Civitas UNESA" },
                        userNim = reporterNim,
                        userContact = reporterPhone.trim()
                    )

                    viewModel.submitReport(newReport) {
                        isSubmitting = false
                        Toast.makeText(context, "Laporan berhasil diterbitkan!", Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_report"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportType == Report.TYPE_LOST) LostRed else UnesaBluePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (reportType == Report.TYPE_LOST) "Terbitkan Laporan Barang Hilang" else "Terbitkan Laporan Barang Ditemukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onLocationResult: (Location) -> Unit) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onLocationResult(loc)
            }
        }
    } catch (e: Exception) {
        // Fallback default Ketintang
    }
}
