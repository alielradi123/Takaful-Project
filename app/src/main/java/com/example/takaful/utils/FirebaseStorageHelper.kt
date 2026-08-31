package com.example.takaful.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object FirebaseStorageHelper {
    /**
     * Uploads an image from a local Uri to Firebase Storage at the specified path
     * and returns the HTTP download URL on success.
     */
    fun uploadImage(
        uri: Uri,
        path: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference.child(path)
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Saves a Uri locally to the app's files directory under the user's ID
     * and returns the absolute path on success.
     */
    fun saveImageLocally(context: android.content.Context, uri: Uri, userId: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = java.io.File(context.filesDir, "profile_picture_$userId.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
