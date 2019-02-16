/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2018-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.simplewatchface2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

fun getBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int, width: Int, height: Int): Bitmap {
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        AppCompatResources.getDrawable(context, drawableId)!!.apply {
            setBounds(0, 0, width, height)
        }.draw(Canvas(this))
    }
}

fun Bitmap.tinted(@ColorInt tintColor: Int): Bitmap {
    // Create the result bitmap
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Make a paint that will draw the mask with a color filter
    val colorFilterPaint = Paint()
    colorFilterPaint.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
    val resultCanvas = Canvas(resultBitmap)

    // Draw this bitmap with the filter
    resultCanvas.drawBitmap(this, 0F, 0F, colorFilterPaint)
    return resultBitmap
}

fun Bitmap.withShadow(shadowRadius: Float, @ColorInt shadowColor: Int): Bitmap {
    // Inspired by https://stackoverflow.com/questions/17783467/drawing-an-outer-shadow-when-drawing-an-image

    // Create a 'mask' by drawing the source bitmap into an alpha only bitmap
    val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    val maskCanvas = Canvas(maskBitmap)
    maskCanvas.drawBitmap(this, 0F, 0F, null)

    // Create the result bitmap
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Make a paint that will draw the mask with a blur
    val blurPaint = Paint(ANTI_ALIAS_FLAG)
    blurPaint.color = shadowColor
    blurPaint.isAntiAlias = true
    blurPaint.maskFilter = BlurMaskFilter(shadowRadius, Blur.NORMAL)
    blurPaint.isFilterBitmap = true
    val resultCanvas = Canvas(resultBitmap)

    // Draw the mask into the result bitmap with the blur paint
    resultCanvas.drawBitmap(maskBitmap, 0F, 0F, blurPaint)

    // Draw this bitmap on top
    resultCanvas.drawBitmap(this, 0F, 0F, null)

    maskBitmap.recycle()
    return resultBitmap
}