package com.ugikpoenya.imageeditor.editor

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import ja.burhanrashid52.photoeditor.PhotoEditorView


class imageTouchLisener(var mPhotoEditorView: PhotoEditorView) : OnTouchListener {
    private val matrix: Matrix = Matrix()
    private val savedMatrix: Matrix = Matrix()
    private var mode = 0
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1.0f
    private val DRAG = 1
    private val NONE = 0
    private val ZOOM = 2


    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        mPhotoEditorView.source.setScaleType(ImageView.ScaleType.MATRIX)
        val scale: Float
        dumpEvent(motionEvent)
        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(motionEvent.x, motionEvent.y)
                Log.d("TAG", "mode=DRAG")
                mode = DRAG
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                Log.d("TAG", "mode=NONE")
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(motionEvent)
                Log.d("TAG", "oldDist=$oldDist")
                if (oldDist > 5f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, motionEvent)
                    mode = ZOOM
                    Log.d("TAG", "mode=ZOOM")
                }
            }

            MotionEvent.ACTION_MOVE -> if (mode === DRAG) {
                matrix.set(savedMatrix)
                if (view.left >= -392) {
                    matrix.postTranslate(motionEvent.x - start.x, motionEvent.y - start.y)
                }
            } else if (mode === ZOOM) {
                val newDist: Float = spacing(motionEvent)
                Log.d("TAG", "newDist=$newDist")
                if (newDist > 5f) {
                    matrix.set(savedMatrix)
                    scale = newDist / oldDist
                    matrix.postScale(scale, scale, mid.x, mid.y)
                }
            }
        }
        mPhotoEditorView.source.setImageMatrix(matrix)
        return true
    }


    fun spacing(motionEvent: MotionEvent): Float {
        val x = motionEvent.getX(0) - motionEvent.getX(1)
        val y = motionEvent.getY(0) - motionEvent.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }


    fun midPoint(pointF: PointF, motionEvent: MotionEvent) {
        pointF[(motionEvent.getX(0) + motionEvent.getX(1)) / 2.0f] = (motionEvent.getY(0) + motionEvent.getY(1)) / 2.0f
    }


    fun dumpEvent(motionEvent: MotionEvent) {
        val sb = StringBuilder()
        val action = motionEvent.action
        val i = action and 255
        sb.append("event ACTION_")
        sb.append(arrayOf("DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?")[i])
        if (i == 5 || i == 6) {
            sb.append("(pid ")
            sb.append(action shr 8)
            sb.append(")")
        }
        sb.append("[")
        var i2 = 0
        while (i2 < motionEvent.pointerCount) {
            sb.append("#")
            sb.append(i2)
            sb.append("(pid ")
            sb.append(motionEvent.getPointerId(i2))
            sb.append(")=")
            sb.append(motionEvent.getX(i2).toInt())
            sb.append(",")
            sb.append(motionEvent.getY(i2).toInt())
            i2++
            if (i2 < motionEvent.pointerCount) {
                sb.append(";")
            }
        }
        sb.append("]")
    }
}