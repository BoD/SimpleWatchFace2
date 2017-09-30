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
package org.jraf.android.simplewatchface2

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
import android.widget.Toast
import java.util.Calendar
import java.util.TimeZone

class SimpleWatchFaceService : CanvasWatchFaceService() {

    companion object {
//        private const val HAND_WIDTH_HOUR = 9f
//        private const val HAND_WIDTH_MINUTE = 5f
//        private const val HAND_WIDTH_SECOND = 3f
//        private const val TICK_WIDTH = 5f

        private const val HAND_WIDTH_HOUR = 10f
        private const val HAND_WIDTH_MINUTE = 6f
        private const val HAND_WIDTH_SECOND = 4f
        private const val TICK_WIDTH = 6f

        private const val HAND_LENGTH_RATIO_HOUR = 1f / 2f + 1f / 8f
        private const val HAND_LENGTH_RATIO_MINUTE = 1f / 2f + 1f / 4f + 1f / 8f
        private const val HAND_LENGTH_RATIO_SECOND = 1f
        private const val TICK_LENGTH_RATIO = 1f / 16f
        private const val CENTER_GAP_LENGTH_RATIO = 1f / 16f

        private const val SHADOW_RADIUS = 5f
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

            mPaintHour.strokeWidth = HAND_WIDTH_HOUR
            mPaintHour.strokeCap = Paint.Cap.ROUND

            mPaintMinute.strokeWidth = HAND_WIDTH_MINUTE
            mPaintMinute.strokeCap = Paint.Cap.ROUND

            mPaintSecond.strokeWidth = HAND_WIDTH_SECOND
            mPaintSecond.strokeCap = Paint.Cap.ROUND

            mPaintTick.strokeWidth = TICK_WIDTH

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

                mPaintHour.setShadowLayer(SHADOW_RADIUS, 0f, 0f, mColorShadow)
                mPaintMinute.setShadowLayer(SHADOW_RADIUS, 0f, 0f, mColorShadow)
                mPaintSecond.setShadowLayer(SHADOW_RADIUS, 0f, 0f, mColorShadow)
                mPaintTick.setShadowLayer(SHADOW_RADIUS, 0f, 0f, mColorShadow)
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

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TAP ->
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(this@SimpleWatchFaceService, R.string.message, Toast.LENGTH_SHORT)
                            .show()
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

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            val innerTickRadius = mCenterX - mTickLength
            val outerTickRadius = mCenterX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mPaintTick)
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            val seconds = mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f
            val secondsRotation = seconds * 6f

//            val minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f
            val minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f + (seconds / 60f) * 6f

            val hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = mCalendar.get(Calendar.HOUR) * 30 + hourHandOffset

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save()

            // Hour
            canvas.rotate(hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - mCenterGapLength,
                    mCenterX,
                    mCenterY - mHandLengthHour,
                    mPaintHour)

            // Minute
            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - mCenterGapLength,
                    mCenterX,
                    mCenterY - mHandLengthMinute,
                    mPaintMinute)

            // Second
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY)
                canvas.drawLine(
                        mCenterX,
                        mCenterY - mCenterGapLength,
                        mCenterX,
                        mCenterY - mHandLengthSecond,
                        mPaintSecond)
            }

//            canvas.drawCircle(
//                    mCenterX,
//                    mCenterY,
//                    mTickLength,
//                    mPaintTick)

            /* Restore the canvas' original orientation. */
            canvas.restore()

        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mTimeZoneReceiverRegistered) {
                return
            }
            mTimeZoneReceiverRegistered = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mTimeZoneReceiverRegistered) {
                return
            }
            mTimeZoneReceiverRegistered = false
            unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeCallbacksAndMessages(null)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(0)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
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
