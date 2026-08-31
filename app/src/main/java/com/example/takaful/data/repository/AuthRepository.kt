package com.example.takaful.data.repository

import android.content.Context
import android.net.Uri
import com.example.takaful.utils.SupabaseStorageHelper
import com.example.takaful.utils.SharedPrefsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser
    val isLoggedIn get() = auth.currentUser != null

    /**
     * Sign in with email and password.
     */
    fun loginWithEmail(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    SharedPrefsHelper(context).saveCredentials(email.trim(), password.trim())
                    onSuccess()
                } else {
                    val exception = task.exception
                    if (exception is com.google.firebase.FirebaseNetworkException) {
                        onFailure("فشل الاتصال بالشبكة. إذا كنت في السودان، يرجى تفعيل VPN وإعادة المحاولة.")
                    } else {
                        onFailure("خطأ في تسجيل الدخول: تحقق من البريد وكلمة المرور.")
                    }
                }
            }
    }

    /**
     * Register a new user and save their profile to Firestore.
     */
    fun register(
        name: String,
        phone: String,
        email: String,
        password: String,
        role: String,
        profileImageUri: Uri?,
        identityType: String?,
        identityNumber: String?,
        identityFrontUri: Uri?,
        identityBackUri: Uri?,
        registrationReason: String?,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Determine user status
                    val status = if (role == "beneficiary" || role == "volunteer") "pending_verification" else "active"

                    val saveUser = { imageUrl: String, idFrontUrl: String, idBackUrl: String ->
                        val userData = hashMapOf(
                            "uid" to userId,
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "role" to role,
                            "photoURL" to imageUrl,
                            "status" to status,
                            "isActive" to true,
                            "paymentMethod" to "",
                            "accountType" to role,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        
                        // Add identity fields if present
                        if (identityType != null) userData["identityType"] = identityType
                        if (identityNumber != null) userData["identityNumber"] = identityNumber
                        if (idFrontUrl.isNotEmpty()) userData["identityFrontURL"] = idFrontUrl
                        if (idBackUrl.isNotEmpty()) userData["identityBackURL"] = idBackUrl
                        if (registrationReason != null) userData["registrationReason"] = registrationReason

                        db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener { 
                                SharedPrefsHelper(context).saveCredentials(email.trim(), password.trim())
                                onSuccess() 
                            }
                            .addOnFailureListener { onFailure("خطأ في حفظ البيانات. حاول مرة أخرى.") }
                    }

                    // A helper function to upload images sequentially
                    fun uploadImages(
                        profUri: Uri?, idFrontUri: Uri?, idBackUri: Uri?,
                        onComplete: (String, String, String) -> Unit
                    ) {
                        var profUrl = ""
                        var frontUrl = ""
                        var backUrl = ""
                        
                        val uploadBack = {
                            if (idBackUri != null) {
                                SupabaseStorageHelper.uploadImage(
                                    context = context, uri = idBackUri, path = "identity_images/${userId}_back.jpg",
                                    onSuccess = { url -> backUrl = url; onComplete(profUrl, frontUrl, backUrl) },
                                    onFailure = { onComplete(profUrl, frontUrl, backUrl) }
                                )
                            } else {
                                onComplete(profUrl, frontUrl, backUrl)
                            }
                        }

                        val uploadFront = {
                            if (idFrontUri != null) {
                                SupabaseStorageHelper.uploadImage(
                                    context = context, uri = idFrontUri, path = "identity_images/${userId}_front.jpg",
                                    onSuccess = { url -> frontUrl = url; uploadBack() },
                                    onFailure = { uploadBack() }
                                )
                            } else {
                                uploadBack()
                            }
                        }

                        if (profUri != null) {
                            val localPath = SupabaseStorageHelper.saveImageLocally(context, profUri, userId)
                            if (localPath != null) {
                                SharedPrefsHelper(context).profileImagePath = localPath
                            }
                            SupabaseStorageHelper.uploadImage(
                                context = context, uri = profUri, path = "profile_images/$userId.jpg",
                                onSuccess = { url -> profUrl = url; uploadFront() },
                                onFailure = { uploadFront() }
                            )
                        } else {
                            uploadFront()
                        }
                    }

                    uploadImages(profileImageUri, identityFrontUri, identityBackUri) { p, f, b ->
                        saveUser(p, f, b)
                    }

                } else {
                    val exception = task.exception
                    if (exception is com.google.firebase.FirebaseNetworkException) {
                        onFailure("فشل الاتصال بالشبكة. إذا كنت في السودان، يرجى تفعيل VPN وإعادة المحاولة.")
                    } else {
                        onFailure("فشل إنشاء الحساب: ${exception?.localizedMessage}")
                    }
                }
            }
    }

    /**
     * Sign in with Google using Credential Manager.
     */
    fun loginWithGoogle(
        context: Context,
        scope: CoroutineScope,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val webClientId = "822889698478-au4mpu2uibqnqddb57ore7824a02vu31.apps.googleusercontent.com"
        val credentialManager = CredentialManager.create(context)

        scope.launch {
            onLoading(true)
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

                auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                    onLoading(false)
                    if (task.isSuccessful) {
                        // Ensure user document exists for Google sign-in users
                        val user = auth.currentUser
                        if (user != null) {
                            db.collection("users").document(user.uid).get()
                                .addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        val userData = hashMapOf(
                                            "uid" to user.uid,
                                            "name" to (user.displayName ?: ""),
                                            "email" to (user.email ?: ""),
                                            "phone" to (user.phoneNumber ?: ""),
                                            "role" to "member",
                                            "accountType" to "member",
                                            "isActive" to true,
                                            "photoURL" to (user.photoUrl?.toString() ?: ""),
                                            "paymentMethod" to "",
                                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                        db.collection("users").document(user.uid).set(userData)
                                    }
                                }
                        }
                        onSuccess()
                    } else {
                        onError("فشل الدخول بجوجل")
                    }
                }
            } catch (e: Exception) {
                onLoading(false)
                onError("تم إلغاء العملية أو لم يتم اختيار حساب")
            }
        }
    }

    /**
     * Send password reset email.
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.localizedMessage ?: "فشل إرسال بريد إعادة التعيين.")
                }
            }
    }

    /**
     * Sign out and clear local data.
     */
    fun logout(context: Context) {
        val prefs = SharedPrefsHelper(context)
        if (!prefs.isBiometricEnabled) {
            prefs.clearCredentials()
        }
        auth.signOut()
    }
}
