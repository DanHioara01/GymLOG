import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

export const onFriendRequestCreated = functions.firestore
  .document("friend_requests/{requestId}")
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data) return;

    const toUserId = data.toUserId;
    const fromUserName = data.fromUserName || "Someone";
    const fromUserPhoto = data.fromUserPhoto || "";

    // Get target user's FCM token
    const userDoc = await db.collection("users").doc(toUserId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) {
      console.log(`No FCM token for user ${toUserId}`);
      return;
    }

    // Send push notification
    const message: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: "Cerere de prietenie",
        body: `${fromUserName} v-a trimis o cerere de prietenie!`,
      },
      data: {
        type: "friend_request",
        fromUserId: data.fromUserId,
        fromUserName: fromUserName,
        requestId: context.params.requestId,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "friend_requests",
          priority: "high",
        },
      },
    };

    try {
      await messaging.send(message);
      console.log(`Notification sent to ${toUserId}`);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  });
