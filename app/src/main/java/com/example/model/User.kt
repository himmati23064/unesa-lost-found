package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val uid: String = "user_23064",

    @PropertyName("name")
    val name: String = "Himmati",

    @PropertyName("email")
    val email: String = "himmati.23064@mhs.unesa.ac.id",

    @PropertyName("nim")
    val nim: String = "23064",

    @PropertyName("phone")
    val phone: String = "081234567890",

    @PropertyName("avatar_url")
    val avatarUrl: String = "",

    @PropertyName("fcm_token")
    val fcmToken: String = "",

    @PropertyName("faculty")
    val faculty: String = "Fakultas Teknik (FT)",

    @PropertyName("study_program")
    val studyProgram: String = "S1 Teknik Informatika",

    @PropertyName("primary_campus")
    val primaryCampus: String = "Kampus Ketintang",

    @PropertyName("role")
    val role: String = "Mahasiswa Aktif",

    @PropertyName("reputation_score")
    val reputationScore: Int = 98,

    @PropertyName("total_reports")
    val totalReports: Int = 3,

    @PropertyName("total_resolved")
    val totalResolved: Int = 2
)
