package com.bakaiti.chat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonBlue,
    unfocusedBorderColor = SurfaceDark,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = NeonBlue,
    unfocusedLabelColor = Color.Gray,
    cursorColor = NeonBlue
)

@Composable
fun AuthScreen(onAuthed: () -> Unit) {
    val context = LocalContext.current
    var isSignup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { onAuthed() }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Firebase sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign-in failed: code ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bakaiti", color = NeonBlue, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Connect with friends, the Desi way!", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("— or use email —", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(18.dp))

            if (isSignup) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            errorMsg?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                enabled = !loading,
                onClick = {
                    errorMsg = null
                    if (email.isBlank() || password.length < 6) {
                        errorMsg = "Enter a valid email and a password (6+ chars)"
                        return@Button
                    }
                    loading = true
                    if (isSignup) {
                        if (displayName.trim().isEmpty() || username.trim().length < 3) {
                            loading = false
                            errorMsg = "Enter your name and a username (3+ chars)"
                            return@Button
                        }
                        val cleanUsername = username.trim().lowercase()
                        firestore.collection("users")
                            .whereEqualTo("usernameLower", cleanUsername)
                            .get()
                            .addOnSuccessListener { snap ->
                                if (!snap.isEmpty) {
                                    loading = false
                                    errorMsg = "Username already taken"
                                } else {
                                    auth.createUserWithEmailAndPassword(email.trim(), password)
                                        .addOnSuccessListener { result ->
                                            val uid = result.user?.uid ?: return@addOnSuccessListener
                                            result.user?.sendEmailVerification()
                                            val data = hashMapOf(
                                                "uid" to uid,
                                                "username" to username.trim(),
                                                "usernameLower" to cleanUsername,
                                                "displayName" to displayName.trim(),
                                                "email" to email.trim(),
                                                "createdAt" to FieldValue.serverTimestamp()
                                            )
                                            firestore.collection("users").document(uid).set(data)
                                                .addOnSuccessListener {
                                                    loading = false
                                                    onAuthed()
                                                }
                                                .addOnFailureListener { e ->
                                                    loading = false
                                                    errorMsg = e.localizedMessage
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            loading = false
                                            errorMsg = e.localizedMessage
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                errorMsg = e.localizedMessage
                            }
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                loading = false
                                onAuthed()
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                errorMsg = e.localizedMessage
                            }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (loading) "Please wait..." else if (isSignup) "Create account" else "Log in",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = { isSignup = !isSignup; errorMsg = null }) {
                Text(
                    if (isSignup) "Already have an account? Log in" else "New here? Create an account",
                    color = NeonBlue,
                    fontSize = 13.sp
                )
            }
        }
    }
}
