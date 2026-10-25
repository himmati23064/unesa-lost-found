package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Representasi dokumen laporan barang hilang (Lost) atau barang ditemukan (Found)
 * untuk civitas akademika Universitas Negeri Surabaya (UNESA).
 */
data class Report(
    @DocumentId
    val id: String = "",

    @PropertyName("type")
    val type: String = TYPE_LOST, // "Lost" or "Found"

    @PropertyName("title")
    val title: String = "",

    @PropertyName("category")
    val category: String = CATEGORY_KTM,

    @PropertyName("description")
    val description: String = "",

    @PropertyName("image_url")
    val imageUrl: String = "",

    @PropertyName("latitude")
    val latitude: Double = 0.0,

    @PropertyName("longitude")
    val longitude: Double = 0.0,

    @PropertyName("location_name")
    val locationName: String = "",

    @PropertyName("campus_zone")
    val campusZone: String = CAMPUS_KETINTANG, // "Kampus Ketintang" or "Kampus Lidah Wetan"

    @PropertyName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @PropertyName("status")
    val status: String = STATUS_OPEN, // "Open", "Resolved", "Matched"

    @PropertyName("user_id")
    val userId: String = "",

    @PropertyName("user_name")
    val userName: String = "",

    @PropertyName("user_contact")
    val userContact: String = "",

    @PropertyName("user_nim")
    val userNim: String = ""
) {
    companion object {
        const val TYPE_LOST = "Lost"
        const val TYPE_FOUND = "Found"

        const val STATUS_OPEN = "Open"
        const val STATUS_RESOLVED = "Resolved"
        const val STATUS_MATCHED = "Matched"

        const val CAMPUS_KETINTANG = "Kampus Ketintang"
        const val CAMPUS_LIDAH_WETAN = "Kampus Lidah Wetan"

        const val CATEGORY_KTM = "KTM / Kartu Identitas"
        const val CATEGORY_ELECTRONICS = "Elektronik & HP"
        const val CATEGORY_LAPTOP = "Laptop & Aksesoris"
        const val CATEGORY_WALLET = "Dompet & Uang"
        const val CATEGORY_KEYS = "Kunci Kendaraan / Kos"
        const val CATEGORY_BAG = "Tas & Ransel"
        const val CATEGORY_DOCUMENTS = "Buku & Berkas Akademik"
        const val CATEGORY_ACCESSORIES = "Aksesoris & Jam Tangan"
        const val CATEGORY_OTHER = "Lainnya"

        val ALL_CATEGORIES = listOf(
            CATEGORY_KTM,
            CATEGORY_ELECTRONICS,
            CATEGORY_LAPTOP,
            CATEGORY_WALLET,
            CATEGORY_KEYS,
            CATEGORY_BAG,
            CATEGORY_DOCUMENTS,
            CATEGORY_ACCESSORIES,
            CATEGORY_OTHER
        )

        val ALL_CAMPUSES = listOf(
            CAMPUS_KETINTANG,
            CAMPUS_LIDAH_WETAN
        )
    }

    val isLost: Boolean get() = type.equals(TYPE_LOST, ignoreCase = true)
    val isFound: Boolean get() = type.equals(TYPE_FOUND, ignoreCase = true)
    val isOpen: Boolean get() = status.equals(STATUS_OPEN, ignoreCase = true)
}
