package com.example.myapplication.view

import android.graphics.Canvas
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.properties.Delegates


class BookView @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attributeSet, defStyle) {


    private var bitmap: Bitmap? = null
    private var bitmapCanvas = Canvas()


    //画第一页的上区域 A
    private val pathAPaint = Paint().apply {
        color = Color.YELLOW
        isAntiAlias = true//设置抗锯齿
    }
    private val pathA = Path()

    //绘制第一页的下区域 C
    private val pathCPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        /**
         * 必须开启这种模式，区域C是ab,bd,dj,ik,ak连接而成的区域 减去 与区域A交集部分后剩余的区域。
         * 这里我们先画了A再画了C，重合部分会减掉并且是A的颜色
         * 所以画C其实把A又画了一遍
         */
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
    }
    private val pathC = Path()

    /**
     * 画出第二页上面的部分B
     */

    private val pathBPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
    }
    private val pathB = Path()

    private val STYLE_TOP_RIGHT = "STYLE_TOP_RIGHT" // f点在右上角
    private val STYLE_LOWER_RIGHT = "STYLE_LOWER_RIGHT" // f点在右下角


    /**
     * 贝塞尔曲线的共计辅助点
     */
    private val a = PointF()
    private var b = PointF()
    private val c = PointF()
    private val d = PointF()
    private val e = PointF()
    private val f = PointF()
    private val g = PointF()
    private val h = PointF()
    private val i = PointF()
    private val j = PointF()
    private var k = PointF()

    init {
        calculatePoints(a, f)
    }

    private var defaultWidth by Delegates.notNull<Float>()
    private var defaultHeight by Delegates.notNull<Float>()

    //屏幕的右下角
    private var viewWidth by Delegates.notNull<Float>()
    private var viewHeight by Delegates.notNull<Float>()

    private var once = true


    private fun setTouchPoint(x: Float, y: Float, style: String) {
        when (style) {
            STYLE_TOP_RIGHT -> {
                f.x = viewWidth
                f.y = 0f
            }

            STYLE_LOWER_RIGHT -> {
                f.x = viewWidth
                f.y = viewHeight
            }
        }
        a.x = x
        a.y = y
        Log.d("ehfoweoifwef", "测试数据:${f}")
        calculatePoints(a, f)
        postInvalidate()
//        val touchPoint = PointF(x, y)
//        if (calcPointCX(touchPoint, f) > 0) {
//            calculatePoints(a, f)
//        } else {
//            calculatePoints1(a, f);
//        }
    }

//    private fun calcPointCX(touchPoint: PointF, f: PointF): Float {
//        val g = PointF()
//        val e = PointF()
//
//        g.x = (touchPoint.x + f.x) / 2;
//        g.y = (touchPoint.y + f.y) / 2;
//
//        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x);
//        e.y = f.y;
//
//        return e.x - (f.x - e.x) / 2;
//
//    }


    /**
     * 回到默认状态
     */
    private fun setDefaultPath() {
        a.x = viewWidth - 1f
        a.y = viewHeight - 1f
        f.x = viewWidth
        f.y = viewHeight
        calculatePoints(a, f)
        postInvalidate()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.y < viewHeight / 2) {
                    setTouchPoint(event.x, event.y, STYLE_TOP_RIGHT)
                } else {
                    setTouchPoint(event.x, event.y, STYLE_LOWER_RIGHT)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                setTouchPoint(event.x, event.y, "")
            }

            MotionEvent.ACTION_UP -> {
                setDefaultPath()
            }
        }
        //这里要消费事件，否则捕捉不到后面的MOVE事件
        return true
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        //获取屏幕的长度和宽度
        viewWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        viewHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        //设置f点的坐标
        f.x = viewWidth
        f.y = viewHeight
        //默认a点的坐标,af如果重合会有特殊情况
        a.x = viewWidth - 1f
        a.y = viewHeight - 1f
        calculatePoints(a, f)
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        /**
         * 动态计算每个点的即时数据
         */
        if (once) {
            bitmap =
                Bitmap.createBitmap(viewWidth.toInt(), viewHeight.toInt(), Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap!!)
            once = false
        }
        //每次绘测前把之前的数据清楚
        bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        /**
         * 分别绘制ABC
         * A有两种情况，一个是f位于右下角一个是f位于右上角
         */
        if (f.x == viewWidth && f.y == 0f) {
            bitmapCanvas.drawPath(getPathAFromTopRight(), pathAPaint);
        }
        if (f.x == viewWidth && f.y == viewHeight) {
            bitmapCanvas.drawPath(getPathAFromBottomRight(), pathAPaint);
        }
        bitmapCanvas.drawPath(getPathC(), pathCPaint);
        bitmapCanvas.drawPath(getPathB(), pathBPaint);
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)

    }

    /**
     * 计算得到pathA
     * f位于右下角
     */
    private fun getPathAFromBottomRight() = run {
        pathA.apply {
            reset()
            lineTo(0f, viewHeight) // 移动到左下角
            lineTo(c.x, c.y) // 移动到c点
            quadTo(e.x, e.y, b.x, b.y) // 从c到b画贝塞尔曲线，控制点为e
            lineTo(a.x, a.y) // 移动到a点
            lineTo(k.x, k.y) // 移动到k点
            quadTo(h.x, h.y, j.x, j.y) // 从k到j画贝塞尔曲线，控制点为h
            lineTo(viewWidth, 0f) // 移动到右上角
            close() // 闭合区域
        }
    }

    /**
     * 获取f点在右上角的pathA
     * @return
     */
    private fun getPathAFromTopRight(): Path {
        pathA.reset()
        pathA.lineTo(c.x, c.y) // 移动到c点
        pathA.quadTo(e.x, e.y, b.x, b.y) // 从c到b画贝塞尔曲线，控制点为e
        pathA.lineTo(a.x, a.y) // 移动到a点
        pathA.lineTo(k.x, k.y) // 移动到k点
        pathA.quadTo(h.x, h.y, j.x, j.y) // 从k到j画贝塞尔曲线，控制点为h
        pathA.lineTo(viewWidth, viewHeight) // 移动到右下角
        pathA.lineTo(0f, viewHeight) // 移动到左下角
        pathA.close()
        return pathA
    }


    /**
     * 得到区域B
     */

    private fun getPathB() = run {
        pathB.apply {
            reset()
            pathB.lineTo(0f, viewHeight)//移动到左下角
            pathB.lineTo(viewWidth, viewHeight)//移动到右下角
            pathB.lineTo(viewWidth, 0f)//移动到右上角
            pathB.close()//闭合区域
        }
    }

    /**
     * 得到区域C
     */
    private fun getPathC() = run {
        pathC.apply {
            reset()
            moveTo(i.x, i.y)//移动到i点
            lineTo(d.x, d.y)//移动到d点
            lineTo(b.x, b.y)//移动到b点
            lineTo(a.x, a.y)//移动到a点
            lineTo(k.x, k.y)//移动到k点
            close()//闭合区域
        }
    }


    /**
     * 计算翻页所用到的点的坐标
     */
    private fun calculatePoints(a: PointF, f: PointF) {
        g.x = (a.x + f.x) / 2
        g.y = (a.y + f.y) / 2

        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x)
        e.y = f.y

        h.x = f.x
        h.y = g.y - (f.x - g.x) * (f.x - g.x) / (f.y - g.y)

        c.x = e.x - (f.x - e.x) / 2
        c.y = f.y

        j.x = f.x
        j.y = h.y - (f.y - h.y) / 2

        b = getIntersectionPoint(a, e, c, j)
        k = getIntersectionPoint(a, h, c, j)

        d.x = (c.x + 2 * e.x + b.x) / 4
        d.y = (2 * e.y + c.y + b.y) / 4

        i.x = (j.x + 2 * h.x + k.x) / 4
        i.y = (2 * h.y + j.y + k.y) / 4
    }

    private fun calculatePoints1(a: PointF, f: PointF) {
        g.x = (a.x + f.x) / 2
        g.y = (a.y + f.y) / 2

        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x)
        e.y = f.y

        h.x = f.x
        h.y = g.y - (f.x - g.x) * (f.x - g.x) / (f.y - g.y)

        c.x = 0f
        c.y = viewHeight

        j.x = f.x
        j.y = h.y - (f.y - h.y) / 2

        b = getIntersectionPoint(a, e, c, j)
        k = getIntersectionPoint(a, h, c, j)

        d.x = (c.x + 2 * e.x + b.x) / 4
        d.y = (2 * e.y + c.y + b.y) / 4

        i.x = (j.x + 2 * h.x + k.x) / 4
        i.y = (2 * h.y + j.y + k.y) / 4
    }


    /**
     * 计算两线段相交点坐标
     * @return 返回该点
     */
    private fun getIntersectionPoint(
        m: PointF,
        n: PointF,
        p: PointF,
        q: PointF
    ): PointF {
        val x1: Float = m.x
        val y1: Float = m.y
        val x2: Float = n.x
        val y2: Float = n.y
        val x3: Float = p.x
        val y3: Float = p.y
        val x4: Float = q.x
        val y4: Float = q.y

        val pointX = ((x1 - x2) * (x3 * y4 - x4 * y3) - (x3 - x4) * (x1 * y2 - x2 * y1)) /
                ((x3 - x4) * (y1 - y2) - (x1 - x2) * (y3 - y4))
        val pointY = ((y1 - y2) * (x3 * y4 - x4 * y3) - (x1 * y2 - x2 * y1) * (y3 - y4)) /
                ((y1 - y2) * (x3 - x4) - (x1 - x2) * (y3 - y4))
        return PointF(pointX, pointY)
    }


    /**
     * 位置像素坐标转dp
     */
    private fun <T> T.pixelsToDp(pixels: Int): Int {
        return (pixels / resources.displayMetrics.density).toInt()
    }


}