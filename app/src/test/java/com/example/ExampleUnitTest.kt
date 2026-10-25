package com.example

import com.example.engine.AdaptiveMatchingEngine
import com.example.model.Report
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testHaversineDistanceClosePoints() {
        // Jarak 2 titik berdekatan di UNESA Ketintang (~15 meter)
        val lat1 = -7.3155
        val lon1 = 112.7275
        val lat2 = -7.3154
        val lon2 = 112.7276

        val dist = AdaptiveMatchingEngine.haversineDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be under 50m", dist < 50.0)
    }

    @Test
    fun testAdaptiveMatchingHighConfidence() {
        val now = System.currentTimeMillis()

        val lostReport = Report(
            id = "rep_1",
            type = Report.TYPE_LOST,
            title = "KTM Mahasiswa Teknik Informatika UNESA",
            category = Report.CATEGORY_KTM,
            description = "Hilang KTM atas nama Himmati dengan lanyard biru FT UNESA",
            imageUrl = "https://example.com/ktm.jpg",
            latitude = -7.3155,
            longitude = 112.7275,
            locationName = "Gedung E1 FT",
            campusZone = Report.CAMPUS_KETINTANG,
            timestamp = now - (3600 * 1000L),
            status = Report.STATUS_OPEN
        )

        val foundReport = Report(
            id = "rep_2",
            type = Report.TYPE_FOUND,
            title = "Ditemukan KTM Mahasiswa Fakultas Teknik UNESA",
            category = Report.CATEGORY_KTM,
            description = "Ditemukan kartu mahasiswa KTM UNESA biru di lab lantai 2",
            imageUrl = "https://example.com/ktm_found.jpg",
            latitude = -7.3154,
            longitude = 112.7276,
            locationName = "Lab Komputer E1 FT",
            campusZone = Report.CAMPUS_KETINTANG,
            timestamp = now,
            status = Report.STATUS_OPEN
        )

        val match = AdaptiveMatchingEngine.calculateMatch(lostReport, foundReport)
        assertNotNull("Match should not be null", match)
        assertTrue("Match score should exceed 75% threshold", match!!.confidenceScore >= 75.0)
        assertTrue("isHighConfidence should be true", match.isHighConfidence)
    }
}
