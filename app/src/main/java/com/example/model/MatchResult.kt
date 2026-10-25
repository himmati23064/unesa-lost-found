package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Representasi hasil pencocokan otomatis (Adaptive Match) antara barang hilang
 * dan barang ditemukan dengan Confidence Score dan perincian bobot multimodal.
 */
data class MatchResult(
    @DocumentId
    val id: String = "",

    @PropertyName("lost_report_id")
    val lostReportId: String = "",

    @PropertyName("found_report_id")
    val foundReportId: String = "",

    @PropertyName("lost_title")
    val lostTitle: String = "",

    @PropertyName("found_title")
    val foundTitle: String = "",

    @PropertyName("category")
    val category: String = "",

    @PropertyName("lost_image_url")
    val lostImageUrl: String = "",

    @PropertyName("found_image_url")
    val foundImageUrl: String = "",

    @PropertyName("lost_location")
    val lostLocation: String = "",

    @PropertyName("found_location")
    val foundLocation: String = "",

    @PropertyName("text_score")
    val textScore: Double = 0.0, // 0.0 - 1.0

    @PropertyName("time_score")
    val timeScore: Double = 0.0, // 0.0 - 1.0

    @PropertyName("location_score")
    val locationScore: Double = 0.0, // 0.0 - 1.0

    @PropertyName("visual_score")
    val visualScore: Double = 0.0, // 0.0 - 1.0

    @PropertyName("confidence_score")
    val confidenceScore: Double = 0.0, // 0 - 100%

    @PropertyName("status")
    val status: String = STATUS_PENDING, // "Pending", "Confirmed", "Rejected"

    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @PropertyName("lost_contact")
    val lostContact: String = "",

    @PropertyName("found_contact")
    val foundContact: String = "",

    @PropertyName("match_summary")
    val matchSummary: String = ""
) {
    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_CONFIRMED = "Confirmed"
        const val STATUS_REJECTED = "Rejected"
    }

    val percentageInt: Int get() = confidenceScore.toInt().coerceIn(0, 100)
    val isHighConfidence: Boolean get() = confidenceScore >= 75.0
}
