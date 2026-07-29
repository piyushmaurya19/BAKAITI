import os

filepath = "app/src/main/java/com/bakaiti/chat/MainActivity.kt"
try:
    with open(filepath, "r") as f:
        code = f.read()

    if "override fun onResume()" in code:
        # 1. Jahan se problem shuru hui wahan se file ko split karein
        parts = code.split("override fun onResume()", 1)
        before = parts[0]
        
        # 2. Check karein kitne brackets '{' khule reh gaye hain
        open_count = before.count("{")
        close_count = before.count("}")
        unmatched = open_count - close_count
        
        # 3. Agar 'onCreate' ya 'setContent' band nahi hua, toh brackets add karein
        if unmatched > 1:
            before += "\n" + "    }\n" * (unmatched - 1)
            
        # 4. Sahi structure mein methods ready karein
        methods = """    override fun onResume() {
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
        # 5. File ko theek kiye gaye code se overwrite karein
        with open(filepath, "w") as f:
            f.write(before + methods)
        print("✅ Python script ne MainActivity.kt ke brackets fix kar diye hain!")
    else:
        print("❌ Code pehle se sahi lag raha hai ya file mein onResume nahi mila.")
except Exception as e:
    print(f"Error: {e}")
