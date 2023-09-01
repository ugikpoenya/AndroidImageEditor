package com.ugikpoenya.imageeditor;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ugikpoenya.imageeditor.eraser.BrushImageView;
import com.ugikpoenya.imageeditor.eraser.TouchImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;


public class ImageEraserActivity extends Activity {
    private Bitmap bitmapMaster;
    private BrushImageView brushImageView;
    private Canvas canvasMaster;
    private float currentx;
    private float currenty;
    private Path drawingPath;
    private Bitmap highResolutionOutput;
    private int imageViewHeight;
    private int imageViewWidth;
    private int initialDrawingCount;
    private boolean isImageResized;
    private boolean isMultipleTouchEraser;
    private boolean isOnBitmapTouch;
    private Bitmap lastEditedBitmap;
    private LinearLayout llTopBar;
    private ImageView mClose;
    private ImageView mDone;
    private ImageView mRedo;
    private ImageView mUndo;
    private Point mainViewSize;
    private MediaScannerConnection msConn;
    private Bitmap originalBitmap;
    private Bitmap resizedBitmap;
    private RelativeLayout rlImageViewContainer;
    private SeekBar sbOffset;
    private SeekBar sbWidth;
    private TouchImageView touchImageView;
    private int updatedBrushSize;
    private int initialDrawingCountLimit = 20;
    private int offset = 0;
    private int undoLimit = 10;
    private float brushSize = 70.0f;
    private ArrayList<Path> paths = new ArrayList<>();
    private ArrayList<Path> redoPaths = new ArrayList<>();
    private Vector<Integer> brushSizes = new Vector<>();
    private Vector<Integer> redoBrushSizes = new Vector<>();
    private int MODE = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_eraser);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }
        this.drawingPath = new Path();
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        this.mainViewSize = point;
        defaultDisplay.getSize(point);
        initViews();

        if (ImageHolder.hasData()) {
            this.originalBitmap = BitmapFactory.decodeByteArray(ImageHolder.getData(), this.offset, ImageHolder.getData().length);
        } else {
            finish();
            Toast.makeText(this, "Not Found Image...", Toast.LENGTH_SHORT).show();
        }

        funSetBitMap();
        updateBrush((float) (this.mainViewSize.x / 2), (float) (this.mainViewSize.y / 2));
    }

    public void initViews() {
        this.touchImageView = (TouchImageView) findViewById(R.id.drawingImageView);
        this.brushImageView = (BrushImageView) findViewById(R.id.brushContainingView);
        this.llTopBar = (LinearLayout) findViewById(R.id.ll_top_bar);
        this.rlImageViewContainer = (RelativeLayout) findViewById(R.id.rl_image_view_container);
        this.mUndo = (ImageView) findViewById(R.id.iv_undo);
        this.mRedo = (ImageView) findViewById(R.id.iv_redo);
        this.mDone = (ImageView) findViewById(R.id.iv_done);
        this.mClose = (ImageView) findViewById(R.id.iv_close);
        this.sbOffset = (SeekBar) findViewById(R.id.sb_offset);
        this.sbWidth = (SeekBar) findViewById(R.id.sb_width);
        this.rlImageViewContainer.getLayoutParams().height = this.mainViewSize.y - this.llTopBar.getLayoutParams().height;
        this.imageViewWidth = this.mainViewSize.x;
        this.imageViewHeight = this.rlImageViewContainer.getLayoutParams().height;
        this.mUndo.setOnClickListener(view -> ImageEraserActivity.this.funUndo());
        this.mRedo.setOnClickListener(view -> ImageEraserActivity.this.funRedo());
        this.mDone.setOnClickListener(view -> ImageEraserActivity.this.setBitmapResultAndFinish());
        this.mClose.setOnClickListener(view -> ImageEraserActivity.this.finish());
        this.touchImageView.setOnTouchListener(new OnTouchListner());
        this.sbWidth.setMax(150);
        this.sbWidth.setProgress((int) (this.brushSize - 20.0f));
        this.sbWidth.setOnSeekBarChangeListener(new OnWidthSeekbarChangeListner());
        this.sbOffset.setMax(350);
        this.sbOffset.setProgress(this.offset);
        this.sbOffset.setOnSeekBarChangeListener(new OnOffsetSeekbarChangeListner());
    }

    public void resetPathArrays() {
        this.mUndo.setEnabled(false);
        this.mRedo.setEnabled(false);
        this.paths.clear();
        this.brushSizes.clear();
        this.redoPaths.clear();
        this.redoBrushSizes.clear();
    }

    public void resetRedoPathArrays() {
        this.mRedo.setEnabled(false);
        this.redoPaths.clear();
        this.redoBrushSizes.clear();
    }

    public void funUndo() {
        int size = this.paths.size();
        if (size != 0) {
            if (size == 1) {
                this.mUndo.setEnabled(false);
            }
            int i = size - 1;
            this.redoPaths.add(this.paths.remove(i));
            this.redoBrushSizes.add(this.brushSizes.remove(i));
            if (!this.mRedo.isEnabled()) {
                this.mRedo.setEnabled(true);
            }
            funUpdateCanvas();
        }
    }

    public void funRedo() {
        int size = this.redoPaths.size();
        if (size != 0) {
            if (size == 1) {
                this.mRedo.setEnabled(false);
            }
            int i = size - 1;
            this.paths.add(this.redoPaths.remove(i));
            this.brushSizes.add(this.redoBrushSizes.remove(i));
            if (!this.mUndo.isEnabled()) {
                this.mUndo.setEnabled(true);
            }
            funUpdateCanvas();
        }
    }

    public void funSetBitMap() {
        this.isImageResized = false;
        Bitmap bitmap = this.resizedBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            this.resizedBitmap = null;
        }
        Bitmap bitmap2 = this.bitmapMaster;
        if (bitmap2 != null) {
            bitmap2.recycle();
            this.bitmapMaster = null;
        }
        this.canvasMaster = null;
        Bitmap funResizeBitmapByCanvas = funResizeBitmapByCanvas();
        this.resizedBitmap = funResizeBitmapByCanvas;
        Bitmap copy = funResizeBitmapByCanvas.copy(Bitmap.Config.ARGB_8888, true);
        this.lastEditedBitmap = copy;
        this.bitmapMaster = Bitmap.createBitmap(copy.getWidth(), this.lastEditedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.bitmapMaster);
        this.canvasMaster = canvas;
        canvas.drawBitmap(this.lastEditedBitmap, 0.0f, 0.0f, (Paint) null);
        this.touchImageView.setImageBitmap(this.bitmapMaster);
        resetPathArrays();
        this.touchImageView.setPan(false);
        this.brushImageView.invalidate();
    }

    public void setBitmapResultAndFinish() {
        Bitmap bitmap = ((BitmapDrawable) this.touchImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageHolder.setData(byteArray);
        setResult(RESULT_OK);
        finish();
    }

    public Bitmap funResizeBitmapByCanvas() {
        float f;
        float f2;
        float width = (float) this.originalBitmap.getWidth();
        float height = (float) this.originalBitmap.getHeight();
        if (width > height) {
            int i = this.imageViewWidth;
            f = (float) i;
            f2 = (((float) i) * height) / width;
        } else {
            int i2 = this.imageViewHeight;
            f2 = (float) i2;
            f = (((float) i2) * width) / height;
        }
        if (f > width || f2 > height) {
            return this.originalBitmap;
        }
        Bitmap createBitmap = Bitmap.createBitmap((int) f, (int) f2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        float f3 = f / width;
        Matrix matrix = new Matrix();
        matrix.postTranslate(0.0f, (f2 - (height * f3)) / 2.0f);
        matrix.preScale(f3, f3);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(this.originalBitmap, matrix, paint);
        this.isImageResized = true;
        return createBitmap;
    }


    public void funMoveToPoint(float f, float f2) {
        float imageViewZoom = getImageViewZoom();
        float f3 = f2 - ((float) this.offset);
        if (this.redoPaths.size() > 0) {
            resetRedoPathArrays();
        }
        PointF imageViewTranslation = getImageViewTranslation();
        double d = (double) (f - imageViewTranslation.x);
        double d2 = (double) imageViewZoom;
        Double.isNaN(d);
        Double.isNaN(d2);
        double d3 = (double) (f3 - imageViewTranslation.y);
        Double.isNaN(d3);
        Double.isNaN(d2);
        this.drawingPath.moveTo((float) ((int) ((float) (d / d2))), (float) ((int) ((float) (d3 / d2))));
        this.updatedBrushSize = (int) (this.brushSize / imageViewZoom);
    }


    public void lineTopoint(Bitmap bitmap, float f, float f2) {
        int i = this.initialDrawingCount;
        int i2 = this.initialDrawingCountLimit;
        if (i < i2) {
            int i3 = i + 1;
            this.initialDrawingCount = i3;
            if (i3 == i2) {
                this.isMultipleTouchEraser = true;
            }
        }
        float imageViewZoom = getImageViewZoom();
        float f3 = f2 - ((float) this.offset);
        PointF imageViewTranslation = getImageViewTranslation();
        double d = (double) (f - imageViewTranslation.x);
        double d2 = (double) imageViewZoom;
        Double.isNaN(d);
        Double.isNaN(d2);
        int i4 = (int) ((float) (d / d2));
        double d3 = (double) (f3 - imageViewTranslation.y);
        Double.isNaN(d3);
        Double.isNaN(d2);
        int i5 = (int) ((float) (d3 / d2));
        if (!this.isOnBitmapTouch && i4 > 0 && i4 < bitmap.getWidth() && i5 > 0 && i5 < bitmap.getHeight()) {
            this.isOnBitmapTouch = true;
        }
        this.drawingPath.lineTo((float) i4, (float) i5);
    }


    public void funAddDrawingPathToArrayList() {
        if (this.paths.size() >= this.undoLimit) {
            funUpdateLastEiditedBitmap();
            this.paths.remove(0);
            this.brushSizes.remove(0);
        }
        if (this.paths.size() == 0) {
            this.mUndo.setEnabled(true);
            this.mRedo.setEnabled(false);
        }
        this.brushSizes.add(Integer.valueOf(this.updatedBrushSize));
        this.paths.add(this.drawingPath);
        this.drawingPath = new Path();
    }


    public void funDrawOnTouchMove() {
        Paint paint = new Paint();
        paint.setStrokeWidth((float) this.updatedBrushSize);
        paint.setColor(0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.canvasMaster.drawPath(this.drawingPath, paint);
        this.touchImageView.invalidate();
    }

    public void funUpdateLastEiditedBitmap() {
        Canvas canvas = new Canvas(this.lastEditedBitmap);
        for (int i = 0; i < 1; i++) {
            int intValue = this.brushSizes.get(i).intValue();
            Paint paint = new Paint();
            paint.setColor(0);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            paint.setStrokeWidth((float) intValue);
            canvas.drawPath(this.paths.get(i), paint);
        }
    }

    public void funUpdateCanvas() {
        this.canvasMaster.drawColor(0, PorterDuff.Mode.CLEAR);
        this.canvasMaster.drawBitmap(this.lastEditedBitmap, 0.0f, 0.0f, (Paint) null);
        for (int i = 0; i < this.paths.size(); i++) {
            int intValue = this.brushSizes.get(i).intValue();
            Paint paint = new Paint();
            paint.setColor(0);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            paint.setStrokeWidth((float) intValue);
            this.canvasMaster.drawPath(this.paths.get(i), paint);
        }
        this.touchImageView.invalidate();
    }

    public void updateBrushWidth() {
        this.brushImageView.width = this.brushSize / 2.0f;
        this.brushImageView.invalidate();
    }

    public void updateBrushOffset() {
        this.brushImageView.centery += ((float) this.offset) - this.brushImageView.offset;
        this.brushImageView.offset = (float) this.offset;
        this.brushImageView.invalidate();
    }

    public void updateBrush(float f, float f2) {
        this.brushImageView.offset = (float) this.offset;
        this.brushImageView.centerx = f;
        this.brushImageView.centery = f2;
        this.brushImageView.width = this.brushSize / 2.0f;
        this.brushImageView.invalidate();
    }

    public float getImageViewZoom() {
        return this.touchImageView.getCurrentZoom();
    }

    public PointF getImageViewTranslation() {
        return this.touchImageView.getTransForm();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        funUpdateCanvas();
        Bitmap bitmap = this.lastEditedBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            this.lastEditedBitmap = null;
        }
        Bitmap bitmap2 = this.originalBitmap;
        if (bitmap2 != null) {
            bitmap2.recycle();
            this.originalBitmap = null;
        }
        Bitmap bitmap3 = this.resizedBitmap;
        if (bitmap3 != null) {
            bitmap3.recycle();
            this.resizedBitmap = null;
        }
        Bitmap bitmap4 = this.bitmapMaster;
        if (bitmap4 != null) {
            bitmap4.recycle();
            this.bitmapMaster = null;
        }
        Bitmap bitmap5 = this.highResolutionOutput;
        if (bitmap5 != null) {
            bitmap5.recycle();
            this.highResolutionOutput = null;
        }
    }


    public class OnTouchListner implements View.OnTouchListener {
        OnTouchListner() {
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            if (motionEvent.getPointerCount() != 1 && !ImageEraserActivity.this.isMultipleTouchEraser) {
                if (ImageEraserActivity.this.initialDrawingCount > 0) {
                    ImageEraserActivity.this.funUpdateCanvas();
                    ImageEraserActivity.this.drawingPath.reset();
                    ImageEraserActivity.this.initialDrawingCount = 0;
                }
                ImageEraserActivity.this.touchImageView.onTouchEvent(motionEvent);
                ImageEraserActivity.this.MODE = 2;
            } else if (action == 0) {
                ImageEraserActivity.this.isOnBitmapTouch = false;
                ImageEraserActivity.this.touchImageView.onTouchEvent(motionEvent);
                ImageEraserActivity.this.MODE = 1;
                ImageEraserActivity.this.initialDrawingCount = 0;
                ImageEraserActivity.this.isMultipleTouchEraser = false;
                ImageEraserActivity.this.funMoveToPoint(motionEvent.getX(), motionEvent.getY());
                ImageEraserActivity.this.updateBrush(motionEvent.getX(), motionEvent.getY());
            } else if (action == 2) {
                if (ImageEraserActivity.this.MODE == 1) {
                    ImageEraserActivity.this.currentx = motionEvent.getX();
                    ImageEraserActivity.this.currenty = motionEvent.getY();
                    ImageEraserActivity imageEraseActivity = ImageEraserActivity.this;
                    imageEraseActivity.updateBrush(imageEraseActivity.currentx, ImageEraserActivity.this.currenty);
                    ImageEraserActivity imageEraseActivity2 = ImageEraserActivity.this;
                    imageEraseActivity2.lineTopoint(imageEraseActivity2.bitmapMaster, ImageEraserActivity.this.currentx, ImageEraserActivity.this.currenty);
                    ImageEraserActivity.this.funDrawOnTouchMove();
                }
            } else if (action == 1 || action == 6) {
                if (ImageEraserActivity.this.MODE == 1 && ImageEraserActivity.this.isOnBitmapTouch) {
                    ImageEraserActivity.this.funAddDrawingPathToArrayList();
                }
                ImageEraserActivity.this.isMultipleTouchEraser = false;
                ImageEraserActivity.this.initialDrawingCount = 0;
                ImageEraserActivity.this.MODE = 0;
            }
            if (action == 1 || action == 6) {
                ImageEraserActivity.this.MODE = 0;
            }
            return true;
        }
    }


    public class OnWidthSeekbarChangeListner implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        OnWidthSeekbarChangeListner() {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            ImageEraserActivity.this.brushSize = ((float) i) + 20.0f;
            ImageEraserActivity.this.updateBrushWidth();
        }
    }


    public class OnOffsetSeekbarChangeListner implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        OnOffsetSeekbarChangeListner() {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            ImageEraserActivity.this.offset = i;
            ImageEraserActivity.this.updateBrushOffset();
        }
    }

    private void saveImage() {
        funSetHighResolutionOutput();
        new imageSaveByAsync().execute(new String[0]);
    }

    private void funSetHighResolutionOutput() {
        if (this.isImageResized) {
            Bitmap createBitmap = Bitmap.createBitmap(this.originalBitmap.getWidth(), this.originalBitmap.getHeight(), this.originalBitmap.getConfig());
            Canvas canvas = new Canvas(createBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.argb(255, 255, 255, 255));
            Rect rect = new Rect(0, 0, this.bitmapMaster.getWidth(), this.bitmapMaster.getHeight());
            Rect rect2 = new Rect(0, 0, this.originalBitmap.getWidth(), this.originalBitmap.getHeight());
            canvas.drawRect(rect2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(this.bitmapMaster, rect, rect2, paint);
            this.highResolutionOutput = null;
            this.highResolutionOutput = Bitmap.createBitmap(this.originalBitmap.getWidth(), this.originalBitmap.getHeight(), this.originalBitmap.getConfig());
            Canvas canvas2 = new Canvas(this.highResolutionOutput);
            canvas2.drawBitmap(this.originalBitmap, 0.0f, 0.0f, (Paint) null);
            Paint paint2 = new Paint();
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas2.drawBitmap(createBitmap, 0.0f, 0.0f, paint2);
            if (createBitmap != null && !createBitmap.isRecycled()) {
                createBitmap.recycle();
                return;
            }
            return;
        }
        this.highResolutionOutput = null;
        Bitmap bitmap = this.bitmapMaster;
        this.highResolutionOutput = bitmap.copy(bitmap.getConfig(), true);
    }


    private class imageSaveByAsync extends AsyncTask<String, Void, Boolean> {
        private imageSaveByAsync() {
        }

        @Override
        protected void onPreExecute() {
            ImageEraserActivity.this.getWindow().setFlags(16, 16);
        }


        public Boolean doInBackground(String... strArr) {
            try {
                ImageEraserActivity imageEraseActivity = ImageEraserActivity.this;
                imageEraseActivity.savePhoto(imageEraseActivity.highResolutionOutput);
                return true;
            } catch (Exception unused) {
                return false;
            }
        }


        public void onPostExecute(Boolean bool) {
            Toast makeText = Toast.makeText(ImageEraserActivity.this.getBaseContext(), "PNG Saved", Toast.LENGTH_LONG);
            makeText.setGravity(17, 0, 0);
            makeText.show();
            ImageEraserActivity.this.getWindow().clearFlags(16);
        }
    }

    public void savePhoto(Bitmap bitmap) {
        File file = new File(Environment.getExternalStorageDirectory(), "ImageEraser");
        file.mkdir();
        Calendar instance = Calendar.getInstance();
        String str = String.valueOf(instance.get(2)) + String.valueOf(instance.get(5)) + String.valueOf(instance.get(1)) + String.valueOf(instance.get(11)) + String.valueOf(instance.get(12)) + String.valueOf(instance.get(13));
        File file2 = new File(file, str.toString() + ".png");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        funScanPhoto(file2.toString());
    }

    public void funScanPhoto(String str) {
        MediaScannerConnection mediaScannerConnection = new MediaScannerConnection(this, new ScanPhotoConnection(str));
        this.msConn = mediaScannerConnection;
        mediaScannerConnection.connect();
    }


    public class ScanPhotoConnection implements MediaScannerConnection.MediaScannerConnectionClient {
        final String val$imageFileName;

        ScanPhotoConnection(String str) {
            this.val$imageFileName = str;
        }

        @Override
        public void onMediaScannerConnected() {
            ImageEraserActivity.this.msConn.scanFile(this.val$imageFileName, null);
        }

        @Override
        public void onScanCompleted(String str, Uri uri) {
            ImageEraserActivity.this.msConn.disconnect();
        }
    }
}