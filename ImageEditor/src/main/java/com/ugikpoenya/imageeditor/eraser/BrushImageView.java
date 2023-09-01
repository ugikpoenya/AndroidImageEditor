package com.ugikpoenya.imageeditor.eraser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.appcompat.widget.AppCompatImageView;


public class BrushImageView extends AppCompatImageView {
    int alpga;
    public float centerx;
    public float centery;
    int density;
    DisplayMetrics metrics;
    public float offset;
    public float smallRadious;
    public float width;

    public BrushImageView(Context context) {
        super(context);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.metrics = displayMetrics;
        int i = (int) displayMetrics.density;
        this.density = i;
        this.alpga = 200;
        this.offset = i * 100;
        this.centerx = i * 166;
        this.centery = i * 200;
        this.width = i * 33;
        this.smallRadious = i * 3;
    }

    public BrushImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.metrics = displayMetrics;
        int i = (int) displayMetrics.density;
        this.density = i;
        this.alpga = 200;
        this.offset = i * 100;
        this.centerx = i * 166;
        this.centery = i * 200;
        this.width = i * 33;
        this.smallRadious = i * 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas.getSaveCount() > 1) {
            canvas.restore();
        }
        canvas.save();
        if (this.offset > 0.0f) {
            Paint paint = new Paint();
            paint.setColor(Color.argb(255, 255, 0, 0));
            paint.setAntiAlias(true);
            canvas.drawCircle(this.centerx, this.centery, this.smallRadious, paint);
        }
        Paint paint2 = new Paint();
        paint2.setColor(Color.argb(this.alpga, 255, 0, 0));
        paint2.setAntiAlias(true);
        canvas.drawCircle(this.centerx, this.centery - this.offset, this.width, paint2);
    }
}
