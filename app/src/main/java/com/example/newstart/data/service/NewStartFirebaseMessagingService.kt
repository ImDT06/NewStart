package com.example.newstart.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.newstart.MainActivity
import com.example.newstart.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.newstart.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NewStartFirebaseMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseMessagingEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val entryPoint = EntryPointAccessors.fromApplication(
            this,
            FirebaseMessagingEntryPoint::class.java
        )
        val repo = entryPoint.userPreferencesRepository()
        val isEnabled = runBlocking { repo.isCommunityNotificationsEnabledFlow.first() }
        
        if (!isEnabled) {
            Log.d(TAG, "Thông báo cộng đồng đã bị tắt.")
            return
        }

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "NewStart"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Bạn có tin nhắn mới"
        val squadId = remoteMessage.data["squadId"]

        sendNotification(title, body, squadId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUserId.isNullOrEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated successfully in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating FCM token in Firestore: ${e.message}")
                }
        }
    }

    private fun sendNotification(title: String, body: String, squadId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            squadId?.let { putExtra("squadId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            squadId.hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "squad_chat_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tin nhắn nhóm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi có tin nhắn mới trong nhóm"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FCM_Service"
    }
}
