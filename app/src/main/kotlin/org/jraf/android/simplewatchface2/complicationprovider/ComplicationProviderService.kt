/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2019-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.simplewatchface2.complicationprovider

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import org.jraf.android.simplewatchface2.prefs.ComplicationProviderPrefs
import org.jraf.android.simplewatchface2.util.darker
import org.jraf.android.simplewatchface2.util.saturated
import kotlin.math.sqrt
import kotlin.random.Random

class ComplicationProviderService : android.support.wearable.complications.ComplicationProviderService() {
    private val prefs by lazy { ComplicationProviderPrefs.get(this) }

    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        if (type != ComplicationData.TYPE_LARGE_IMAGE) {
            // Ignore incompatible types
            manager.noUpdateRequired(complicationId)
            return
        }

        manager.updateComplicationData(
            complicationId,
            ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(Icon.createWithBitmap(createBitmap()))
                .build()
        )
    }

    private fun createBitmap(): Bitmap {
        val res = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(res)

        if (!prefs.randomColor) {
            // Background
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                prefs.color1.saturated().darker(),
                prefs.color1.saturated().darker().darker()
            )

            // Other transparent gradients
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                randomColor() and 0x38FFFFFF.toInt(),
                0
            )
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                0xE0000000.toInt(),
                0
            )
        } else {
            // Background
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                randomColor(),
                randomColor()
            )

            // Other transparent gradients
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                randomTransparentColor(),
                0
            )
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                randomTransparentColor(),
                0
            )
            drawGradientCircle(
                canvas,
                WIDTH.randomFloat(),
                HEIGHT.randomFloat(),
                0xE0000000.toInt(),
                0
            )
        }
        return res
    }

    private fun Int.randomFloat() = Random.nextDouble(toDouble()).toFloat()

    private fun drawGradientCircle(
        canvas: Canvas,
        x: Float,
        y: Float,
        centerColor: Int,
        edgeColor: Int
    ) {
        val gradient = RadialGradient(
            x,
            y,
            WIDTH / 2F,
            centerColor,
            edgeColor,
            Shader.TileMode.CLAMP
        )
        val paint = Paint()
        paint.isDither = true
        paint.shader = gradient
        canvas.drawCircle(x, y, WIDTH.toFloat() * sqrt(2F), paint)
    }

    private fun randomColor() = Color.HSVToColor(
        floatArrayOf(
            Random.nextFloat(0F, 360F),
            Random.nextFloat(.5F, 1F),
            .3F
        )
    )

    private fun randomTransparencyMask(): Int = (Random.nextInt(0x10, 0x80) shl (8 + 8 + 8)) or 0x00FFFFFF

    private fun randomTransparentColor() = randomColor() and randomTransparencyMask()

    private fun Random.Default.nextFloat(from: Float, until: Float): Float = Random.nextDouble(from.toDouble(), until.toDouble()).toFloat()

    companion object {
        private const val WIDTH = 64
        private const val HEIGHT = 64
    }
}