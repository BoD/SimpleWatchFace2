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

class WatchfacePrefs(context: Context) {
    private val prefs = Prefs(context)
    val sharedPreferences = prefs.sharedPreferences

    var colorAuto by prefs.Boolean(false)
    var colorBackground by prefs.Int(DEFAULT_COLOR_BACKGROUND)
    var colorHandHour by prefs.Int(DEFAULT_COLOR_HAND_HOUR)
    var colorHandMinute by prefs.Int(DEFAULT_COLOR_HAND_MINUTE)
    var colorHandSecond by prefs.Int(DEFAULT_COLOR_HAND_SECOND)
    var colorDial by prefs.Int(DEFAULT_COLOR_DIAL)
    var colorComplicationsBase by prefs.Int(DEFAULT_COLOR_COMPLICATIONS_BASE)
    var colorComplicationsHighlight by prefs.Int(DEFAULT_COLOR_COMPLICATIONS_HIGHLIGHT)
    var dialStyle by prefs.String(DEFAULT_DIAL_STYLE)
    var smartNumbers by prefs.Boolean(false)

    enum class DialStyle {
        NOTHING,
        DOTS_4,
        DOTS_12,
        TICKS_4,
        TICKS_12,
        NUMBERS_4,
        NUMBERS_12,
    }

    companion object {
        const val KEY_COLOR_BACKGROUND = "colorBackground"
        const val KEY_COLOR_HAND_HOUR = "colorHandHour"
        const val KEY_COLOR_HAND_MINUTE = "colorHandMinute"
        const val KEY_COLOR_HAND_SECOND = "colorHandSecond"
        const val KEY_COLOR_DIAL = "colorDial"
        const val KEY_COLOR_COMPLICATIONS_BASE = "colorComplicationsBase"
        const val KEY_COLOR_COMPLICATIONS_HIGHLIGHT = "colorComplicationsHighlight"
        const val KEY_SMART_NUMBERS = "smartNumbers"

        const val DEFAULT_COLOR_BACKGROUND = 0xFF220000.toInt()
        const val DEFAULT_COLOR_HAND_HOUR = Color.WHITE
        const val DEFAULT_COLOR_HAND_MINUTE = Color.WHITE
        const val DEFAULT_COLOR_HAND_SECOND = Color.RED
        const val DEFAULT_COLOR_DIAL = Color.RED
        const val DEFAULT_COLOR_COMPLICATIONS_BASE = Color.WHITE
        const val DEFAULT_COLOR_COMPLICATIONS_HIGHLIGHT = Color.RED
        val DEFAULT_DIAL_STYLE = DialStyle.TICKS_12.name
    }
}