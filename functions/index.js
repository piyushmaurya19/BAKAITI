const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendChatNotification = functions.firestore
  .document("notifications/{docId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const receiverId = data.receiverId;
    const senderName = data.senderName;
    const message = data.message;

    try {
      // 1. Database se receiver ka FCM token nikalna
      const userDoc = await admin.firestore().collection("users").doc(receiverId).get();
      
      if (!userDoc.exists) {
        console.log("User nahi mila:", receiverId);
        return null;
      }

      const fcmToken = userDoc.data().fcmToken;
      if (!fcmToken) {
        console.log("FCM Token nahi mila is user ke liye:", receiverId);
        return null;
      }

      // 2. Notification ka payload (Data) taiyaar karna
      const payload = {
        notification: {
          title: `New message from ${senderName}`,
          body: message,
        },
        token: fcmToken,
      };

      // 3. Notification push karna
      const response = await admin.messaging().send(payload);
      console.log("Notification bhej di gayi:", response);

      // 4. (Optional) Bhejne ke baad us notification document ko delete kar do taaki database na bhare
      await snap.ref.delete();

      return null;
    } catch (error) {
      console.error("Notification bhejne mein error aaya:", error);
      return null;
    }
  });
