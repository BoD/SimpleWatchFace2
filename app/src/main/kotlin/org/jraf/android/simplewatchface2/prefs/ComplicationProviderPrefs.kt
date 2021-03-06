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
package org.jraf.android.simplewatchface2.prefs

import android.content.Context
import android.graphics.Color
import org.jraf.android.kprefs.Prefs

class ComplicationProviderPrefs(context: Context) {
    private val prefs = Prefs(context, fileName = "complication_provider")
    val sharedPreferences = prefs.sharedPreferences

    var randomColor by prefs.Boolean(true)
    var color1 by prefs.Int(DEFAULT_COLOR1)
    var style by prefs.String(Style.CLASSIC.name)

    enum class Style {
        CLASSIC,
        PIXELATED,
        SQUARES,
        CIRCLES
    }

    companion object {
        const val KEY_COLOR1 = "color1"

        const val DEFAULT_COLOR1 = Color.RED
    }
}