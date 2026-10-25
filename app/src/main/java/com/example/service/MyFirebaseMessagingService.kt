package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service penerima pesan FCM (Firebase Cloud Messaging) real-time untuk aplikasi
 * UNESA Lost and Found. Menerima sinyal kecocokan dari Firebase Cloud Functions.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val CHANNEL_ID = "unesa_lost_found_channel"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Pesan FCM Diterima Dari: ${remoteMessage.from}")

        // 1. Ekstrak data payload dari FCM Cloud Function
        val data = remoteMessage.data
        val score = data["confidence_score"] ?: "85"
        val lostTitle = data["lost_title"] ?: "Barang Anda"
        val foundTitle = data["found_title"] ?: "Barang Temuan"
        val summary = data["match_summary"] ?: "Sistem Adaptive Match mendeteksi kecocokan $score%"

        val title = remoteMessage.notification?.title ?: "Peluang Kecocokan Ditemukan!"
        val body = remoteMessage.notification?.body ?: "Kecocokan $score% antara '$lostTitle' dan '$foundTitle'"

        // 2. Tampilkan Android System Notification
        sendNotification(
            title = title,
            messageBody = body,
            score = score,
            detailedSummary = summary
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
        // Di sini token dapat dikirimkan ke koleksi users di Firestore
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        score: String,
        detailedSummary: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("OPEN_TAB", "MATCHES")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat NotificationChannel untuk Android O (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UNESA Lost & Found Matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi cerdas saat barang hilang dan ditemukan memiliki skor kecocokan tinggi (>75%)"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$messageBody\n\nSkor Kecocokan: $score%\nPerincian: $detailedSummary")
            )
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
