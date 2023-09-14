package com.ugikpoenya.sampleappimageeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ugikpoenya.imageeditor.ImageEditorActivity
import com.ugikpoenya.imageeditor.ImageHolder
import com.ugikpoenya.imageeditor.ImageEraserActivity
import com.ugikpoenya.imageeditor.ImagePicker
import com.ugikpoenya.imageeditor.R
import com.ugikpoenya.imageeditor.currentPhotoFile
import com.ugikpoenya.sampleappimageeditor.databinding.ActivityMainBinding
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    var currentBitmab: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        ImageHolder.setData(null)
    }

    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOG", "cropImage Result")
            val uri = UCrop.getOutput(result.data!!)
            currentBitmab = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    uri
                )
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, uri!!)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(1)
                    decoder.isMutableRequired = true
                }
            }
            binding?.imageView?.setImageBitmap(currentBitmab)
        }
    }

    fun getImageCamera(view: View?) {
        cameraLauncer.launch(ImagePicker().getIntentCamera(this))
    }

    fun getImageGalery(view: View?) {
        galeryLauncer.launch(ImagePicker().getIntentGallery())
    }

    fun showImageEraser(view: View?) {
        if (currentBitmab != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            currentBitmab?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            try {
                byteArrayOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            ImageHolder.setData(byteArray)
            val intent = Intent(applicationContext, ImageEraserActivity::class.java)
            eraserLauncer.launch(intent)
        }
    }

    fun showImageCroper(view: View?) {
        if (currentBitmab != null) {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val croppedFile = File.createTempFile(
                "cropped_${timeStamp}_", /* prefix */
                ".png", /* suffix */
                storageDir /* directory */
            )
            val fOut = FileOutputStream(croppedFile)
            currentBitmab!!.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            fOut.flush()
            fOut.close()
            showCroppImage(Uri.fromFile(croppedFile))
        }
    }

    fun showImageEditor(view: View?) {
        if (currentBitmab != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            currentBitmab?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            try {
                byteArrayOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            ImageHolder.setData(byteArray)
        }

        val stickers = ArrayList<String>()
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392452.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392455.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392459.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392462.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392465.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392467.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392469.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392471.png")
        stickers.add("https://cdn-icons-png.flaticon.com/256/4392/4392522.png")

        val intent = Intent(applicationContext, ImageEditorActivity::class.java)
//        intent.putExtra("height", getScreenWidth())
//        intent.putExtra("width", getScreenWidth())
//        intent.putStringArrayListExtra("stickers", stickers)
        eraserLauncer.launch(intent)
    }


    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        (applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }


    private var galeryLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showCroppImage(result.data?.data)
        }
    }

    private var cameraLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showCroppImage(Uri.fromFile(currentPhotoFile))
        }
    }

    private val eraserLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOG", "eraserImage Result")
            currentBitmab = BitmapFactory.decodeByteArray(ImageHolder.getData(), 0, ImageHolder.getData().size)
            binding?.imageView?.setImageBitmap(currentBitmab)
        }
    }


    private fun showCroppImage(imageUri: Uri?) {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val croppedFile = File.createTempFile(
            "cropped_${timeStamp}_", /* prefix */
            ".png", /* suffix */
            storageDir /* directory */
        )
        val options = UCrop.Options()
        options.setCompressionFormat(Bitmap.CompressFormat.PNG)
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
        if (imageUri != null) {
            val cropIntent = UCrop.of(imageUri, Uri.fromFile(croppedFile))
                .withOptions(options)
                .getIntent(this)
            cropImage.launch(cropIntent)
        }
    }
}