package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchResult
import com.example.ui.theme.FoundGreen
import com.example.ui.theme.LostRed
import com.example.ui.theme.MatchConfidenceHigh
import com.example.ui.theme.MatchConfidenceMed
import com.example.ui.theme.UnesaBluePrimary
import com.example.ui.theme.UnesaYellow

/**
 * Komponen UI Khusus untuk menampilkan notifikasi kecocokan barang
 * dengan Circular Progress Indicator persentase skor (Match Confidence Score).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchNotificationCard(
    match: MatchResult,
    modifier: Modifier = Modifier,
    onConfirmMatch: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var progressTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(match.confidenceScore) {
        progressTarget = (match.confidenceScore / 100.0).toFloat().coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "confidence_progress"
    )

    val scoreColor = when {
        match.confidenceScore >= 75.0 -> MatchConfidenceHigh
        match.confidenceScore >= 60.0 -> MatchConfidenceMed
        else -> Color(0xFF757575)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_notification_card_${match.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Judul Algoritma + Circular Score Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UnesaBluePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Pencocokan Cerdas",
                            tint = UnesaBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Peluang Kecocokan Ditemukan!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adaptive Multimodal Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = UnesaBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Circular Progress Indicator Skor Kecocokan
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("match_circular_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier.size(56.dp),
                        color = scoreColor.copy(alpha = 0.15f),
                        strokeWidth = 5.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(56.dp),
                        color = scoreColor,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${match.percentageInt}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                            text = "MATCH",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Komparasi Barang Hilang vs Barang Ditemukan
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Item Hilang
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LostRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HILANG",
                                color = LostRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = match.lostTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Item Ditemukan
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FoundGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TEMUAN",
                                color = FoundGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = match.foundTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lokasi Temuan
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lokasi: ${match.foundLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-skor chips (NLP Teks, Waktu, Lokasi Haversine)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScoreChip(label = "Teks NLP", scorePercent = (match.textScore * 100).toInt())
                ScoreChip(label = "Waktu", scorePercent = (match.timeScore * 100).toInt())
                ScoreChip(label = "Lokasi GPS", scorePercent = (match.locationScore * 100).toInt())
                if (match.visualScore > 0) {
                    ScoreChip(label = "Visual", scorePercent = (match.visualScore * 100).toInt())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Hubungi & Konfirmasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val contactPhone = match.foundContact.ifBlank { match.lostContact }
                        if (contactPhone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_contact_finder"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Hubungi",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hubungi", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onConfirmMatch?.invoke()
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_confirm_match"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnesaBluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Klaim Cocok",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Klaim Cocok", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, scorePercent: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$scorePercent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (scorePercent >= 75) MatchConfidenceHigh else MaterialTheme.colorScheme.primary
            )
        }
    }
}
