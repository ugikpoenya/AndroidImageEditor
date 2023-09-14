package com.ugikpoenya.imageeditor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ugikpoenya.imageeditor.databinding.ActivityImageEditorBinding
import com.ugikpoenya.imageeditor.editor.EditingToolsAdapter
import com.ugikpoenya.imageeditor.editor.EmojiBSFragment
import com.ugikpoenya.imageeditor.editor.FilterListener
import com.ugikpoenya.imageeditor.editor.FilterViewAdapter
import com.ugikpoenya.imageeditor.editor.PropertiesBSFragment
import com.ugikpoenya.imageeditor.editor.ShapeBSFragment
import com.ugikpoenya.imageeditor.editor.StickerBSFragment
import com.ugikpoenya.imageeditor.editor.TextEditorDialogFragment
import com.ugikpoenya.imageeditor.editor.ToolType
import com.ugikpoenya.imageeditor.editor.imageTouchLisener
import com.yalantis.ucrop.UCrop
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class ImageEditorActivity : AppCompatActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, EmojiBSFragment.EmojiListener, StickerBSFragment.StickerListener,
    EditingToolsAdapter.OnItemSelected, FilterListener {
    private lateinit var binding: ActivityImageEditorBinding
    var mProgressDialog: ProgressDialog? = null

    lateinit var mPhotoEditor: PhotoEditor
    private lateinit var mPropertiesBSFragment: PropertiesBSFragment
    private lateinit var mShapeBuilder: ShapeBuilder
    private lateinit var mShapeBSFragment: ShapeBSFragment
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private var mIsFilterVisible = false
    private val mConstraintSet = ConstraintSet()

    private lateinit var mEmojiBSFragment: EmojiBSFragment
    private lateinit var mStickerBSFragment: StickerBSFragment

    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initComponent()
    }

    private fun initToolbar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.black)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (mIsFilterVisible) {
            showFilter(false)
            binding.txtCurrentTool.setText(R.string.label_app)
        } else if (!mPhotoEditor.isCacheEmpty) {
            showSaveDialog()
        } else {
            finish()
        }
    }

    private fun initComponent() {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.setMessage("Saving...")
        mProgressDialog?.setCancelable(false)
        mProgressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER)

        binding.imgClose.setOnClickListener {
            onBackPressed()
        }
        binding.imgCamera.setOnClickListener {
            cameraLauncer.launch(ImagePicker().getIntentCamera(this))
        }

        binding.imgUndo.setOnClickListener { mPhotoEditor.undo() }
        binding.imgRedo.setOnClickListener { mPhotoEditor.redo() }
        binding.imgGallery.setOnClickListener {
            galeryLauncer.launch(ImagePicker().getIntentGallery())
        }

        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvConstraintTools.layoutManager = llmTools
        binding.rvConstraintTools.adapter = mEditingToolsAdapter

        mShapeBSFragment = ShapeBSFragment()
        mShapeBSFragment.setPropertiesChangeListener(this)

        mEmojiBSFragment = EmojiBSFragment()
        mEmojiBSFragment.setEmojiListener(this)

        var stickers = intent.getStringArrayListExtra("stickers")
        if (stickers.isNullOrEmpty()) {
            stickers = ArrayList<String>()
            val folders = assets.list("stickers")
            folders?.forEach { folder ->
                val files = assets.list("stickers/$folder")
                files?.forEach { file ->
                    stickers.add("file:///android_asset/stickers/$folder/$file")
                }
            }
        }

        mStickerBSFragment = StickerBSFragment(stickers)
        mStickerBSFragment.setStickerListener(this)

        mPropertiesBSFragment = PropertiesBSFragment()
        mPropertiesBSFragment.setPropertiesChangeListener(this)

        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilterView.layoutManager = llmFilters
        binding.rvFilterView.adapter = mFilterViewAdapter

        binding.imgSave.setOnClickListener { saveImage() }

        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        val layoutParams = binding.imgStickerMainLayout.layoutParams
        if (height > 0) {
            layoutParams.width = height
            layoutParams.height = height
        }
        if (width > 0) {
            layoutParams.width = width
        }

        binding.imgStickerMainLayout.layoutParams = layoutParams
        mPhotoEditor = PhotoEditor.Builder(this, binding.photoEditorView).setPinchTextScalable(pinchTextScalable).build()
        binding.photoEditorView.source.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        mPhotoEditor.setOnPhotoEditorListener(this)

        if (ImageHolder.hasData()) {
            val originalBitmap = BitmapFactory.decodeByteArray(ImageHolder.getData(), 0, ImageHolder.getData().size)
            binding.photoEditorView.source.setImageBitmap(originalBitmap)
        }
    }


    companion object {
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }

    private fun getBitmapFromView(view: View): Bitmap? {
        val createBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(createBitmap)
        val background = view.background
        if (background != null) {
            background.draw(canvas)
        } else {
            canvas.drawColor(0)
        }
        view.draw(canvas)
        return createBitmap
    }

    fun saveImage() {
        mProgressDialog?.show()
        mPhotoEditor.clearHelperBox()
        val bitmap = getBitmapFromView(binding.photoEditorView)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        try {
            byteArrayOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        ImageHolder.setData(byteArray)
        setResult(RESULT_OK)
        mProgressDialog?.dismiss()
        finish()
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


    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOG", "cropImage Result")
            val uri = UCrop.getOutput(result.data!!)
            val bitmap = MediaStore.Images.Media.getBitmap(
                contentResolver, uri
            )
            binding.photoEditorView.source.setImageBitmap(bitmap)
        }
    }


    private val eraserImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LOG", "eraserImage Result")
            mPhotoEditor.clearAllViews()
            val bitmap = BitmapFactory.decodeByteArray(ImageHolder.getData(), 0, ImageHolder.getData().size)
            binding.photoEditorView.source.setImageBitmap(bitmap)
        }
    }

    override fun onToolSelected(toolType: ToolType) {
        handle_WhenEnableAndDisbale_Move(false)

        when (toolType) {
            ToolType.SHAPE -> {
                mPhotoEditor.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor.setShape(mShapeBuilder)
                binding.txtCurrentTool.setText(R.string.label_shape)
                showBottomSheetDialogFragment(mShapeBSFragment)
            }

            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditorListener {
                    override fun onDone(inputText: String, colorCode: Int) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(colorCode)
                        mPhotoEditor.addText(inputText, styleBuilder)
                        binding.txtCurrentTool.setText(R.string.label_text)
                    }
                })
            }

            ToolType.ERASER -> {
                mPhotoEditor.brushEraser()
                binding.txtCurrentTool.setText(R.string.label_eraser_mode)
            }

            ToolType.ERASER_IMAGE -> {
                val bitmap = getBitmapFromView(binding.photoEditorView)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                try {
                    byteArrayOutputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                ImageHolder.setData(byteArray)
                val intent = Intent(applicationContext, ImageEraserActivity::class.java)
                eraserImage.launch(intent)
            }

            ToolType.FILTER -> {
                binding.txtCurrentTool.setText(R.string.label_filter)
                showFilter(true)
            }

            ToolType.EMOJI -> showBottomSheetDialogFragment(mEmojiBSFragment)
            ToolType.STICKER -> showBottomSheetDialogFragment(mStickerBSFragment)
            ToolType.MOVE -> handle_WhenEnableAndDisbale_Move(true)
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeColor(colorCode))
        binding.txtCurrentTool.setText(R.string.label_brush)
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeOpacity(opacity))
        binding.txtCurrentTool.setText(R.string.label_brush)
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeSize(shapeSize.toFloat()))
        binding.txtCurrentTool.setText(R.string.label_brush)
    }

    override fun onShapePicked(shapeType: ShapeType) {
        mPhotoEditor.setShape(mShapeBuilder.withShapeType(shapeType))
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(binding.rootView)

        val rvFilterId: Int = binding.rvFilterView.id

        if (isVisible) {
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }

        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        TransitionManager.beginDelayedTransition(binding.rootView, changeBounds)
        mConstraintSet.applyTo(binding.rootView)
    }

    override fun onFilterSelected(photoFilter: PhotoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter)
    }

    override fun onEmojiClick(emojiUnicode: String) {
        mPhotoEditor.addEmoji(emojiUnicode)
        binding.txtCurrentTool.setText(R.string.label_emoji)
    }

    override fun onStickerClick(bitmap: Bitmap) {
        mPhotoEditor.addImage(bitmap)
        binding.txtCurrentTool.setText(R.string.label_sticker)
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("Save") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("Discard") { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }


    private fun handle_WhenEnableAndDisbale_Move(z: Boolean) {
        if (z) {
            binding.txtCurrentTool.text = "Move"
            mPhotoEditor.setBrushDrawingMode(false)
            binding.photoEditorView.setOnTouchListener(imageTouchLisener(binding.photoEditorView))
        } else {
            binding.photoEditorView.setOnTouchListener(null)
            binding.txtCurrentTool.setText(R.string.label_app)
        }
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            "LOG",
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String, colorCode: Int) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(colorCode)
                mPhotoEditor.editText(rootView!!, inputText, styleBuilder)
                binding.txtCurrentTool.setText(R.string.label_text)
            }
        })
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            "LOG",
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        Log.d("LOG", "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        Log.d("LOG", "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
        Log.d("LOG", "onTouchView() called with: event = [$event]")
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor.undo()
            R.id.imgRedo -> mPhotoEditor.redo()
            R.id.imgSave -> saveImage()
            R.id.imgClose -> onBackPressed()
            R.id.imgCamera -> cameraLauncer.launch(ImagePicker().getIntentCamera(this))
            R.id.imgGallery -> galeryLauncer.launch(ImagePicker().getIntentGallery())
        }
    }
}