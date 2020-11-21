/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2017-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.SystemProviders
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.core.util.set
import androidx.core.util.valueIterator
import androidx.palette.graphics.Palette
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.prefs.WatchfacePrefs
import org.jraf.android.simplewatchface2.util.getBitmapFromDrawable
import org.jraf.android.simplewatchface2.util.saturated
import org.jraf.android.simplewatchface2.util.tinted
import org.jraf.android.simplewatchface2.util.withShadow
import org.jraf.android.util.log.Log
import java.util.Calendar
import java.util.TimeZone

class SimpleWatchFaceService : CanvasWatchFaceService() {

    companion object {
        const val COMPLICATION_ID_LEFT = 1
        const val COMPLICATION_ID_TOP = 2
        const val COMPLICATION_ID_RIGHT = 3
        const val COMPLICATION_ID_BOTTOM = 4
        const val COMPLICATION_ID_BACKGROUND = 5

        private const val TICK_MAJOR_LENGTH_RATIO = 1F / 7F
        private const val TICK_MINOR_LENGTH_RATIO = 1F / 16F
        private const val DOT_MAJOR_RADIUS_RATIO = 1F / 18F
        private const val DOT_MINOR_RADIUS_RATIO = 1F / 24F
        private const val NUMBER_MAJOR_SIZE_RATIO = 1F / 4F
        private const val NUMBER_MINOR_SIZE_RATIO = 1F / 6F
        private const val CENTER_GAP_LENGTH_RATIO = 1F / 32F
        private const val COMPLICATION_SMALL_WIDTH_RATIO = 1F / 1.8F
        private const val COMPLICATION_BIG_WIDTH_RATIO = 1.3F
        private const val COMPLICATION_BIG_HEIGHT_RATIO = 1F / 2.25F

        private val COMPLICATION_IDS = intArrayOf(
            COMPLICATION_ID_LEFT,
            COMPLICATION_ID_TOP,
            COMPLICATION_ID_RIGHT,
            COMPLICATION_ID_BOTTOM,
            COMPLICATION_ID_BACKGROUND
        )
    }

    private enum class ComplicationSize {
        SMALL,
        BIG;

        companion object {
            fun fromComplicationType(complicationType: Int) = when (complicationType) {
                ComplicationData.TYPE_LARGE_IMAGE, ComplicationData.TYPE_LONG_TEXT -> BIG
                else -> SMALL
            }
        }
    }

    inner class SimpleWatchFaceEngine : CanvasWatchFaceService.Engine() {
        private val prefs by lazy { WatchfacePrefs(this@SimpleWatchFaceService) }

        private val updateTimeHandler = EngineHandler(this)

        private var calendar: Calendar = Calendar.getInstance()

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }
        private var timeZoneReceiverRegistered = false

        private var width = 0
        private var height = 0
        private var centerX = 0F
        private var centerY = 0F

        private var shadowRadius = 0F

        private var digitsMarginVertical = 0F

        private var tickMinorLength = 0F
        private var tickMajorLength = 0F
        private var dotMinorRadius = 0F
        private var dotMajorRadius = 0F
        private var numberMinorSize = 0F
        private var numberMajorSize = 0F
        private var centerGapLength = 0F
        private var complicationSmallWidth = 0F
        private var complicationBigWidth = 0F
        private var complicationBigHeight = 0F

        private var dialRadius = 0F

        @ColorInt
        private var colorBackground = 0
        @ColorInt
        private var colorHandHour = 0
        @ColorInt
        private var colorHandMinute = 0
        @ColorInt
        private var colorHandSecond = 0
        @ColorInt
        private var colorDial = 0
        @ColorInt
        private var colorShadow = 0
        @ColorInt
        private var colorComplicationsBase = 0
        @ColorInt
        private var colorComplicationsHighlight = 0

        private var dialStyle: WatchfacePrefs.DialStyle = WatchfacePrefs.DialStyle.valueOf(WatchfacePrefs.DEFAULT_DIAL_STYLE)

        private val paintHour = Paint()
        private val paintMinute = Paint()
        private val paintSecond = Paint()
        private val paintTick = Paint()

        private var ambient = false
        private var lowBitAmbient = false
        private var burnInProtection = false
        private var chinHeight = 0

        private val complicationDrawableById = SparseArray<ComplicationDrawable>(COMPLICATION_IDS.size)
        private val complicationSizeById = SparseArray<ComplicationSize>(COMPLICATION_IDS.size)
        private var hasBackgroundComplication = false

        private lateinit var handHourAmbientBitmap: Bitmap
        private lateinit var handHourActiveBitmap: Bitmap
        private val handHourBitmap inline get() = if (ambient) handHourAmbientBitmap else handHourActiveBitmap

        private lateinit var handMinuteAmbientBitmap: Bitmap
        private lateinit var handMinuteActiveBitmap: Bitmap
        private val handMinuteBitmap inline get() = if (ambient) handMinuteAmbientBitmap else handMinuteActiveBitmap

        private lateinit var handSecondBitmap: Bitmap

        private val shouldTimerBeRunning get() = isVisible && !ambient

        private val handHourSourceBitmap by lazy {
            getBitmapFromDrawable(this@SimpleWatchFaceService, R.drawable.hand_hour, width, height)
        }

        private val handMinuteSourceBitmap by lazy {
            getBitmapFromDrawable(this@SimpleWatchFaceService, R.drawable.hand_minute, width, height)
        }

        private val handSecondSourceBitmap by lazy {
            getBitmapFromDrawable(this@SimpleWatchFaceService, R.drawable.hand_second, width, height)
        }

        // TODO: Reset this if the font changes
        private val numberTextBounds: Map<String, Rect> by lazy {
            val res = mutableMapOf<String, Rect>()
            for (numberIndex in 0..23) {
                val textSize = if (numberIndex % 3 == 0) numberMajorSize else numberMinorSize
                paintTick.textSize = textSize
                val textBounds = Rect()
                val text = numberIndex.toString()
                paintTick.getTextBounds(text, 0, text.length, textBounds)
                res[text] = textBounds
            }
            res
        }

        private var useBackgroundPalette: Boolean = true
        private var backgroundPalette: Palette? = null

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@SimpleWatchFaceService)
                    .setAcceptsTapEvents(true)
                    .setHideNotificationIndicator(true)
                    .setHideStatusBar(true)
                    .build()
            )

            loadPrefs()
            colorShadow = 0x80000000.toInt()

            shadowRadius = resources.getDimensionPixelSize(R.dimen.shadow_radius).toFloat()

            digitsMarginVertical = resources.getDimensionPixelSize(R.dimen.digits_margin_vertical).toFloat()

            paintHour.isFilterBitmap = true
            paintMinute.isFilterBitmap = true
            paintSecond.isFilterBitmap = true

            paintTick.strokeWidth = resources.getDimensionPixelSize(R.dimen.tick_width).toFloat()
            paintTick.textAlign = Paint.Align.CENTER
            paintTick.typeface = Typeface.createFromAsset(assets, "fonts/Oswald-Medium.ttf")

            initComplications()
            updateComplicationDrawableColors()
            prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(onPrefsChanged)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            this.width = width
            this.height = height

            centerX = width / 2F
            centerY = height / 2F

            tickMinorLength = centerX * TICK_MINOR_LENGTH_RATIO
            tickMajorLength = centerX * TICK_MAJOR_LENGTH_RATIO
            dotMinorRadius = centerX * DOT_MINOR_RADIUS_RATIO
            dotMajorRadius = centerX * DOT_MAJOR_RADIUS_RATIO
            numberMinorSize = centerX * NUMBER_MINOR_SIZE_RATIO
            numberMajorSize = centerX * NUMBER_MAJOR_SIZE_RATIO
            centerGapLength = centerX * CENTER_GAP_LENGTH_RATIO
            complicationSmallWidth = centerX * COMPLICATION_SMALL_WIDTH_RATIO
            complicationBigWidth = centerX * COMPLICATION_BIG_WIDTH_RATIO
            complicationBigHeight = centerY * COMPLICATION_BIG_HEIGHT_RATIO

            // Complications
            updateComplicationDrawableBounds()

            updatePaints()

            initAmbientBitmaps()

            updateBitmaps()
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)
            chinHeight = insets.systemWindowInsetBottom
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)

            lowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            burnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            // Complications
            for (complicationDrawable in complicationDrawableById.valueIterator()) {
                complicationDrawable.setLowBitAmbient(lowBitAmbient)
                complicationDrawable.setBurnInProtection(burnInProtection)
            }

            updatePaints()
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

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updatePaints()

            // Complications
            for (complicationDrawable in complicationDrawableById.valueIterator()) {
                complicationDrawable.setInAmbientMode(inAmbientMode)
            }
            updateTimer()
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onDestroy() {
            updateTimeHandler.removeCallbacksAndMessages(null)
            prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(onPrefsChanged)
            super.onDestroy()
        }

        private fun updateComplicationDrawableColors() {
            for (complicationId in COMPLICATION_IDS.filterNot { it == COMPLICATION_ID_BACKGROUND }) {
                val complicationDrawable = complicationDrawableById[complicationId]
                val complicationSize = complicationSizeById[complicationId]

                // Active mode
                complicationDrawable.setBorderColorActive(if (complicationSize == ComplicationSize.SMALL) colorComplicationsHighlight else Color.TRANSPARENT)
                complicationDrawable.setRangedValuePrimaryColorActive(colorComplicationsHighlight)
                complicationDrawable.setTextColorActive(colorComplicationsBase)
                complicationDrawable.setTitleColorActive(colorComplicationsBase)
                complicationDrawable.setIconColorActive(colorComplicationsBase)
                complicationDrawable.setTextSizeActive(resources.getDimensionPixelSize(R.dimen.complication_textSize))
                complicationDrawable.setTitleSizeActive(resources.getDimensionPixelSize(R.dimen.complication_titleSize))
                complicationDrawable.setBackgroundColorActive(resources.getColor(R.color.complication_background, null))

                // Ambient mode
                complicationDrawable.setBorderColorAmbient(Color.TRANSPARENT)
                complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE)
                complicationDrawable.setTextColorAmbient(Color.WHITE)
                complicationDrawable.setTitleColorAmbient(Color.WHITE)
                complicationDrawable.setIconColorAmbient(Color.WHITE)
                complicationDrawable.setTextSizeAmbient(resources.getDimensionPixelSize(R.dimen.complication_textSize))
                complicationDrawable.setTitleSizeAmbient(resources.getDimensionPixelSize(R.dimen.complication_titleSize))
                complicationDrawable.setBackgroundColorAmbient(Color.TRANSPARENT)
            }
        }

        private fun loadPrefs() {
            colorBackground = prefs.colorBackground
            colorHandHour = prefs.colorHandHour
            colorHandMinute = prefs.colorHandMinute
            colorHandSecond = prefs.colorHandSecond
            colorDial = prefs.colorDial
            colorComplicationsBase = prefs.colorComplicationsBase
            colorComplicationsHighlight = prefs.colorComplicationsHighlight
            dialStyle = WatchfacePrefs.DialStyle.valueOf(prefs.dialStyle)
            useBackgroundPalette = prefs.colorAuto
        }

        private fun updatePaints() {
            if (!ambient) {
                // Active mode
                paintTick.color = colorDial

                paintHour.isAntiAlias = true
                paintMinute.isAntiAlias = true
                paintSecond.isAntiAlias = true
                paintTick.isAntiAlias = true

                paintTick.setShadowLayer(shadowRadius, 0F, 0F, colorShadow)
            } else {
                // Ambient mode
//                paintTick.color = colorDial.grayScale()
                paintTick.color = colorDial

                paintHour.isAntiAlias = !lowBitAmbient
                paintMinute.isAntiAlias = !lowBitAmbient
                paintTick.isAntiAlias = !lowBitAmbient

                paintTick.clearShadowLayer()
            }
        }

        private fun initAmbientBitmaps() {
            handHourAmbientBitmap = handHourSourceBitmap
                .tinted(Color.WHITE)
                .withShadow(shadowRadius, colorShadow)

            handMinuteAmbientBitmap = handMinuteSourceBitmap
                .tinted(Color.WHITE)
                .withShadow(shadowRadius, colorShadow)
        }

        private fun updateBitmaps() {
            handHourActiveBitmap = handHourSourceBitmap
                .tinted(colorHandHour)
                .withShadow(shadowRadius, colorShadow)

            handMinuteActiveBitmap = handMinuteSourceBitmap
                .tinted(colorHandMinute)
                .withShadow(shadowRadius, colorShadow)

            handSecondBitmap = handSecondSourceBitmap
                .tinted(colorHandSecond)
                .withShadow(shadowRadius, colorShadow)
        }

        private fun updateColorsWithPalette() {
            val backgroundPalette = backgroundPalette
            if (useBackgroundPalette && backgroundPalette != null) {
                val saturatedDominantColor = backgroundPalette.getDominantColor(Color.RED).saturated()
                colorHandHour = Color.WHITE
                colorHandMinute = Color.WHITE
                colorHandSecond = saturatedDominantColor
                colorDial = saturatedDominantColor
                colorComplicationsBase = Color.WHITE
                colorComplicationsHighlight = saturatedDominantColor
            }
        }

        private fun initComplications() {
            val topComplicationDrawable = ComplicationDrawable(this@SimpleWatchFaceService).apply {
                setNoDataText("--")
            }
            complicationDrawableById[COMPLICATION_ID_TOP] = topComplicationDrawable
            complicationSizeById[COMPLICATION_ID_TOP] = ComplicationSize.SMALL
            setDefaultSystemComplicationProvider(COMPLICATION_ID_TOP, SystemProviders.NEXT_EVENT, ComplicationData.TYPE_LONG_TEXT)

            val rightComplicationDrawable = ComplicationDrawable(this@SimpleWatchFaceService).apply {
                setNoDataText("--")
            }
            complicationDrawableById[COMPLICATION_ID_RIGHT] = rightComplicationDrawable
            complicationSizeById[COMPLICATION_ID_RIGHT] = ComplicationSize.SMALL
            setDefaultSystemComplicationProvider(COMPLICATION_ID_RIGHT, SystemProviders.STEP_COUNT, ComplicationData.TYPE_SHORT_TEXT)

            val bottomComplicationDrawable = ComplicationDrawable(this@SimpleWatchFaceService).apply {
                setNoDataText("--")
            }
            complicationDrawableById[COMPLICATION_ID_BOTTOM] = bottomComplicationDrawable
            complicationSizeById[COMPLICATION_ID_BOTTOM] = ComplicationSize.SMALL
            setDefaultSystemComplicationProvider(COMPLICATION_ID_BOTTOM, SystemProviders.DATE, ComplicationData.TYPE_LONG_TEXT)

            val leftComplicationDrawable = ComplicationDrawable(this@SimpleWatchFaceService).apply {
                setNoDataText("--")
            }
            complicationDrawableById[COMPLICATION_ID_LEFT] = leftComplicationDrawable
            complicationSizeById[COMPLICATION_ID_LEFT] = ComplicationSize.SMALL
            setDefaultSystemComplicationProvider(COMPLICATION_ID_LEFT, SystemProviders.DAY_OF_WEEK, ComplicationData.TYPE_SHORT_TEXT)

            val backgroundComplicationDrawable = ComplicationDrawable(this@SimpleWatchFaceService).apply {
                setNoDataText("--")
            }
            backgroundComplicationDrawable.setBorderWidthActive(0)
            backgroundComplicationDrawable.setBorderWidthAmbient(0)
            complicationDrawableById[COMPLICATION_ID_BACKGROUND] = backgroundComplicationDrawable

            setActiveComplications(*COMPLICATION_IDS)
        }

        private fun updateComplicationDrawableBounds() {
            val horizMargin = Math.max(numberTextBounds.getValue("3").width(), numberTextBounds.getValue("9").width())
            val vertMargin = Math.max(numberTextBounds.getValue("0").height(), numberTextBounds.getValue("6").height())

            // Left
            val leftCmplWidth = complicationSmallWidth
            val leftCmplLeft = horizMargin / 2 + centerX / 2 - leftCmplWidth / 2
            val leftCmplTop = centerY - leftCmplWidth / 2
            val leftCmplBounds = Rect(
                leftCmplLeft.toInt(),
                leftCmplTop.toInt(),
                (leftCmplLeft + leftCmplWidth).toInt(),
                (leftCmplTop + leftCmplWidth).toInt()
            )
            complicationDrawableById[COMPLICATION_ID_LEFT].bounds = leftCmplBounds

            // Right
            val rightCmplWidth = complicationSmallWidth
            val rightCmplLeft = centerX * 2 - rightCmplWidth - leftCmplLeft
            val rightCmplTop = centerY - rightCmplWidth / 2
            val rightCmplBounds = Rect(
                rightCmplLeft.toInt(),
                rightCmplTop.toInt(),
                (rightCmplLeft + rightCmplWidth).toInt(),
                (rightCmplTop + rightCmplWidth).toInt()
            )
            complicationDrawableById[COMPLICATION_ID_RIGHT].bounds = rightCmplBounds

            // Top
            val topCmplIsSmall = complicationSizeById[COMPLICATION_ID_TOP] == ComplicationSize.SMALL
            val topCmplWidth = if (topCmplIsSmall) complicationSmallWidth else complicationBigWidth
            val topCmplHeight = if (topCmplIsSmall) complicationSmallWidth else complicationBigHeight
            val topCmplLeft = centerX - topCmplWidth / 2
            val topCmplTop = if (topCmplIsSmall) vertMargin / 2 + centerY / 2 - topCmplHeight / 2 else vertMargin / 2 + rightCmplTop / 2 - topCmplHeight / 2
            val topCmplBounds = Rect(
                topCmplLeft.toInt(),
                topCmplTop.toInt(),
                (topCmplLeft + topCmplWidth).toInt(),
                (topCmplTop + topCmplHeight).toInt()
            )
            complicationDrawableById[COMPLICATION_ID_TOP].bounds = topCmplBounds

            // Bottom
            val bottomCmplIsSmall = complicationSizeById[COMPLICATION_ID_BOTTOM] == ComplicationSize.SMALL
            val bottomCmplWidth = if (bottomCmplIsSmall) complicationSmallWidth else complicationBigWidth
            val bottomCmplHeight = if (bottomCmplIsSmall) complicationSmallWidth else complicationBigHeight
            val bottomCmplLeft = centerX - bottomCmplWidth / 2
            val bottomCmplTop =
                centerY * 2 - (if (bottomCmplIsSmall) vertMargin / 2 + centerY / 2 - bottomCmplHeight / 2 else vertMargin / 2 + rightCmplTop / 2 - bottomCmplHeight / 2) - bottomCmplHeight
            val bottomCmplBounds = Rect(
                bottomCmplLeft.toInt(),
                bottomCmplTop.toInt(),
                (bottomCmplLeft + bottomCmplWidth).toInt(),
                (bottomCmplTop + bottomCmplHeight).toInt()
            )
            complicationDrawableById[COMPLICATION_ID_BOTTOM].bounds = bottomCmplBounds

            // Background
            complicationDrawableById[COMPLICATION_ID_BACKGROUND].bounds = Rect(
                0,
                0,
                width,
                height
            )
        }

        override fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData) {
            complicationDrawableById[complicationId].setComplicationData(complicationData)
            complicationSizeById[complicationId] = ComplicationSize.fromComplicationType(complicationData.type)
            updateComplicationDrawableBounds()
            updateComplicationDrawableColors()

            if (complicationId == COMPLICATION_ID_BACKGROUND) {
                Log.d("Received background complication type=${complicationData.type} size=${complicationSizeById[complicationId]}")
                hasBackgroundComplication = complicationData.type !in intArrayOf(
                    ComplicationData.TYPE_EMPTY,
                    ComplicationData.TYPE_NO_PERMISSION,
                    ComplicationData.TYPE_NO_DATA
                )
                if (hasBackgroundComplication) {
                    val drawable = complicationData.largeImage.loadDrawable(this@SimpleWatchFaceService)
                    if (drawable is BitmapDrawable) {
                        backgroundPalette = Palette.from(drawable.bitmap).generate()
                        updateColorsWithPalette()
                        updatePaints()
                        updateBitmaps()
                        updateComplicationDrawableColors()
                        updateTimer()
                    }
                }
            }

            invalidate()
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TAP -> {
                    // Exclude the background complication
                    for (complicationId in COMPLICATION_IDS.filterNot { it == COMPLICATION_ID_BACKGROUND }) {
                        val complicationDrawable = complicationDrawableById.get(complicationId)
                        val successfulTap = complicationDrawable.onTap(x, y)
                        if (successfulTap) return
                    }
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now - now % 1000

            // Background / background complication
            drawBackground(canvas, now)

            // Dial
            when (dialStyle) {
                WatchfacePrefs.DialStyle.DOTS_4 -> drawDots(canvas, 4)
                WatchfacePrefs.DialStyle.DOTS_12 -> drawDots(canvas, 12)
                WatchfacePrefs.DialStyle.TICKS_4 -> drawTicks(canvas, 4)
                WatchfacePrefs.DialStyle.TICKS_12 -> drawTicks(canvas, 12)
                WatchfacePrefs.DialStyle.NUMBERS_4 -> drawNumbers(canvas, 4)
                WatchfacePrefs.DialStyle.NUMBERS_12 -> drawNumbers(canvas, 12)

                WatchfacePrefs.DialStyle.NOTHING -> {
                    // Do nothing
                }
            }

            // Other complications
            drawOtherComplications(canvas, now)

            // Hands
            drawHands(canvas)
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawBackground(canvas: Canvas, currentTimeMillis: Long) {
            if (!ambient) {
                if (hasBackgroundComplication) {
                    // Complication
                    val complicationDrawable = complicationDrawableById.get(COMPLICATION_ID_BACKGROUND)
                    complicationDrawable.draw(canvas, currentTimeMillis)
                } else {
                    // Color
                    canvas.drawColor(colorBackground)
                }
            } else {
                canvas.drawColor(Color.BLACK)
            }
        }


        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawDots(canvas: Canvas, num: Int) {
            for (dotIndex in 0 until num) {
                val rotation = (dotIndex.toDouble() * Math.PI * 2.0 / num).toFloat()
                val dotRadius = if (num > 4 && dotIndex % 3 == 0) dotMajorRadius else dotMinorRadius
                val cx = Math.sin(rotation.toDouble()).toFloat() * (centerX - dotMajorRadius) + centerX
                val cy = (-Math.cos(rotation.toDouble())).toFloat() * (centerX - dotMajorRadius) + centerY
                canvas.drawCircle(
                    cx,
                    cy,
                    dotRadius,
                    paintTick
                )
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawTicks(canvas: Canvas, num: Int) {
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

        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawNumbers(canvas: Canvas, num: Int) {
            for (numberIndex in 0..11) {
                // Don't draw minor numbers if we only want major ones
                if (num == 4 && numberIndex % 3 != 0) continue

                val text = getNumberText(numberIndex)
                val rotation = (numberIndex.toDouble() * Math.PI * 2.0 / num).toFloat()
                val textSize = if (numberIndex % 3 == 0) numberMajorSize else numberMinorSize
                paintTick.textSize = textSize
                val textHeight = numberTextBounds.getValue(text).height()

                // Initialize the radius the first time
                // TODO: Reset this if the font changes
                if (dialRadius == 0F) {
                    // Calculate the radius using a margin
                    dialRadius = centerX - textHeight / 2 - digitsMarginVertical
                }

                val cx = Math.sin(rotation.toDouble()).toFloat() * dialRadius + centerX
                var cy = (-Math.cos(rotation.toDouble())).toFloat() * dialRadius + centerY

                val diff = (centerY * 2 - chinHeight) - (cy + textHeight / 2)
                if (diff < 0) {
                    cy += diff
                }

                canvas.drawText(
                    text,
                    cx,
                    cy + textHeight / 2,
                    paintTick
                )
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun getNumberText(numberIndex: Int): String {
            return if (!prefs.smartNumbers) {
                if (numberIndex == 0) "12" else numberIndex.toString()
            } else {
                val curHour = calendar[Calendar.HOUR_OF_DAY]
                if (curHour < 12) {
                    if (numberIndex < curHour) (numberIndex + 12).toString() else numberIndex.toString()
                } else {
                    if (numberIndex < curHour - 12) numberIndex.toString() else (numberIndex + 12).toString()
                }
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawOtherComplications(canvas: Canvas, currentTimeMillis: Long) {
            if (ambient) {
                // Draw only the top complications
                val complicationDrawable = complicationDrawableById.get(COMPLICATION_ID_TOP)
                complicationDrawable.draw(canvas, currentTimeMillis)
            } else {
                // Draw all of them, except the background complication
                for (complicationId in COMPLICATION_IDS.filterNot { it == COMPLICATION_ID_BACKGROUND }) {
                    val complicationDrawable = complicationDrawableById.get(complicationId)
                    complicationDrawable.draw(canvas, currentTimeMillis)
                }
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun drawHands(canvas: Canvas) {
            val seconds = calendar[Calendar.SECOND]
            val secondsRotation = seconds * 6F

            val minutes = calendar[Calendar.MINUTE]
            val minutesRotation = minutes * 6F + (seconds / 60F) * 6F

            val hoursRotation = calendar[Calendar.HOUR] * 30F + (minutes / 60F) * 30F

            canvas.save()

            // Hour
            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawBitmap(handHourBitmap, 0F, 0F, paintHour)

            // Minute
            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
            canvas.drawBitmap(handMinuteBitmap, 0F, 0F, paintMinute)

            // Second
            if (!ambient) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY)
                canvas.drawBitmap(handSecondBitmap, 0F, 0F, paintSecond)
            }

            canvas.restore()
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
            updateTimeHandler.sendEmptyMessage(0)
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning) {
                val now = System.currentTimeMillis()
                val delay = 1000 - now % 1000
                updateTimeHandler.sendEmptyMessageDelayed(0, delay)
            }
        }

        private val onPrefsChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            loadPrefs()
            updateColorsWithPalette()
            updatePaints()
            updateBitmaps()
            updateComplicationDrawableColors()
            updateTimer()
        }
    }

    override fun onCreateEngine() = SimpleWatchFaceEngine()
}