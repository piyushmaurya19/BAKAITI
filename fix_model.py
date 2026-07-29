import os
import re

folder = "app/src/main/java/com/bakaiti/chat"
updated = False

for root, dirs, files in os.walk(folder):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r") as f:
                code = f.read()
            
            # Check karte hain ki UserProfile is file mein hai ya nahi
            if "data class UserProfile" in code:
                if "val isOnline" not in code:
                    # Automatically isOnline add karna
                    pattern = r"(data class UserProfile\s*\([\s\S]*?)(\s*\))"
                    replacement = r"\1,\n    val isOnline: Boolean = false\2"
                    new_code = re.sub(pattern, replacement, code, count=1)
                    
                    with open(path, "w") as f:
                        f.write(new_code)
                    print(f"✅ Magic Fix Applied: 'isOnline' automatically {file} mein add ho gaya!")
                else:
                    print(f"✅ 'isOnline' pehle se hi {file} mein maujood hai.")
                updated = True
                break
    if updated:
        break

if not updated:
    print("❌ Error: UserProfile class project mein nahi mili!")
