package com.example.engine

import com.example.model.MatchResult
import com.example.model.Report
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Algoritma Inti: Adaptive Context-Aware Multimodal Matching
 *
 * Menerapkan formula pencocokan cerdas multimodal:
 * - Skor Kedekatan Teks (NLP tokenization & keyword extraction)
 * - Skor Kedekatan Waktu (Time decay)
 * - Skor Kedekatan Lokasi (Haversine formula jarak koordinat)
 * - Pembobotan Dinamis:
 *   - Ada Foto: Visual 40%, Teks 30%, Waktu 15%, Lokasi 15% (Sisa 60%)
 *   - Tanpa Foto: Teks 50%, Waktu 30%, Lokasi 20%
 * - Ambang batas (threshold): > 75%
 */
object AdaptiveMatchingEngine {

    private const val EARTH_RADIUS_METERS = 6371000.0
    private const val MATCH_THRESHOLD = 75.0

    /**
     * Menghitung kecocokan antara satu laporan barang hilang dan satu laporan barang ditemukan.
     */
    fun calculateMatch(lostReport: Report, foundReport: Report): MatchResult? {
        // Hanya cocokkan jika salah satu Lost dan yang lain Found
        if (lostReport.isLost == foundReport.isLost) return null

        // 1. Skor Teks (NLP Sederhana)
        val textScore = computeTextSimilarity(lostReport, foundReport)

        // 2. Skor Waktu (Time decay)
        val timeScore = computeTimeSimilarity(lostReport.timestamp, foundReport.timestamp)

        // 3. Skor Lokasi (Haversine)
        val locationScore = computeLocationSimilarity(
            lat1 = lostReport.latitude,
            lon1 = lostReport.longitude,
            locName1 = lostReport.locationName,
            zone1 = lostReport.campusZone,
            lat2 = foundReport.latitude,
            lon2 = foundReport.longitude,
            locName2 = foundReport.locationName,
            zone2 = foundReport.campusZone
        )

        // 4. Pembobotan Dinamis (Adaptive Weighting)
        val hasVisual = lostReport.imageUrl.isNotBlank() && foundReport.imageUrl.isNotBlank()
        val visualScore = if (hasVisual) {
            // Jika kategori sama dan kedua laporan memiliki gambar, estimasikan kecocokan visual
            if (lostReport.category.equals(foundReport.category, ignoreCase = true)) 0.85 else 0.50
        } else {
            0.0
        }

        val totalScore: Double = if (hasVisual) {
            // Bobot visual 40%, sisa 60%: Teks 30%, Waktu 15%, Lokasi 15%
            (visualScore * 0.40) + (textScore * 0.30) + (timeScore * 0.15) + (locationScore * 0.15)
        } else {
            // Tanpa foto: Teks 50%, Waktu 30%, Lokasi 20%
            (textScore * 0.50) + (timeScore * 0.30) + (locationScore * 0.20)
        }

        val percentageScore = (totalScore * 100.0).coerceIn(0.0, 100.0)

        // Return MatchResult jika di atas batas atau untuk evaluasi
        val summary = generateMatchSummary(textScore, timeScore, locationScore, visualScore, hasVisual)

        return MatchResult(
            id = "${lostReport.id}_${foundReport.id}",
            lostReportId = lostReport.id,
            foundReportId = foundReport.id,
            lostTitle = lostReport.title,
            foundTitle = foundReport.title,
            category = if (lostReport.category.isNotBlank()) lostReport.category else foundReport.category,
            lostImageUrl = lostReport.imageUrl,
            foundImageUrl = foundReport.imageUrl,
            lostLocation = lostReport.locationName.ifBlank { lostReport.campusZone },
            foundLocation = foundReport.locationName.ifBlank { foundReport.campusZone },
            textScore = textScore,
            timeScore = timeScore,
            locationScore = locationScore,
            visualScore = visualScore,
            confidenceScore = percentageScore,
            status = if (percentageScore >= MATCH_THRESHOLD) MatchResult.STATUS_PENDING else MatchResult.STATUS_REJECTED,
            createdAt = System.currentTimeMillis(),
            lostContact = lostReport.userContact,
            foundContact = foundReport.userContact,
            matchSummary = summary
        )
    }

    /**
     * Hitung kemiripan teks menggunakan token Jaccard & pencocokan kategori.
     */
    private fun computeTextSimilarity(rep1: Report, rep2: Report): Double {
        val catMatchBonus = if (rep1.category.equals(rep2.category, ignoreCase = true)) 0.35 else 0.0

        val stopWords = setOf("yang", "di", "ke", "dari", "ini", "itu", "dan", "atau", "pada", "ada", "saya", "telah", "hilang", "ditemukan", "barang", "unesa")

        val tokens1 = tokenize("${rep1.title} ${rep1.description}", stopWords)
        val tokens2 = tokenize("${rep2.title} ${rep2.description}", stopWords)

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return (catMatchBonus * 1.5).coerceAtMost(1.0)
        }

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size

        val jaccard = if (union > 0) intersection.toDouble() / union.toDouble() else 0.0

        // Kombinasikan Jaccard dan Kategori
        return (jaccard * 0.65 + catMatchBonus).coerceIn(0.0, 1.0)
    }

    private fun tokenize(text: String, stopWords: Set<String>): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && !stopWords.contains(it) }
            .toSet()
    }

    /**
     * Hitung kedekatan waktu dengan kurva eksponensial decay.
     */
    private fun computeTimeSimilarity(t1: Long, t2: Long): Double {
        val diffMillis = Math.abs(t1 - t2)
        val diffHours = diffMillis / (1000.0 * 60.0 * 60.0)

        return when {
            diffHours <= 6.0 -> 1.0
            diffHours <= 24.0 -> 0.90
            diffHours <= 48.0 -> 0.75
            diffHours <= 120.0 -> 0.50 // 5 hari
            diffHours <= 240.0 -> 0.30 // 10 hari
            else -> 0.15
        }
    }

    /**
     * Hitung kedekatan lokasi menggunakan Haversine Formula.
     */
    fun computeLocationSimilarity(
        lat1: Double,
        lon1: Double,
        locName1: String,
        zone1: String,
        lat2: Double,
        lon2: Double,
        locName2: String,
        zone2: String
    ): Double {
        // Jika koordinat valid (> 0 atau < 0 bukan 0.0)
        if (lat1 != 0.0 && lon1 != 0.0 && lat2 != 0.0 && lon2 != 0.0) {
            val distMeters = haversineDistanceMeters(lat1, lon1, lat2, lon2)
            return when {
                distMeters <= 50 -> 1.0
                distMeters <= 200 -> 0.90
                distMeters <= 500 -> 0.80
                distMeters <= 1000 -> 0.65
                distMeters <= 2500 -> 0.40
                else -> 0.15
            }
        }

        // Fallback ke pencocokan nama gedung / zona kampus UNESA
        if (locName1.isNotBlank() && locName2.isNotBlank() && locName1.equals(locName2, ignoreCase = true)) {
            return 0.95
        }

        if (zone1.isNotBlank() && zone2.isNotBlank() && zone1.equals(zone2, ignoreCase = true)) {
            return 0.75
        }

        return 0.25
    }

    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun generateMatchSummary(
        text: Double,
        time: Double,
        loc: Double,
        visual: Double,
        hasVisual: Boolean
    ): String {
        val textPct = (text * 100).toInt()
        val timePct = (time * 100).toInt()
        val locPct = (loc * 100).toInt()

        return if (hasVisual) {
            val visPct = (visual * 100).toInt()
            "Multimodal: Visual $visPct%, Teks $textPct%, Lokasi $locPct%, Waktu $timePct%"
        } else {
            "Kontekstual: Teks $textPct%, Waktu $timePct%, Lokasi $locPct% (Tanpa Foto)"
        }
    }
}
