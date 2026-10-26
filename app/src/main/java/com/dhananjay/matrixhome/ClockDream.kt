package com.dhananjay.matrixhome

import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import kotlin.random.Random

/** Real system screensaver: black screen with a slowly drifting clock (protects the panel from burn-in). */
class ClockDream : DreamService() {
    private val handler = Handler(Looper.getMainLooper())
    private var box: LinearLayout? = null

    private val mover = object : Runnable {
        override fun run() {
            box?.apply {
                translationX = Random.nextInt(-240, 241).toFloat()
                translationY = Random.nextInt(-140, 141).toFloat()
            }
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        val fmt = if (Prefs(this).use24h) "HH:mm" else "h:mm"
        val time = TextClock(this).apply {
            format12Hour = fmt; format24Hour = fmt
            textSize = 96f
            setTextColor(Color.argb(220, 255, 255, 255))
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"; format24Hour = "EEEE, d MMMM"
            textSize = 22f
            setTextColor(Color.argb(150, 255, 255, 255))
            gravity = Gravity.CENTER
        }
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(time); addView(date)
        }
        box = b
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(
            b,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        setContentView(root)
        handler.postDelayed(mover, 60_000)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(mover)
        super.onDetachedFromWindow()
    }
}
