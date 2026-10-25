package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.MatchResult
import com.example.model.Report
import com.example.ui.components.MatchNotificationCard
import com.example.ui.components.ReportCard
import com.example.ui.theme.FoundGreen
import com.example.ui.theme.LostRed
import com.example.ui.theme.UnesaBlueDark
import com.example.ui.theme.UnesaBluePrimary
import com.example.ui.theme.UnesaYellow
import com.example.viewmodel.ReportUiState
import com.example.viewmodel.ReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReportViewModel,
    onNavigateToAddReport: (String) -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Barang Hilang, 1: Barang Ditemukan, 2: Hasil Match
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) } // 0: Feed, 1: Matches, 2: Posko UNESA, 3: Akun
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Launcher izin notifikasi untuk Android 13+ (POST_NOTIFICATIONS)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi aktif! Anda akan menerima update kecocokan barang.", Toast.LENGTH_SHORT).show()
        }
    }

    // Dengarkan event one-shot dari ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = UnesaYellow,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "U",
                                    fontWeight = FontWeight.Black,
                                    color = UnesaBluePrimary,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UNESA Lost & Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Layanan Temuan & AI Match",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Tombol Search
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier.testTag("btn_toggle_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari Barang",
                            tint = Color.White
                        )
                    }

                    // Tombol Uji Algoritma Cerdas (Simulasi FCM & Match)
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.triggerAdaptiveMatchingSimulation(context)
                        },
                        modifier = Modifier.testTag("btn_trigger_match_simulation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Simulasi Match Cerdas",
                            tint = UnesaYellow
                        )
                    }

                    // Avatar Pengguna / Akses Manajemen Akun
                    IconButton(
                        onClick = onNavigateToAccount,
                        modifier = Modifier.testTag("btn_top_profile_account")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = UnesaYellow,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = currentUser?.name?.take(1)?.uppercase() ?: "U"
                                Text(
                                    text = initial,
                                    fontWeight = FontWeight.Black,
                                    color = UnesaBluePrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UnesaBluePrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val highMatchesCount = if (uiState is ReportUiState.Success) {
                (uiState as ReportUiState.Success).matches.count { it.isHighConfidence }
            } else 0

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 0,
                    onClick = { selectedBottomNavIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                    label = { Text("Feed") },
                    modifier = Modifier.testTag("nav_feed")
                )
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 1,
                    onClick = {
                        selectedBottomNavIndex = 1
                        selectedTabIndex = 2
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (highMatchesCount > 0) {
                                    Badge(containerColor = UnesaYellow) {
                                        Text(text = "$highMatchesCount", color = UnesaBluePrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Kecocokan AI")
                        }
                    },
                    label = { Text("Kecocokan") },
                    modifier = Modifier.testTag("nav_matches")
                )
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 2,
                    onClick = { selectedBottomNavIndex = 2 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Posko Satpam") },
                    label = { Text("Posko") },
                    modifier = Modifier.testTag("nav_posko")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAccount,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Akun Saya") },
                    label = { Text("Akun Saya") },
                    modifier = Modifier.testTag("nav_account")
                )
            }
        },
        floatingActionButton = {
            if (selectedBottomNavIndex != 2) {
                FloatingActionButton(
                    onClick = {
                        val currentType = if (selectedTabIndex == 1) Report.TYPE_FOUND else Report.TYPE_LOST
                        onNavigateToAddReport(currentType)
                    },
                    containerColor = if (selectedTabIndex == 0) LostRed else UnesaBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_report")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Lapor")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedTabIndex == 1) "Lapor Temuan" else "Lapor Hilang",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedBottomNavIndex) {
                2 -> {
                    // Layar Info Posko Satpam & Loket L&F UNESA
                    PoskoGuideView()
                }
                else -> {
                    // Banner Izin Notifikasi jika belum diizinkan pada Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        Surface(
                            color = UnesaYellow.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF855300))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Aktifkan notifikasi untuk menerima info kecocokan barang real-time",
                                        fontSize = 12.sp,
                                        color = Color(0xFF855300)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = UnesaBluePrimary,
                                    modifier = Modifier.clickable {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                ) {
                                    Text(
                                        text = "Izinkan",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar Expansible
                    AnimatedVisibility(visible = isSearchExpanded) {
                        val currentSearch = (uiState as? ReportUiState.Success)?.searchQuery ?: ""
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentSearch,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Cari barang, lokasi (mis: FT, Perpus)...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (currentSearch.isNotBlank()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("input_search"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // TabRow untuk Berpindah antara 'Barang Hilang' dan 'Barang Ditemukan' (+ Tab Kecocokan Cerdas)
                    val tabTitles = listOf("Barang Hilang", "Barang Ditemukan", "Kecocokan AI")
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = UnesaBluePrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = if (selectedTabIndex == 0) LostRed else if (selectedTabIndex == 1) FoundGreen else UnesaYellow
                            )
                        }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index
                                    if (index == 2) selectedBottomNavIndex = 1 else selectedBottomNavIndex = 0
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == index) {
                                            if (index == 0) LostRed else if (index == 1) FoundGreen else UnesaBluePrimary
                                        } else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.testTag("tab_$index")
                            )
                        }
                    }

                    // Chips Filter Kategori dan Kampus UNESA
                    FilterChipsBar(
                        selectedCategory = (uiState as? ReportUiState.Success)?.selectedCategory,
                        selectedCampus = (uiState as? ReportUiState.Success)?.selectedCampus,
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onCampusSelected = { viewModel.onCampusSelected(it) }
                    )

                    // Konten Utama berdasarkan UI State
                    when (val state = uiState) {
                        is ReportUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = UnesaBluePrimary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Memuat data laporan UNESA...", fontSize = 13.sp)
                                }
                            }
                        }

                        is ReportUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = LostRed, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(state.message, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = UnesaBluePrimary,
                                        modifier = Modifier.clickable { viewModel.refreshReports() }
                                    ) {
                                        Text("Coba Lagi", color = Color.White, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            }
                        }

                        is ReportUiState.Success -> {
                            when (selectedTabIndex) {
                                0 -> {
                                    // Feed Barang Hilang
                                    ReportListView(
                                        reports = state.filteredLostReports,
                                        userName = currentUser?.name ?: "Civitas UNESA",
                                        emptyMessage = "Belum ada laporan barang hilang di kategori/kampus ini.",
                                        onResolveClick = { viewModel.markAsResolved(it.id) }
                                    )
                                }
                                1 -> {
                                    // Feed Barang Ditemukan
                                    ReportListView(
                                        reports = state.filteredFoundReports,
                                        userName = currentUser?.name ?: "Civitas UNESA",
                                        emptyMessage = "Belum ada barang ditemukan yang dilaporkan.",
                                        onResolveClick = { viewModel.markAsResolved(it.id) }
                                    )
                                }
                                2 -> {
                                    // Tab Kecocokan Cerdas (Adaptive Match Results)
                                    MatchListView(
                                        matches = state.matches,
                                        onTriggerSim = { viewModel.triggerAdaptiveMatchingSimulation(context) },
                                        onConfirmMatch = {
                                            Toast.makeText(context, "Klaim berhasil dicatat ke sistem UNESA!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsBar(
    selectedCategory: String?,
    selectedCampus: String?,
    onCategorySelected: (String?) -> Unit,
    onCampusSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chip Kampus Ketintang
        CampusChip(
            label = "Kampus Ketintang",
            isSelected = selectedCampus == Report.CAMPUS_KETINTANG,
            onClick = { onCampusSelected(Report.CAMPUS_KETINTANG) }
        )

        // Chip Kampus Lidah Wetan
        CampusChip(
            label = "Kampus Lidah Wetan",
            isSelected = selectedCampus == Report.CAMPUS_LIDAH_WETAN,
            onClick = { onCampusSelected(Report.CAMPUS_LIDAH_WETAN) }
        )

        // Divider kecil
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Chips Kategori
        Report.ALL_CATEGORIES.take(5).forEach { cat ->
            val isSelected = selectedCategory == cat
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) UnesaBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, UnesaBluePrimary) else null,
                modifier = Modifier.clickable { onCategorySelected(cat) }
            ) {
                Text(
                    text = cat,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) UnesaBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CampusChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) UnesaBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ReportListView(
    reports: List<Report>,
    userName: String,
    emptyMessage: String,
    onResolveClick: (Report) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lazy_report_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Banner Sambutan Kampus UNESA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.unesa_campus_hero),
                        contentDescription = "Kampus UNESA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Halo, $userName! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pusat pelaporan terpadu & pencocokan cerdas barang hilang civitas UNESA",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(reports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    onResolveClick = { onResolveClick(report) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun MatchListView(
    matches: List<MatchResult>,
    onTriggerSim: () -> Unit,
    onConfirmMatch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lazy_matches_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header Info Algoritma
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = UnesaBluePrimary.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = UnesaBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Algoritma Pencocokan Multimodal Aktif",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = UnesaBluePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Algoritma membandingkan teks NLP, Haversine lokasi kampus, jeda waktu, dan fitur visual. Notifikasi dipicu saat skor > 75%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (matches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada kecocokan yang terdeteksi.\nTekan tombol simulasi di kanan atas untuk memicu algoritma.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(matches, key = { it.id }) { match ->
                MatchNotificationCard(
                    match = match,
                    onConfirmMatch = onConfirmMatch
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun PoskoGuideView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Posko Resmi Lost & Found UNESA",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = UnesaBluePrimary
        )
        Text(
            text = "Jika menemukan barang berharga, harap segera serahkan ke posko satpam terdekat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        PoskoCard(
            campus = "Kampus Ketintang",
            location = "Pos Satpam Gerbang Utama & Rektorat",
            contact = "031-8299945 (Satpam Ketintang)",
            hours = "24 Jam Setiap Hari",
            notes = "Melayani penyimpanan barang hilang di FT, FEB, FMIPA, FISH."
        )

        Spacer(modifier = Modifier.height(12.dp))

        PoskoCard(
            campus = "Kampus Lidah Wetan",
            location = "Posko Keamanan Gedung Rektorat & Perpustakaan Pusat",
            contact = "031-7532160 (Satpam Lidah)",
            hours = "24 Jam Setiap Hari",
            notes = "Melayani penyimpanan barang hilang di FIP, FBS, FIKK, Danau & Graha UNESA."
        )
    }
}

@Composable
private fun PoskoCard(
    campus: String,
    location: String,
    contact: String,
    hours: String,
    notes: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(campus, fontWeight = FontWeight.Bold, color = UnesaBluePrimary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Lokasi: $location", style = MaterialTheme.typography.bodyMedium)
            Text("Kontak: $contact", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("Jam Operasional: $hours", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
