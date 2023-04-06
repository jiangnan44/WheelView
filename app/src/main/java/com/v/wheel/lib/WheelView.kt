package com.v.wheel.lib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.v.wheelview.R
import kotlin.math.*

/**
 * Author:v
 * Time:2023/3/30
 * base on [Android-PickerView](https://github.com/Bigkoo/Android-PickerView)
 * todo support rtl
 */
class WheelView : View {
    private var handler: Handler? = null
    private var gestureDetector: GestureDetector? = null
    private var gestureListener: GestureDetector.SimpleOnGestureListener? = null
    private var itemSelectedListener: ((item: WheelItem) -> Unit)? = null
    private lateinit var items: List<WheelItem>


    private var _initPosition = 0
    private var _selectedItem = 0
    private var _preCurrentIndex = 0

    private var _wvHeight = 0
    private var _wvWidth = 0
    private var _radius = 0


    private var _visibleItemCount = 11
    private var _isAlphaGradient = false
    private var _isLoop = false
    private var _label: String? = null

    private var _textSize = 0f
    private var _maxTextWidth = 10
    private var _maxTextHeight = 10
    private var _itemHeight = 10f
    private var _centerTypeface: Typeface? = null
    private var _subTypeface: Typeface? = null

    private var _textColorOut = Color.DKGRAY
    private var _textColorCenter = Color.BLUE
    private var _dividerColor = Color.TRANSPARENT
    private var _showDivider = true
    private var _lineSpacingMultiplier = 1.6f
    private var _contentBias = 0f
    private var _labelBias = 1f

    private var _topTranslate = 0f
    private var _topDividerY = 0f
    private var _bottomDividerY = 0f
    private var _totalScrollY = 0f
    private var _centerContentOffset = 0f


    private lateinit var subTextPaint: Paint
    private lateinit var centerTextPaint: Paint
    private lateinit var dividerPaint: Paint


    private fun initView(context: Context, attrs: AttributeSet?) {
        _textSize = sp2px(15f)

        var centerTextMidWeight = true
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.WheelView, 0, 0)
            _textColorOut = a.getColor(R.styleable.WheelView_subTextColor, Color.DKGRAY)
            _textColorCenter = a.getColor(R.styleable.WheelView_centerTextColor, Color.BLUE)
            _dividerColor = a.getColor(R.styleable.WheelView_dividerColor, Color.LTGRAY)
            _showDivider = a.getBoolean(R.styleable.WheelView_dividerVisible, false)
            _isLoop = a.getBoolean(R.styleable.WheelView_canLoop, false)
            _contentBias = a.getFloat(R.styleable.WheelView_contentBias, 0f)
            _labelBias = a.getFloat(R.styleable.WheelView_labelBias, 1f)
            _label = a.getString(R.styleable.WheelView_label)
            _isAlphaGradient = a.getBoolean(R.styleable.WheelView_subTextGradient, false)
            centerTextMidWeight = a.getBoolean(R.styleable.WheelView_centerTextMidWeight, true)
            _textSize = a.getDimensionPixelOffset(
                R.styleable.WheelView_android_textSize,
                _textSize.toInt()
            ).toFloat()

            val mul = a.getFloat(
                R.styleable.WheelView_android_lineSpacingMultiplier,
                _lineSpacingMultiplier
            )
            setLineSpacingMultiplier(mul)
            val c = a.getInteger(R.styleable.WheelView_visibleItemCount, 11)
            setVisibleItemCount(c)
            a.recycle()
        }

        val density = resources.displayMetrics.density
        _centerContentOffset = if (density < 1) {
            2.4f
        } else if (density >= 1 && density < 2) {
            4.0f
        } else if (density >= 2 && density < 3) {
            6.0f
        } else density * 2.5f
        initLoopView(context)
        initPaints(centerTextMidWeight)
    }

    private fun initLoopView(context: Context) {
        gestureListener = initGestureListener()
        gestureDetector = GestureDetector(context, gestureListener!!).also {
            it.setIsLongpressEnabled(false)
        }

        _totalScrollY = 0f
        _initPosition = -1
        initHandler()
    }

    private fun initPaints(centerTextMidWeight: Boolean) {
        subTextPaint = Paint().apply {
            color = _textColorOut
            isAntiAlias = true
            typeface = _subTypeface
            textSize = _textSize
        }

        centerTextPaint = Paint().apply {
            color = _textColorCenter
            isAntiAlias = true
            typeface = _centerTypeface
            textSize = _textSize
            if (centerTextMidWeight) {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 1.2f
            }
        }

        dividerPaint = Paint().apply {
            color = _dividerColor
            isAntiAlias = true
        }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }


    private fun initHandler() {
        if (handler != null) return
        handler = object : Handler(Looper.getMainLooper()) {
            private var realTotalOffset = 0
            private var realOffset = 0
            private var currentVelocity = 0f


            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    WHAT_SCROLL -> handleScroll(msg)
                    WHAT_FLING -> handleFling(msg)
                }
            }

            private fun handleFling(msg: Message) {
                val velocity = msg.obj
                if (velocity is Float) {
                    currentVelocity = if (velocity > 2000f) {
                        2000f
                    } else if (velocity < -2000f) {
                        -2000f
                    } else velocity
                }

                if (abs(currentVelocity) in 0f..20f) {
                    removeMessages(WHAT_FLING)
                    doSmoothScroll()
                    return
                }

                val dy = (currentVelocity / 100f).toInt()
                _totalScrollY -= dy
                if (!_isLoop) {
                    var top = -_initPosition * _itemHeight
                    var bottom = (items.size - 1 - _initPosition) * _itemHeight
                    val threshold = _itemHeight * 0.25f
                    if (_totalScrollY - threshold < top) {
                        top = _totalScrollY + dy
                    } else if (_totalScrollY + threshold > bottom) {
                        bottom = _totalScrollY + dy
                    }

                    if (_totalScrollY <= top) {
                        currentVelocity = 40f
                        _totalScrollY = top
                    } else if (_totalScrollY >= bottom) {
                        currentVelocity = -40f
                        _totalScrollY = bottom
                    }
                }


                if (currentVelocity < 0f) {
                    currentVelocity += 20f
                } else {
                    currentVelocity -= 20f
                }

                invalidate()
                sendEmptyMessageDelayed(WHAT_FLING, DELAY_FLING)
            }


            private fun handleScroll(msg: Message) {
                val offset = msg.obj
                if (offset is Float) {
                    realTotalOffset = offset.toInt()
                }


                realOffset = (realTotalOffset * 0.1f).toInt()
                if (realOffset == 0) {
                    realOffset = if (realTotalOffset < 0) {
                        -1
                    } else 1
                }

                if (abs(realTotalOffset) <= 1) {
                    removeMessages(WHAT_SCROLL)
                    onItemSelected()
//                    invalidate()
                    return
                } else {
                    _totalScrollY += realOffset
                    if (!_isLoop) {
                        val top = -_initPosition * _itemHeight
                        val bottom = (items.size - 1 - _initPosition) * _itemHeight
                        if (_totalScrollY <= top || _totalScrollY >= bottom) {
                            _totalScrollY -= realOffset
                            removeMessages(WHAT_SCROLL)
                            onItemSelected()
                            return
                        }
                    }
                }

                invalidate()
                realTotalOffset -= realOffset
                sendEmptyMessageDelayed(WHAT_SCROLL, DELAY_SCROLL)
            }
        }
    }


    private fun initGestureListener() = object : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            doFling(velocityY)
            return true
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (noData()) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        _wvWidth = MeasureSpec.getSize(widthMeasureSpec)

        val hm = MeasureSpec.getMode(heightMeasureSpec)
        measureTextWidthHeight()

        val halfCir = _itemHeight * (_visibleItemCount - 1)
        _radius = (halfCir / Math.PI).toInt()
        val nh = _radius.shl(1)

        if (hm == MeasureSpec.EXACTLY) {
            val mh = MeasureSpec.getSize(heightMeasureSpec)
            if (mh < nh) {
                throw IllegalStateException("Insufficient height:$mh ,require:$nh")
            }
            _wvHeight = mh
            _topTranslate = (mh - nh).shr(1).toFloat()
        } else {
            _topTranslate = 0f
            _wvHeight = nh
        }

        measurePositions()
//        Log.d(TAG, "onMeasure measuredW=$_wvWidth,measuredH=$_wvHeight")
        setMeasuredDimension(_wvWidth, _wvHeight)
    }


    private fun measurePositions() {
        _topDividerY = (_wvHeight - _itemHeight) / 2f
        _bottomDividerY = (_wvHeight + _itemHeight) / 2f


        if (_initPosition == -1) {
            _initPosition = if (_isLoop) {
                (items.size + 1).shr(1)
            } else {
                0
            }
//            Log.d(TAG, "initPosition=$_initPosition isLoop=$_isLoop ")
        }
        _preCurrentIndex = _initPosition
    }


    private var previousY = 0f
    private var startTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val noConsume = gestureDetector?.onTouchEvent(event) == false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTime = System.currentTimeMillis()
                cancelHandleJob()
                previousY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = previousY - event.rawY
                previousY = event.rawY
                _totalScrollY += dy
//                Log.d(TAG, "onTouchEvent move dy=$dy previousY=$previousY")

                if (_isLoop) {
                    invalidate()
                    return true
                }

                val top = -_initPosition * _itemHeight
                val bottom = (items.size - 1 - _initPosition) * _itemHeight
                val threshold = 0.25f * _itemHeight
                if ((dy < 0 && _totalScrollY - threshold < top) ||
                    (dy > 0 && _totalScrollY + threshold > bottom)
                ) {
                    _totalScrollY -= dy
                    return true
                } else {
                    invalidate()
                }
            }
            else -> {
                if (noConsume) {
                    if (System.currentTimeMillis() - startTime > 120L) {
                        doSmoothScroll()
                    } else {
                        doClick()
                    }
                }
            }
        }

        return true
    }


    override fun onDraw(canvas: Canvas) {
        if (noData()) return

        updatePreCurrentIndex()

        if (_showDivider) {
            canvas.drawLine(0f, _topDividerY, _wvWidth.toFloat(), _topDividerY, dividerPaint)
            canvas.drawLine(0f, _bottomDividerY, _wvWidth.toFloat(), _bottomDividerY, dividerPaint)
        }

        if (!_label.isNullOrBlank()) {
            val labelX = getLabelStartX(_label!!)
            val labelY =
                _bottomDividerY - (_itemHeight - _maxTextHeight) / 2f - _centerContentOffset
            canvas.drawText(_label!!, labelX, labelY, centerTextPaint)
        }


        val itemOffset = _totalScrollY % _itemHeight
        var count = 0
        while (count < _visibleItemCount) {

            canvas.save()

            val radian = (_itemHeight * count - itemOffset) / _radius
            val angle = (90f - radian / Math.PI * 180f).toFloat()
//            Log.d(TAG, "radian=$radian angle=$angle")
            if (angle > 90f || angle < -90f) {
                canvas.restore()
                count++
                continue
            }

            val (index, content) = getContent(count)

            val offsetCoefficient = (abs(angle) / 90f).pow(2.2f)
            val contentX = getContentStartX(content)
            val translateY = _topTranslate +
                    (_radius - cos(radian) * _radius - sin(radian) * _maxTextHeight / 2f)
            val bottomY = _maxTextHeight + translateY


            canvas.translate(0f, translateY)

            if (_topDividerY in translateY..bottomY) {

                canvas.run {
                    save()
                    clipRect(0f, 0f, _wvWidth.toFloat(), _topDividerY - translateY)
                    scale(1f, (sin(radian) * SCALE_CONTENT))
                    updateOutPainStyle(offsetCoefficient, angle)
                    drawText(content, contentX, _maxTextHeight.toFloat(), subTextPaint)
                    restore()

                    save()
                    clipRect(0f, _topDividerY - translateY, _wvWidth.toFloat(), _itemHeight)
                    scale(1f, sin(radian))
                    drawText(
                        content,
                        contentX,
                        _maxTextHeight - _centerContentOffset,
                        centerTextPaint
                    )
                    restore()
                }
            } else if (_bottomDividerY in translateY..bottomY) {

                canvas.run {
                    save()
                    clipRect(0f, 0f, _wvWidth.toFloat(), _bottomDividerY - translateY)
                    scale(1f, sin(radian))
                    drawText(
                        content,
                        contentX,
                        _maxTextHeight - _centerContentOffset,
                        centerTextPaint
                    )
                    restore()

                    save()
                    clipRect(0f, _bottomDividerY - translateY, _wvWidth.toFloat(), _itemHeight)
                    scale(1f, (sin(radian) * SCALE_CONTENT))
                    updateOutPainStyle(offsetCoefficient, angle)
                    drawText(content, contentX, _maxTextHeight.toFloat(), subTextPaint)
                    restore()
                }
            } else if (translateY >= _topDividerY && bottomY <= _bottomDividerY) {

                val y = _maxTextHeight - _centerContentOffset
                canvas.drawText(content, contentX, y, centerTextPaint)
                _selectedItem = index
//                Log.d(TAG, "onDraw center y=$y selectedItem=$_selectedItem")
            } else {

                canvas.run {
                    save()
                    clipRect(0f, 0f, _wvWidth.toFloat(), _itemHeight)
                    scale(1f, (sin(radian) * SCALE_CONTENT))
                    updateOutPainStyle(offsetCoefficient, angle)
                    drawText(
                        content,
                        contentX,
                        _maxTextHeight.toFloat(),
                        subTextPaint
                    )
                    restore()
                }
            }

            canvas.restore()
            count++
        }

    }

    private fun updatePreCurrentIndex() {
        val change = (_totalScrollY / _itemHeight).toInt()
        _preCurrentIndex = _initPosition + change % items.size

//        Log.d(TAG, "onDraw updatePreCurrentIndex change=$change preCurrentIndex=$_preCurrentIndex")
        if (!_isLoop) {
            if (_preCurrentIndex < 0) {
                _preCurrentIndex = 0
            } else if (_preCurrentIndex > items.size - 1) {
                _preCurrentIndex = items.size - 1
            }
        } else {
            if (_preCurrentIndex < 0) {
                _preCurrentIndex += items.size
            } else if (_preCurrentIndex > items.size - 1) {
                _preCurrentIndex -= items.size
            }
        }
    }


    private fun getContent(counter: Int): Pair<Int, String> {
        val index = _preCurrentIndex - (_visibleItemCount.shr(1) - counter)

        val i: Int
        val t: String
        if (_isLoop) {
            i = if (index < 0) {
                index + items.size
            } else if (index > items.size - 1) {
                index - items.size
            } else {
                index
            }
            t = items[i].text()
        } else {
            if (index < 0) {
                i = index + items.size
                t = ""
            } else if (index > items.size - 1) {
                i = index - items.size
                t = ""
            } else {
                i = index
                t = items[index].text()
            }
        }

//        Log.d(TAG, "getContentAndIndex index=$index t=$t")
        return Pair(i, t)
    }

    private fun updateOutPainStyle(offsetCoefficient: Float, angle: Float) {

        subTextPaint.alpha = if (_isAlphaGradient) {
            ((90f - abs(angle)) / 90f * 255).toInt()
        } else 255
    }

    private val tmpRect = Rect()
    private fun getContentStartX(content: String): Float {
        if (_contentBias > 0f && _contentBias < 1f) {
            return _wvWidth * _contentBias
        }
        tmpRect.setEmpty()
        centerTextPaint.getTextBounds(content, 0, content.length, tmpRect)
        val x = (_wvWidth - tmpRect.width()).shr(1)

        return x.toFloat()
    }

    private fun getLabelStartX(label: String): Float {

//        Log.d(TAG, "getLabelStart labelBias=$_labelBias")
        if (_labelBias > 0f && _labelBias < 1f) {
            return _wvWidth * _labelBias
        }

        return _wvWidth - getTextWidth(centerTextPaint, label)
    }


    private fun getTextWidth(paint: Paint, txt: String?): Float {
        if (txt.isNullOrBlank()) return 0f
        var ret = 0f
        val len = txt.length
        val widths = FloatArray(len)
        paint.getTextWidths(txt, widths)
        for (i in 0 until len) {
            ret += ceil(widths[i])
        }
        return ret
    }


    private fun noData() = !::items.isInitialized || items.isEmpty()


    private fun cancelHandleJob() {
        handler?.run {
            removeMessages(WHAT_SCROLL)
            removeMessages(WHAT_FLING)
        }
    }


    private fun doClick() {
    }


    private fun doSmoothScroll() {
        cancelHandleJob()

        var of = (_totalScrollY % _itemHeight + _itemHeight) % _itemHeight
        of = if (of > _itemHeight / 2f) {
            _itemHeight - of
        } else {
            -of
        }

//        Log.d(TAG, "smoothScroll of=$of ")

        handler?.run {
            val msg = obtainMessage(WHAT_SCROLL, of)
            sendMessageDelayed(msg, DELAY_SCROLL)
        }
    }

    private fun doFling(yVelocity: Float) {
        cancelHandleJob()
        handler?.run {
            val msg = obtainMessage(WHAT_FLING, yVelocity)
            sendMessageDelayed(msg, DELAY_FLING)
        }
    }


    private fun measureTextWidthHeight() {
        for (item in items) {
            val txt = item.text()
            centerTextPaint.getTextBounds(txt, 0, txt.length, tmpRect)
            val txtWidth = tmpRect.width()
            if (txtWidth > _maxTextWidth) {
                _maxTextWidth = txtWidth
            }
            val txtHeight = tmpRect.height()
            if (txtHeight > _maxTextHeight) {
                _maxTextHeight = txtHeight
            }
        }
        _maxTextHeight += 2
        _itemHeight = _lineSpacingMultiplier * _maxTextHeight
    }

    private fun onItemSelected() {
        itemSelectedListener?.let {
            postDelayed({
                it.invoke(items[_selectedItem])
            }, 200L)
        }
    }

    //this may cause bugs on fragment or recyclerView
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler?.let {
            it.removeCallbacksAndMessages(null)
            handler = null
        }
        gestureListener = null
        gestureDetector = null
        itemSelectedListener = null
    }

    //*******************public api start************************

    fun setupStringItems(dataList: List<String>) {
        this.items = dataList.mapIndexed { index, s -> SimpleWheelItem(index, s) }
        invalidate()
    }

    fun setupItems(dataList: List<WheelItem>) {
        this.items = ArrayList(dataList)
        items.sortedBy { it.index() }
        invalidate()
    }

    fun setVisibleItemCount(count: Int) {
        //add first and last item for +2,visible count can only be odd,thus +1
        _visibleItemCount = if (count % 2 == 0) {
            count + 3
        } else {
            count + 2
        }
//        Log.d(TAG, "count=$count setVisibleItemCount =$_visibleItemCount")
    }


    @Suppress("MemberVisibilityCanBePrivate")
    fun setLineSpacingMultiplier(mul: Float) {
        _lineSpacingMultiplier = if (mul < 1.0f) {
            1.0f
        } else if (mul > 4.0f) {
            4.0f
        } else mul
    }


    fun setLabel(label: String) {
        _label = label
    }

    fun setCenterTextFakeBold(fakeMiddleWeight: Boolean) {
        centerTextPaint.apply {
            if (fakeMiddleWeight) {
                typeface = Typeface.DEFAULT
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 1.2f
            } else {
                typeface = _centerTypeface
                strokeWidth = 1.0f
            }
        }
    }

    fun setCenterTextTypeface(font: Typeface) {
        this._centerTypeface = font
        centerTextPaint.typeface = font
    }

    fun setSubTextTypeface(font: Typeface) {
        this._subTypeface = font
        subTextPaint.typeface = font
    }

    /**
     * @param size the textSize in sp
     */
    fun setTextSize(size: Float) {
        if (size > 0f) {
            _textSize = sp2px(size)
            centerTextPaint.textSize = _textSize
            subTextPaint.textSize = _textSize
        }
    }

    fun setCanLoop(loop: Boolean) {
        _isLoop = loop
    }

    fun setCenterTextColor(@ColorInt color: Int) {
        centerTextPaint.color = color
    }

    fun setSubTextColor(@ColorInt color: Int) {
        subTextPaint.color = color
    }

    fun dividerVisible(show: Boolean) {
        _showDivider = show
    }

    fun setCurrentSelect(position: Int) {
        if (noData()) throw IllegalStateException("Please setup data first!")
        if (position < 0 || position >= items.size) {
            throw IllegalStateException("Invalid position!")
        }

        this._selectedItem = position
        this._initPosition = position
//        Log.w(TAG, "setCurrentSelect $_initPosition")
        _totalScrollY = 0f
        invalidate()
    }

    fun subTextAlphaGradient(gradient: Boolean) {
        _isAlphaGradient = gradient
    }


    fun setOnItemSelectedListener(listener: (item: WheelItem) -> Unit) {
        this.itemSelectedListener = listener
    }

    fun currentItem() = items[_selectedItem]


    //*******************public api end************************


    private fun sp2px(sp: Float): Float {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (sp * fontScale + 0.5f)
    }


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView(context, attrs)
    }


    companion object {
        private const val TAG = "WheelView"
        private const val WHAT_FLING = 0x100
        private const val WHAT_SCROLL = 0x101

        private const val DELAY_FLING = 16L
        private const val DELAY_SCROLL = 10L
        private const val SCALE_CONTENT = 0.8f
    }


    interface WheelItem {
        fun text(): String
        fun index(): Int
    }

    data class SimpleWheelItem(private val i: Int, private val t: String) : WheelItem {
        override fun text() = t
        override fun index() = i
    }
}

