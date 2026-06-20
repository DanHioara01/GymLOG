package com.example.gymlog2

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = UserProfileManager(applicationContext).getOwnUserId()
        if (userId != "local_user") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirestoreHelper().saveFcmToken(userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    NetworkClient.api.upsertUser(mapOf(
                        "id" to userId,
                        "fcmToken" to token
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Kinetic"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val senderName = remoteMessage.data["fromUserName"] ?: body

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showFriendRequestNotification(
            targetUserId = UserProfileManager(applicationContext).getOwnUserId(),
            senderName = senderName
        )
    }
}
