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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import org.jraf.android.simplewatchface2.R
import java.util.Calendar
import java.util.TimeZone

class SimpleWatchFaceService : CanvasWatchFaceService() {

    companion object {
        private const val HAND_LENGTH_RATIO_HOUR = 1f / 2f + 1f / 8f
        private const val HAND_LENGTH_RATIO_MINUTE = 1f / 2f + 1f / 4f + 1f / 8f
        private const val HAND_LENGTH_RATIO_SECOND = 1f
        private const val TICK_LENGTH_RATIO = 1f / 16f
        private const val CENTER_GAP_LENGTH_RATIO = 1f / 32f
    }

    inner class SimpleWatchFaceEngine : CanvasWatchFaceService.Engine() {
        private val mUpdateTimeHandler = EngineHandler(this)

        private var mCalendar: Calendar = Calendar.getInstance()

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }
        private var mTimeZoneReceiverRegistered = false

        private var mCenterX = 0f
        private var mCenterY = 0f

        private var mHandWidthHour = 0f
        private var mHandWidthMinute = 0f
        private var mHandWidthSecond = 0f
        private var mShadowRadius = 0f

        private var mHandLengthHour = 0f
        private var mHandLengthSecond = 0f
        private var mHandLengthMinute = 0f
        private var mTickLength = 0f
        private var mCenterGapLength = 0f

        private var mColorHand: Int = 0
        private var mColorHandHighlight: Int = 0
        private var mColorTick: Int = 0
        private var mColorShadow: Int = 0

        private val mPaintBackground = Paint()
        private val mPaintHour = Paint()
        private val mPaintMinute = Paint()
        private val mPaintSecond = Paint()
        private val mPaintTick = Paint()

        private var mBackgroundBitmap: Bitmap? = null

        private var mAmbient = false
        private var mLowBitAmbient = false
        private var mBurnInProtection = false


        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@SimpleWatchFaceService)
                    .setAcceptsTapEvents(true)
                    .build())

            mPaintBackground.color = Color.BLACK
//            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg)

            mColorHand = Color.WHITE
            mColorHandHighlight = Color.RED
            mColorTick = Color.WHITE
            mColorShadow = Color.BLACK

            mHandWidthHour = resources.getDimensionPixelSize(R.dimen.hand_width_hour).toFloat()
            mHandWidthMinute = resources.getDimensionPixelSize(R.dimen.hand_width_minute).toFloat()
            mHandWidthSecond = resources.getDimensionPixelSize(R.dimen.hand_width_second).toFloat()
            mShadowRadius = resources.getDimensionPixelSize(R.dimen.shadow_radius).toFloat()

            mPaintHour.strokeWidth = mHandWidthHour
            mPaintHour.strokeCap = Paint.Cap.ROUND

            mPaintMinute.strokeWidth = mHandWidthMinute
            mPaintMinute.strokeCap = Paint.Cap.ROUND

            mPaintSecond.strokeWidth = mHandWidthSecond
            mPaintSecond.strokeCap = Paint.Cap.ROUND

            mPaintTick.strokeWidth = resources.getDimensionPixelSize(R.dimen.tick_width).toFloat()

//            /* Extract colors from background image to improve watchface style. */
//            Palette.from(mBackgroundBitmap).generate { palette ->
//                if (palette != null) {
//                    mColorHandHighlight = palette.getVibrantColor(Color.RED)
//                    mColorHand = palette.getLightVibrantColor(Color.WHITE)
//                    mColorShadow = palette.getDarkMutedColor(Color.BLACK)
//                    updateWatchHandStyle()
//                }
//            }

            updateWatchHandStyle()
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            updateWatchHandStyle()

            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (mAmbient) {
                mPaintHour.color = Color.WHITE
                mPaintMinute.color = Color.WHITE
                mPaintSecond.color = Color.WHITE
                mPaintTick.color = Color.WHITE

                mPaintHour.isAntiAlias = false
                mPaintMinute.isAntiAlias = false
                mPaintSecond.isAntiAlias = false
                mPaintTick.isAntiAlias = false

                mPaintHour.clearShadowLayer()
                mPaintMinute.clearShadowLayer()
                mPaintSecond.clearShadowLayer()
                mPaintTick.clearShadowLayer()
            } else {
                mPaintHour.color = mColorHand
                mPaintMinute.color = mColorHand
                mPaintSecond.color = mColorHandHighlight
                mPaintTick.color = mColorHand

                mPaintHour.isAntiAlias = true
                mPaintMinute.isAntiAlias = true
                mPaintSecond.isAntiAlias = true
                mPaintTick.isAntiAlias = true

                mPaintHour.setShadowLayer(mShadowRadius, 0f, 0f, mColorShadow)
                mPaintMinute.setShadowLayer(mShadowRadius, 0f, 0f, mColorShadow)
                mPaintSecond.setShadowLayer(mShadowRadius, 0f, 0f, mColorShadow)
                mPaintTick.setShadowLayer(mShadowRadius, 0f, 0f, mColorShadow)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            mCenterX = width / 2f
            mCenterY = height / 2f

            mHandLengthHour = mCenterX * HAND_LENGTH_RATIO_HOUR
            mHandLengthMinute = mCenterX * HAND_LENGTH_RATIO_MINUTE
            mHandLengthSecond = mCenterX * HAND_LENGTH_RATIO_SECOND
            mTickLength = mCenterX * TICK_LENGTH_RATIO
            mCenterGapLength = mCenterX * CENTER_GAP_LENGTH_RATIO

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
            mCalendar.timeInMillis = now - now % 1000

            if (mAmbient) {
                canvas.drawColor(Color.BLACK)
            } else {
//                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mPaintBackground)
                canvas.drawColor(0xFF330000.toInt())
            }

            // Ticks
            val innerTickRadius = mCenterX - mTickLength
            val outerTickRadius = mCenterX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(mCenterX + innerX,
                        mCenterY + innerY,
                        mCenterX + outerX,
                        mCenterY + outerY,
                        mPaintTick)
            }

            val seconds = mCalendar[Calendar.SECOND]
            val secondsRotation = seconds * 6f

            val minutes = mCalendar[Calendar.MINUTE]
            val minutesRotation = minutes * 6f + (seconds / 60f) * 6f

            val hoursRotation = mCalendar[Calendar.HOUR] * 30f + (minutes / 60f) * 30f

            canvas.save()

            // Hour
            canvas.rotate(hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - mCenterGapLength - mHandWidthHour / 2f,
                    mCenterX,
                    mCenterY - mHandLengthHour,
                    mPaintHour)

            // Minute
            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - mCenterGapLength - mHandWidthMinute / 2f,
                    mCenterX,
                    mCenterY - mHandLengthMinute,
                    mPaintMinute)

            // Second
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY)
                canvas.drawLine(
                        mCenterX,
                        mCenterY - mCenterGapLength - mHandWidthMinute / 2f,
                        mCenterX,
                        mCenterY - mHandLengthSecond + mHandWidthSecond / 2f,
                        mPaintSecond)
            }

//            canvas.drawCircle(
//                    mCenterX,
//                    mCenterY,
//                    mTickLength,
//                    mPaintTick)

            canvas.restore()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            updateTimer()
        }

        private fun registerReceiver() {
            if (mTimeZoneReceiverRegistered) return
            mTimeZoneReceiverRegistered = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mTimeZoneReceiverRegistered) return
            mTimeZoneReceiverRegistered = false
            unregisterReceiver(mTimeZoneReceiver)
        }

        private fun updateTimer() {
            mUpdateTimeHandler.removeCallbacksAndMessages(null)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(0)
            }
        }

        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val now = System.currentTimeMillis()
                val delay = 1000 - now % 1000
                mUpdateTimeHandler.sendEmptyMessageDelayed(0, delay)
            }
        }

    }

    override fun onCreateEngine() = SimpleWatchFaceEngine()
}
