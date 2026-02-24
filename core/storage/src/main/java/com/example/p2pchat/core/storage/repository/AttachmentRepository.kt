package com.example.p2pchat.core.storage.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }

    suspend fun saveAttachment(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.readBytes()
            inputStream.close()

            // For MVP, saving plaintext in app-private storage.
            // Android Sandbox protects this file from other apps.
            // Future enhancement: Accept a Cipher or Key to encrypt bytes here.

            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(attachmentDir, fileName)
            file.writeBytes(bytes)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAttachmentFile(filename: String): File {
        return File(attachmentDir, filename)
    }
}
