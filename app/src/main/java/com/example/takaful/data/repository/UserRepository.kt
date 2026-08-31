package com.example.takaful.data.repository

import android.content.Context
import android.net.Uri
import com.example.takaful.data.model.UserProfile
import com.example.takaful.utils.SupabaseStorageHelper   // ✅ Supabase بدلاً من Firebase Storage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Timestamp
import com.example.takaful.utils.SharedPrefsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")
    private var userListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val _photoURL = MutableStateFlow("")
    val photoURL = _photoURL.asStateFlow()

    /**
     * Fetches the current user's profile from Firestore.
     * Falls back to FirebaseAuth display name if Firestore doc doesn't exist.
     */
    fun loadCurrentUserProfile(onComplete: () -> Unit = {}) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        // Refresh and save FCM Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                usersCollection.document(uid).update("fcmToken", task.result).addOnFailureListener {
                    // Ignore, maybe document doesn't exist yet
                }
            }
        }

        var isFirstLoad = true
        userListenerRegistration?.remove()
        
        userListenerRegistration = usersCollection.document(uid).addSnapshotListener { doc, e ->
            if (e != null) {
                if (isFirstLoad) {
                    val fallbackProfile = UserProfile(
                        uid      = uid,
                        name     = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "مستخدم تكافل",
                        email    = currentUser.email ?: ""
                    )
                    _userProfile.value = fallbackProfile
                    onComplete()
                    isFirstLoad = false
                }
                return@addSnapshotListener
            }

            if (doc != null && doc.exists()) {
                val profile = UserProfile.fromFirestore(doc, uid).copy(
                    email = currentUser.email ?: ""
                )
                val finalProfile = if (profile.displayName.isBlank() || profile.displayName == "مستخدم تكافل") {
                    profile.copy(
                        name = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "مستخدم تكافل"
                    )
                } else profile

                _userProfile.value = finalProfile
                _photoURL.value = finalProfile.photoURL
            } else {
                // Create new Firestore document with new schema
                val fallbackProfile = UserProfile(
                    uid      = uid,
                    name     = currentUser.displayName ?: "مستخدم تكافل",
                    email    = currentUser.email ?: "",
                    phone    = currentUser.phoneNumber ?: "",
                    role     = "member"
                )
                _userProfile.value = fallbackProfile

                usersCollection.document(uid).set(
                    fallbackProfile.toFirestoreMap(),
                    SetOptions.merge()
                )
            }
            
            if (isFirstLoad) {
                onComplete()
                isFirstLoad = false
            }
        }
    }

    /**
     * Updates the user's name and phone in Firestore.
     */
    fun updateProfile(
        name: String,
        phone: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name"     to name,
            "phone"    to phone
        )

        usersCollection.document(uid).update(updates)
            .addOnSuccessListener {
                _userProfile.value = _userProfile.value.copy(name = name, phone = phone)
                onSuccess()
            }
            .addOnFailureListener {
                usersCollection.document(uid).set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        _userProfile.value = _userProfile.value.copy(name = name, phone = phone)
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
    }

    /**
     * Updates the user's role.
     */
    fun updateRole(
        role: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid).update(
            mapOf("role" to role)
        ).addOnSuccessListener {
            _userProfile.value = _userProfile.value.copy(role = role)
            onSuccess()
        }.addOnFailureListener { onFailure(it) }
    }

    /**
     * Uploads a new profile picture to Supabase Storage and updates Firestore with the public URL.
     */
    fun updateProfilePicture(
        context: Context,
        uri: Uri,
        localPath: String?,
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val path = "profile_images/$uid.jpg"

        SupabaseStorageHelper.uploadImage(
            context  = context,
            uri      = uri,
            path     = path,
            onSuccess = { publicUrl ->
                usersCollection.document(uid).update(
                    mapOf("photoURL" to publicUrl)
                ).addOnSuccessListener {
                    _photoURL.value = publicUrl
                    _userProfile.value = _userProfile.value.copy(photoURL = publicUrl)
                    onSuccess(publicUrl)
                }.addOnFailureListener {
                    // Firestore update failed but image is uploaded — still return URL
                    _photoURL.value = publicUrl
                    onFailure(it)
                }
            },
            onFailure = { e -> onFailure(e) }
        )
    }

    /**
     * Deletes the profile picture from Firestore and local storage.
     */
    fun deleteProfilePicture(
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        
        usersCollection.document(uid).update(
            mapOf("photoURL" to "")
        ).addOnSuccessListener {
            _photoURL.value = ""
            _userProfile.value = _userProfile.value.copy(photoURL = "")
            
            // Clear local cache if exists
            val prefs = SharedPrefsHelper(context)
            prefs.profileImagePath = null
            
            onSuccess()
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    fun updateAvailability(
        isAvailable: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid).update(
            mapOf("isAvailable" to isAvailable)
        ).addOnSuccessListener {
            _userProfile.value = _userProfile.value.copy(isAvailable = isAvailable)
            onSuccess()
        }.addOnFailureListener { e -> onFailure(e) }
    }

    fun clearProfile() {
        userListenerRegistration?.remove()
        userListenerRegistration = null
        _userProfile.value = UserProfile()
        _photoURL.value = ""
    }
}
