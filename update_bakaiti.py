import os

print("Starting BAKAITI code update via Python...")

# 1. Create MyFirebaseMessagingService.kt
service_code = """package com.bakaiti.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: "New Message"
        val body = remoteMessage.notification?.body ?: ""
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        val channelId = "BAKAITI_CHAT_CHANNEL"
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Chat", NotificationManager.IMPORTANCE_HIGH))
        }
        manager.notify(0, builder.build())
    }
}
"""
service_path = "app/src/main/java/com/bakaiti/chat/MyFirebaseMessagingService.kt"
os.makedirs(os.path.dirname(service_path), exist_ok=True)
with open(service_path, "w") as f:
    f.write(service_code)
print("-> Created MyFirebaseMessagingService.kt")

# 2. Update build.gradle
gradle_path = "app/build.gradle"
with open(gradle_path, "r") as f:
    gradle_data = f.read()

if "firebase-messaging-ktx" not in gradle_data:
    gradle_data = gradle_data.replace(
        "dependencies {", 
        "dependencies {\n    implementation 'com.google.firebase:firebase-messaging-ktx:23.4.1'"
    )
    with open(gradle_path, "w") as f:
        f.write(gradle_data)
    print("-> Updated build.gradle")
else:
    print("-> FCM dependency already exists.")

# 3. Update AndroidManifest.xml
manifest_path = "app/src/main/AndroidManifest.xml"
with open(manifest_path, "r") as f:
    manifest_data = f.read()

if "android.permission.POST_NOTIFICATIONS" not in manifest_data:
    manifest_data = manifest_data.replace(
        "<application", 
        '    <uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n\n    <application'
    )

if "MyFirebaseMessagingService" not in manifest_data:
    service_xml = """
        <service android:name=".MyFirebaseMessagingService" android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>"""
    manifest_data = manifest_data.replace("</application>", f"{service_xml}\n    </application>")
    
with open(manifest_path, "w") as f:
    f.write(manifest_data)
print("-> Updated AndroidManifest.xml")

# 4. Update MainActivity.kt for Online/Offline Status
ma_path = "app/src/main/java/com/bakaiti/chat/MainActivity.kt"
with open(ma_path, "r") as f:
    ma_data = f.read()

if "updateUserStatus" not in ma_data:
    methods = """
    override fun onResume() {
        super.onResume()
        updateUserStatus(isOnline = true)
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus(isOnline = false)
    }

    private fun updateUserStatus(isOnline: Boolean) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .update(mapOf("isOnline" to isOnline, "lastSeen" to System.currentTimeMillis()))
        }
    }
}"""
    last_brace_index = ma_data.rfind('}')
    if last_brace_index != -1:
        ma_data = ma_data[:last_brace_index] + methods + ma_data[last_brace_index+1:]
        with open(ma_path, "w") as f:
            f.write(ma_data)
        print("-> Updated MainActivity.kt")
else:
    print("-> MainActivity.kt already has online status logic.")

print("✅ Push Notifications and Online Status successfully configured via Python!")
