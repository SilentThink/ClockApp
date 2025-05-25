package com.example.clockapp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.clockapp.R
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ClockView"
        private const val HOURS_IN_CIRCLE = 12
        private const val MINUTES_IN_CIRCLE = 60
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val rect = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var numberRadius = 0f

    // 时钟刻度的长度
    private val hourTickLength = 30f
    private val minuteTickLength = 15f
    private val numberTextSize = 24f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        try {
            super.onSizeChanged(w, h, oldw, oldh)
            centerX = w / 2f
            centerY = h / 2f
            radius = (min(w, h) / 2f * 0.85f).coerceAtLeast(0f) // 留出15%的边距
            numberRadius = radius * 0.85f // 数字位置
            rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            Log.d(TAG, "onSizeChanged: w=$w, h=$h, radius=$radius")
        } catch (e: Exception) {
            Log.e(TAG, "onSizeChanged error: ${e.message}", e)
        }
    }

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions: width=$width, height=$height")
                return
            }
            
            // 绘制背景圆
            paint.apply {
                style = Paint.Style.FILL
                color = ContextCompat.getColor(context, R.color.clock_face)
            }
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 绘制边框
            paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = ContextCompat.getColor(context, R.color.clock_border)
            }
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 绘制刻度和数字
            drawTicksAndNumbers(canvas)

            // 获取当前时间
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR).toFloat()
            val minute = calendar.get(Calendar.MINUTE).toFloat()
            val second = calendar.get(Calendar.SECOND).toFloat()
            val millis = calendar.get(Calendar.MILLISECOND).toFloat()

            // 计算平滑的秒针角度
            val smoothSecond = second + (millis / 1000f)

            // 绘制时针
            drawHand(canvas, ((hour + minute / 60f) * 5f), radius * 0.5f, 8f, 
                ContextCompat.getColor(context, R.color.hour_hand))
            
            // 绘制分针
            drawHand(canvas, minute, radius * 0.7f, 6f, 
                ContextCompat.getColor(context, R.color.minute_hand))
            
            // 绘制秒针
            drawHand(canvas, smoothSecond, radius * 0.8f, 3f, 
                ContextCompat.getColor(context, R.color.second_hand))
            
            // 绘制中心点
            paint.apply {
                style = Paint.Style.FILL
                color = ContextCompat.getColor(context, R.color.center_dot)
            }
            canvas.drawCircle(centerX, centerY, 8f, paint)
            
            // 每16毫秒更新一次视图（约60fps）
            postInvalidateDelayed(16)
        } catch (e: Exception) {
            Log.e(TAG, "onDraw error: ${e.message}", e)
        }
    }

    private fun drawTicksAndNumbers(canvas: Canvas) {
        // 绘制刻度
        for (i in 0 until MINUTES_IN_CIRCLE) {
            val angle = Math.toRadians((i * 6).toDouble()) // 每个刻度6度
            val isHour = i % 5 == 0
            
            if (!isHour) {
                val tickLength = minuteTickLength
                val startRadius = radius - tickLength
                
                val startX = centerX + startRadius * sin(angle).toFloat()
                val startY = centerY - startRadius * cos(angle).toFloat()
                val endX = centerX + radius * sin(angle).toFloat()
                val endY = centerY - radius * cos(angle).toFloat()
                
                paint.apply {
                    strokeWidth = 2f
                    color = ContextCompat.getColor(context, R.color.tick_marks)
                    alpha = 153 // 60% opacity
                }
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }

        // 绘制数字
        paint.apply {
            style = Paint.Style.FILL
            textSize = numberTextSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = ContextCompat.getColor(context, R.color.digital_text)
        }

        for (i in 1..HOURS_IN_CIRCLE) {
            val angle = Math.toRadians((i * 30 - 90).toDouble()) // 每个数字30度，从-90度开始
            val x = centerX + numberRadius * cos(angle).toFloat()
            val y = centerY + numberRadius * sin(angle).toFloat() + numberTextSize/3 // 微调y坐标以居中显示
            canvas.drawText(i.toString(), x, y, paint)
        }
    }

    private fun drawHand(canvas: Canvas, value: Float, length: Float, width: Float, color: Int) {
        try {
            val angle = Math.toRadians((value * 6).toDouble()) - Math.PI / 2
            paint.apply {
                strokeWidth = width
                this.color = color
                style = Paint.Style.STROKE
                setShadowLayer(4f, 0f, 2f, Color.argb(50, 0, 0, 0))
            }
            
            val endX = centerX + length * cos(angle).toFloat()
            val endY = centerY + length * sin(angle).toFloat()
            
            canvas.drawLine(centerX, centerY, endX, endY, paint)
            paint.clearShadowLayer()
        } catch (e: Exception) {
            Log.e(TAG, "drawHand error: ${e.message}", e)
        }
    }
} 