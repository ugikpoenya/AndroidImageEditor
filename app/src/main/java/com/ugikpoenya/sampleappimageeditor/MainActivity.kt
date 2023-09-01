package com.ugikpoenya.sampleappimageeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.ugikpoenya.imageeditor.ImageEditorActivity
import com.ugikpoenya.imageeditor.ImageHolder
import com.ugikpoenya.imageeditor.ImageEraserActivity
import com.ugikpoenya.imageeditor.ImagePicker
import com.ugikpoenya.imageeditor.currentPhotoFile
import com.ugikpoenya.sampleappimageeditor.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    var currentBitmab: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
    }


    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
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
        intent.putStringArrayListExtra("stickers", stickers)
        eraserLauncer.launch(intent)
    }


    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        (applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }


    private var galeryLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val cropImageContractOptions = CropImageContractOptions(uri, CropImageOptions())
            cropImage.launch(cropImageContractOptions)
        }
    }

    private var cameraLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cropImageContractOptions = CropImageContractOptions(Uri.fromFile(currentPhotoFile), CropImageOptions())
            cropImage.launch(cropImageContractOptions)
        }
    }

    private val eraserLauncer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOG", "eraserImage Result")
            currentBitmab = BitmapFactory.decodeByteArray(ImageHolder.getData(), 0, ImageHolder.getData().size)
            binding?.imageView?.setImageBitmap(currentBitmab)
        }
    }
}