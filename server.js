const admin = require("firebase-admin");
const express = require("express");

// Apni master key load karein
const serviceAccount = require("./serviceAccountKey.json");

const app = express();
const port = process.env.PORT || 3000;

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
console.log("🚀 Firebase Admin Initialized!");

// Database mein 'notifications' collection ko dekhte raho
db.collection("notifications").onSnapshot(snapshot => {
  snapshot.docChanges().forEach(async (change) => {
    if (change.type === "added") {
      const data = change.doc.data();
      const docId = change.doc.id;
      const receiverId = data.receiverId;
      const senderName = data.senderName;
      const message = data.message;

      try {
        const userDoc = await db.collection("users").doc(receiverId).get();
        if (userDoc.exists && userDoc.data().fcmToken) {
          const fcmToken = userDoc.data().fcmToken;

          const payload = {
            notification: {
              title: `New message from ${senderName}`,
              body: message,
            },
            token: fcmToken,
          };

          await admin.messaging().send(payload);
          console.log(`✅ Notification sent to ${senderName}`);
          
          await db.collection("notifications").doc(docId).delete();
        }
      } catch (error) {
        console.error("❌ Error sending notification:", error);
      }
    }
  });
});

// Dummy route taaki Render ko lage website chal rahi hai
app.get('/', (req, res) => {
    res.send("✅ Bakaiti Notification Server is RUNNING 24/7!");
});

app.listen(port, () => {
    console.log(`🌐 Web server listening on port ${port}`);
});
