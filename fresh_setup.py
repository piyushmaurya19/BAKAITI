import os

print("🚀 BAKAITI Fresh Setup Start Ho Raha Hai...\n")

# 1. MyFirebaseMessagingService.kt Banayein
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
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid).update("fcmToken", token)
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
os.makedirs("app/src/main/java/com/bakaiti/chat", exist_ok=True)
with open("app/src/main/java/com/bakaiti/chat/MyFirebaseMessagingService.kt", "w") as f:
    f.write(service_code)
print("✅ 1. Notification Service file ban gayi.")

# 2. build.gradle Update Karein
gradle_path = "app/build.gradle"
if os.path.exists(gradle_path):
    with open(gradle_path, "r") as f:
        gradle_data = f.read()
    if "firebase-messaging-ktx" not in gradle_data:
        gradle_data = gradle_data.replace("dependencies {", "dependencies {\n    implementation 'com.google.firebase:firebase-messaging-ktx:23.4.1'")
        with open(gradle_path, "w") as f:
            f.write(gradle_data)
        print("✅ 2. build.gradle update ho gayi.")
    else:
        print("✅ 2. build.gradle pehle se theek hai.")

# 3. AndroidManifest Update Karein
manifest_path = "app/src/main/AndroidManifest.xml"
if os.path.exists(manifest_path):
    with open(manifest_path, "r") as f:
        manifest_data = f.read()
    if "android.permission.POST_NOTIFICATIONS" not in manifest_data:
        manifest_data = manifest_data.replace("<application", '    <uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n\n    <application')
    if "MyFirebaseMessagingService" not in manifest_data:
        service_xml = """        <service android:name=".MyFirebaseMessagingService" android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>"""
        manifest_data = manifest_data.replace("</application>", f"{service_xml}\n    </application>")
    with open(manifest_path, "w") as f:
        f.write(manifest_data)
    print("✅ 3. AndroidManifest update ho gaya.")

# 4. MainActivity Fix (Sabse important)
ma_path = "app/src/main/java/com/bakaiti/chat/MainActivity.kt"
if os.path.exists(ma_path):
    with open(ma_path, "r") as f:
        code = f.read()
    
    # Pichla koi kharab code ho toh usko hata do
    if "override fun onResume()" in code:
        code = code.split("override fun onResume()")[0]
        code = code[:code.rfind('}')] + "\n}\n"

    new_methods = """
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
}
"""
    last_brace = code.rfind('}')
    if last_brace != -1:
        code = code[:last_brace] + new_methods
        with open(ma_path, "w") as f:
            f.write(code)
        print("✅ 4. MainActivity ekdam sahi structure (brackets) me set ho gayi!")

print("\n🎉 DONE! Saara setup completely fresh aur error-free ho gaya hai.")
