package com.example.takaful.service

import com.example.takaful.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Try to get notification from the notification payload or data payload
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "إشعار جديد من تكافل"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        if (body.isNotEmpty()) {
            NotificationHelper.showNotification(this, title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If user is logged in, update their token in Firestore
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .update("fcmToken", token)
        }
    }
}
