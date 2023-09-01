package com.ugikpoenya.imageeditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

lateinit var currentPhotoFile: File

class ImagePicker {
    fun getIntentGallery(): Intent {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        return Intent.createChooser(intent, "Select Picture")
    }

    fun getIntentCamera(context: Context): Intent {
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        currentPhotoFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(context.packageManager)
        val photoURI: Uri = FileProvider.getUriForFile(
            context,
            "com.ugikpoenya.imageeditor.fileprovider",
            currentPhotoFile
        )
        return takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
    }

}