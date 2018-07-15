/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2018 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
@file:Suppress("NOTHING_TO_INLINE")

package org.jraf.android.simplewatchface2.util

import android.graphics.Color
import android.support.annotation.ColorInt
import androidx.core.graphics.luminance

@ColorInt
inline fun @receiver:ColorInt Int.grayScale(): Int {
    val luminance = (luminance * 255).toInt()
    return Color.rgb(luminance, luminance, luminance)
}

@ColorInt
inline fun @receiver:ColorInt Int.withAlpha(alpha: Float): Int {
    return Color.argb((alpha * 255).toInt(), Color.red(this), Color.green(this), Color.blue(this))
}