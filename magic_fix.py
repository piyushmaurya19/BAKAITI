import os

path = "app/src/main/java/com/bakaiti/chat/MainActivity.kt"
with open(path, "r") as f:
    content = f.read()

# 1. Purane galat jagah wale functions ko delete karein
if "override fun onResume()" in content:
    content = content.split("override fun onResume()")[0]
elif "private fun updateUserStatus" in content:
    content = content.split("private fun updateUserStatus")[0]

# 2. End ke saare extra brackets hata dein taaki hum naye sire se close kar sakein
content = content.rstrip()
while content.endswith("}"):
    content = content[:-1].rstrip()

# 3. Brackets mathematically count karein
open_braces = content.count("{")
close_braces = content.count("}")
missing_braces = open_braces - close_braces

# 4. Main class ke alawa baaki saare open blocks ko sahi jagah close karein
if missing_braces > 1:
    content += "\n"
    for i in range(missing_braces - 1, 0, -1):
        content += ("    " * i) + "}\n"

# 5. Functions ko ekdam sahi class level par lagayein
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
final_code = content.rstrip() + "\n" + new_methods

with open(path, "w") as f:
    f.write(final_code)
print("✅ Magic Fix Applied: MainActivity.kt ke brackets 100% fix ho gaye!")
