package com.nanaboison.signonline

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class SignatureView : View {

    private lateinit var mPath: Path
    private lateinit var mPaint: Paint
    private var bgPaint = Paint(Color.WHITE)

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null

    private var curX: Float = 0.toFloat()
    private var curY:Float = 0.toFloat()

    private val TOUCH_TOLERANCE = 4
    private val STROKE_WIDTH = 7

    internal var mSignatureThere = false

    private var canvaDrawColor = Color.BLACK

    constructor(context: Context): super(context) init {
        init()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) init {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defstyle: Int): super(context, attrs, defstyle) init {
        init()
    }

    fun init(){
       // focusable = FOCUSABLE
        mPath = Path()
        mPaint = Paint()
        mPaint.setColor(canvaDrawColor)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = STROKE_WIDTH.toFloat()
    }

    fun clearSignature(): Boolean {
        if (mBitmap != null)
            createFakeMotionEvents()
        if (mCanvas != null) {
            mCanvas!!.drawColor(Color.BLACK)
            mCanvas!!.drawPaint(bgPaint)
            mPath.reset()
            mBitmap?.eraseColor(Color.WHITE)

            invalidate()
        } else {
            return false
        }
        mSignatureThere = false
        return true
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        var bitmapWidth = mBitmap?.getWidth() ?: 0
        var bitmapHeight = mBitmap?.height ?: 0

        if(bitmapWidth>=width && bitmapHeight>=height) return

        if(bitmapWidth < width) bitmapWidth = width

        if(bitmapHeight < height) bitmapHeight = height

        var newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        newBitmap.eraseColor(Color.WHITE)

        var newCanvas = Canvas()
        newCanvas.setBitmap(newBitmap)

        var getBitmap = MainActivity.bitmapImage

        if(mBitmap != null){
            newCanvas.drawBitmap(mBitmap!!, 0.toFloat(), 0.toFloat(), null)
        }else if(getBitmap != null && MainActivity.signature){
            newCanvas.drawBitmap(getBitmap, 0.toFloat(), 0.toFloat(), null)
        }

        mBitmap = newBitmap
        mCanvas = newCanvas
    }

    fun getImage(): Bitmap? {
        return if (mSignatureThere) {
            this.mBitmap
        } else {
            null
        }
    }

    fun drawCanvas(canvas: Canvas){
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mPaint)

    }

    fun setImage(bitmap: Bitmap) {
        this.mBitmap = bitmap
        this.invalidate()
    }

    private fun createFakeMotionEvents() {
        val downEvent = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 100,
            MotionEvent.ACTION_DOWN,
            1f,
            1f,
            0
        )
        val upEvent = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 100,
            MotionEvent.ACTION_UP,
            1f,
            1f,
            0
        )
        onTouchEvent(downEvent)
        onTouchEvent(upEvent)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mPaint)
        canvas.drawPath(mPath, mPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchDown(x, y)
            MotionEvent.ACTION_MOVE -> touchMove(x, y)
            MotionEvent.ACTION_UP -> touchUp()
        }
        invalidate()
        return true
    }

    /**----------------------------------------------------------
     * Private methods
     * --------------------------------------------------------- */

    private fun touchDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        curX = x
        curY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - curX)
        val dy = Math.abs(y - curY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(curX, curY, (x + curX) / 2, (y + curY) / 2)
            curX = x
            curY = y
        }

        mSignatureThere = true
    }

    private fun touchUp() {
        mPath.lineTo(curX, curY)
        if (mCanvas == null) {
            mCanvas = Canvas()
            mCanvas!!.setBitmap(mBitmap)
        }
        mCanvas!!.drawPath(mPath, mPaint)
        mPath.reset()
    }
}