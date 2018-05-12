/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2017 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.simplewatchface2.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.view.WindowInsets
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.prefs.Configuration
import org.jraf.android.simplewatchface2.prefs.ConfigurationConstants
import org.jraf.android.simplewatchface2.prefs.ConfigurationPrefs
import java.util.Calendar
import java.util.TimeZone

class SimpleWatchFaceService : CanvasWatchFaceService() {

    companion object {
        private const val HAND_LENGTH_RATIO_HOUR = 1f / 2f + 1f / 8f
        private const val HAND_LENGTH_RATIO_MINUTE = 1f / 2f + 1f / 4f + 1f / 8f
        private const val HAND_LENGTH_RATIO_SECOND = 1f
        private const val TICK_MAJOR_LENGTH_RATIO = 1f / 7f
        private const val TICK_MINOR_LENGTH_RATIO = 1f / 16f
        private const val DOT_MAJOR_RADIUS_RATIO = 1f / 18f
        private const val DOT_MINOR_RADIUS_RATIO = 1f / 24f
        private const val NUMBER_MAJOR_SIZE_RATIO = 1f / 3f
        private const val NUMBER_MINOR_SIZE_RATIO = 1f / 5f
        private const val CENTER_GAP_LENGTH_RATIO = 1f / 32f
    }

    inner class SimpleWatchFaceEngine : CanvasWatchFaceService.Engine() {
        private val prefs by lazy { ConfigurationPrefs.get(this@SimpleWatchFaceService) }

        private val updateTimeHandler = EngineHandler(this)

        private var calendar: Calendar = Calendar.getInstance()

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }
        private var timeZoneReceiverRegistered = false

        private var centerX = 0f
        private var centerY = 0f

        private var handWidthHour = 0f
        private var handWidthMinute = 0f
        private var handWidthSecond = 0f
        private var shadowRadius = 0f

        private var handLengthHour = 0f
        private var handLengthSecond = 0f
        private var handLengthMinute = 0f
        private var tickMinorLength = 0f
        private var tickMajorLength = 0f
        private var dotMinorRadius = 0f
        private var dotMajorRadius = 0f
        private var numberMinorSize = 0f
        private var numberMajorSize = 0f
        private var centerGapLength = 0f

        private var dialRadius = 0F

        private var colorBackground: Int = 0
        private var colorHandHour: Int = 0
        private var colorHandMinute: Int = 0
        private var colorHandSecond: Int = 0
        private var colorTick: Int = 0
        private var colorShadow: Int = 0

        private var tickStyle: Configuration.TickStyle = Configuration.TickStyle.valueOf(ConfigurationConstants.DEFAULT_TICK_STYLE)

        private val paintHour = Paint()
        private val paintMinute = Paint()
        private val paintSecond = Paint()
        private val paintTick = Paint()

        private var ambient = false
        private var lowBitAmbient = false
        private var burnInProtection = false
        private var chinHeight: Int = 0

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@SimpleWatchFaceService)
                    .setAcceptsTapEvents(true)
                    .build()
            )

//            mPaintBackground.color = Color.BLACK
//            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg)

            colorShadow = 0x80000000.toInt()
            loadPrefs()

            handWidthHour = resources.getDimensionPixelSize(R.dimen.hand_width_hour).toFloat()
            handWidthMinute = resources.getDimensionPixelSize(R.dimen.hand_width_minute).toFloat()
            handWidthSecond = resources.getDimensionPixelSize(R.dimen.hand_width_second).toFloat()
            shadowRadius = resources.getDimensionPixelSize(R.dimen.shadow_radius).toFloat()

            paintHour.strokeWidth = handWidthHour
            paintHour.strokeCap = Paint.Cap.ROUND

            paintMinute.strokeWidth = handWidthMinute
            paintMinute.strokeCap = Paint.Cap.ROUND

            paintSecond.strokeWidth = handWidthSecond
            paintSecond.strokeCap = Paint.Cap.ROUND

            paintTick.strokeWidth = resources.getDimensionPixelSize(R.dimen.tick_width).toFloat()
            paintTick.textAlign = Paint.Align.CENTER
            paintTick.typeface = Typeface.createFromAsset(assets, "fonts/Oswald-Medium.ttf")

//            /* Extract colors from background image to improve watchface style. */
//            Palette.from(mBackgroundBitmap).generate { palette ->
//                if (palette != null) {
//                    mColorHandHighlight = palette.getVibrantColor(Color.RED)
//                    mColorHand = palette.getLightVibrantColor(Color.WHITE)
//                    colorShadow = palette.getDarkMutedColor(Color.BLACK)
//                    updateWatchHandStyle()
//                }
//            }

            updateWatchHandStyle()
            prefs.registerOnSharedPreferenceChangeListener(mOnPrefsChanged)
        }

        private fun loadPrefs() {
            colorBackground = prefs.colorBackground
            colorHandHour = prefs.colorHandHour
            colorHandMinute = prefs.colorHandMinute
            colorHandSecond = prefs.colorHandSecond
            colorTick = prefs.colorTicks
            tickStyle = Configuration.TickStyle.valueOf(prefs.tickStyle)
        }

        override fun onDestroy() {
            updateTimeHandler.removeCallbacksAndMessages(null)
            prefs.unregisterOnSharedPreferenceChangeListener(mOnPrefsChanged)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            burnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)
            chinHeight = insets.systemWindowInsetBottom
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updateWatchHandStyle()

            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (ambient) {
                paintHour.color = Color.WHITE
                paintMinute.color = Color.WHITE
//                paintTick.color = colorTick.grayScale()

//                paintHour.isAntiAlias = false
//                paintMinute.isAntiAlias = false
//                paintTick.isAntiAlias = false
                paintHour.isAntiAlias = true
                paintMinute.isAntiAlias = true
                paintTick.isAntiAlias = true

                paintHour.clearShadowLayer()
                paintMinute.clearShadowLayer()
                paintSecond.clearShadowLayer()
                paintTick.clearShadowLayer()
            } else {
                paintHour.color = colorHandHour
                paintMinute.color = colorHandMinute
                paintSecond.color = colorHandSecond
                paintTick.color = colorTick

                paintHour.isAntiAlias = true
                paintMinute.isAntiAlias = true
                paintSecond.isAntiAlias = true
                paintTick.isAntiAlias = true

                paintHour.setShadowLayer(shadowRadius, 0f, 0f, colorShadow)
                paintMinute.setShadowLayer(shadowRadius, 0f, 0f, colorShadow)
                paintSecond.setShadowLayer(shadowRadius, 0f, 0f, colorShadow)
                paintTick.setShadowLayer(shadowRadius, 0f, 0f, colorShadow)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            handLengthHour = centerX * HAND_LENGTH_RATIO_HOUR
            handLengthMinute = centerX * HAND_LENGTH_RATIO_MINUTE
            handLengthSecond = centerX * HAND_LENGTH_RATIO_SECOND
            tickMinorLength = centerX * TICK_MINOR_LENGTH_RATIO
            tickMajorLength = centerX * TICK_MAJOR_LENGTH_RATIO
            dotMinorRadius = centerX * DOT_MINOR_RADIUS_RATIO
            dotMajorRadius = centerX * DOT_MAJOR_RADIUS_RATIO
            numberMinorSize = centerX * NUMBER_MINOR_SIZE_RATIO
            numberMajorSize = centerX * NUMBER_MAJOR_SIZE_RATIO
            centerGapLength = centerX * CENTER_GAP_LENGTH_RATIO

//            /* Scale loaded background image (more efficient) if surface dimensions change. */
//            val scale = width / mBackgroundBitmap.width.toFloat()
//
//            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                    (mBackgroundBitmap.width * scale).toInt(),
//                    (mBackgroundBitmap.height * scale).toInt(), true)

        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TAP -> {
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now - now % 1000

            if (ambient) {
                canvas.drawColor(Color.BLACK)
            } else {
//                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mPaintBackground)
                canvas.drawColor(colorBackground)
            }

            // Ticks
            when (tickStyle) {
                Configuration.TickStyle.DOTS_4 -> drawDots(canvas, 4)
                Configuration.TickStyle.DOTS_12 -> drawDots(canvas, 12)
                Configuration.TickStyle.TICKS_4 -> drawTicks(canvas, 4)
                Configuration.TickStyle.TICKS_12 -> drawTicks(canvas, 12)
                Configuration.TickStyle.NUMBERS_4 -> drawNumbers(canvas, 4)
                Configuration.TickStyle.NUMBERS_12 -> drawNumbers(canvas, 12)

                Configuration.TickStyle.NOTHING -> {
                    // Do nothing
                }
            }

            val seconds = calendar[Calendar.SECOND]
            val secondsRotation = seconds * 6f

            val minutes = calendar[Calendar.MINUTE]
            val minutesRotation = minutes * 6f + (seconds / 60f) * 6f

            val hoursRotation = calendar[Calendar.HOUR] * 30f + (minutes / 60f) * 30f

            canvas.save()

            // Hour
            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawLine(
                centerX,
                centerY - centerGapLength - handWidthHour / 2f,
                centerX,
                centerY - handLengthHour,
                paintHour
            )

            // Minute
            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
            canvas.drawLine(
                centerX,
                centerY - centerGapLength - handWidthMinute / 2f,
                centerX,
                centerY - handLengthMinute,
                paintMinute
            )

            // Second
            if (!ambient) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY)
                canvas.drawLine(
                    centerX,
                    centerY - centerGapLength - handWidthMinute / 2f,
                    centerX,
                    centerY - handLengthSecond + handWidthSecond / 2f,
                    paintSecond
                )
            }

//            canvas.drawCircle(
//                    centerX,
//                    centerY,
//                    tickMinorLength,
//                    paintTick)

            canvas.restore()
        }

        private fun drawDots(canvas: Canvas, num: Int) {
            for (dotIndex in 0 until num) {
                val rotation = (dotIndex.toDouble() * Math.PI * 2.0 / num).toFloat()
                val dotRadius = if (num > 4 && dotIndex % 3 == 0) dotMajorRadius else dotMinorRadius
                val cx = Math.sin(rotation.toDouble()).toFloat() * (centerX - dotRadius) + centerX
                val cy = (-Math.cos(rotation.toDouble())).toFloat() * (centerX - dotRadius) + centerY
                canvas.drawCircle(
                    cx,
                    cy,
                    dotRadius,
                    paintTick
                )
            }
        }

        private fun drawTicks(canvas: Canvas, num: Int) {
            val innerMinorTickRadius = centerX - tickMinorLength
            val innerMajorTickRadius = centerX - tickMajorLength
            val outerTickRadius = centerX
            for (tickIndex in 0 until num) {
                val rotation = (tickIndex.toDouble() * Math.PI * 2.0 / num).toFloat()
                val innerTickRadius = if (num > 4 && tickIndex % 3 == 0) innerMajorTickRadius else innerMinorTickRadius
                val innerX = Math.sin(rotation.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(rotation.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(rotation.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(rotation.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(
                    centerX + innerX,
                    centerY + innerY,
                    centerX + outerX,
                    centerY + outerY,
                    paintTick
                )
            }
        }

        private fun drawNumbers(canvas: Canvas, num: Int) {
            val textBounds = Rect()
            for (numberIndex in 0..11) {
                // Don't draw minor numbers if we only want major ones
                if (num == 4 && numberIndex % 3 != 0) continue

                val text = if (numberIndex == 0) "12" else numberIndex.toString()
                val rotation = (numberIndex.toDouble() * Math.PI * 2.0 / num).toFloat()
                val textSize = if (numberIndex % 3 == 0) numberMajorSize else numberMinorSize
                paintTick.textSize = textSize
                paintTick.getTextBounds(text, 0, text.length, textBounds)
                val textHeight = textBounds.height()
                val textWidth = textBounds.width()

                // Initialize the radius the first time
                // TODO: Reset this if the font changes
                if (dialRadius == 0F) {
                    // Calculate the radius so it
                    val textHalfWidth = textWidth / 2
                    dialRadius = centerX - textHeight / 2 - (centerX - Math.sqrt((centerX * centerX - textHalfWidth * textHalfWidth).toDouble())).toFloat()
                }

                val cx = Math.sin(rotation.toDouble()).toFloat() * dialRadius + centerX
                var cy = (-Math.cos(rotation.toDouble())).toFloat() * dialRadius + centerY

                val diff = (centerY * 2 - chinHeight) - (cy + textHeight / 2)
                if (diff < 0) {
                    cy += diff
                }

//                paintTick.color = Color.rgb(0, 0xFF, 0)
//                canvas.drawCenteredRect(
//                    cx,
//                    cy,
//                    textWidth,
//                    textHeight,
//                    paintTick
//                )
//
//                paintTick.color = Color.rgb(0xFF, 0xFF, 0xFF)
                canvas.drawText(
                    text,
                    cx,
                    cy + textHeight / 2,
                    paintTick
                )
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            updateTimer()
        }

        private fun registerReceiver() {
            if (timeZoneReceiverRegistered) return
            timeZoneReceiverRegistered = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!timeZoneReceiverRegistered) return
            timeZoneReceiverRegistered = false
            unregisterReceiver(timeZoneReceiver)
        }

        private fun updateTimer() {
            updateTimeHandler.removeCallbacksAndMessages(null)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(0)
            }
        }

        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val now = System.currentTimeMillis()
                val delay = 1000 - now % 1000
                updateTimeHandler.sendEmptyMessageDelayed(0, delay)
            }
        }

        private val mOnPrefsChanged: SharedPreferences.OnSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            loadPrefs()
            updateWatchHandStyle()
            updateTimer()
        }
    }

    override fun onCreateEngine() = SimpleWatchFaceEngine()
}

