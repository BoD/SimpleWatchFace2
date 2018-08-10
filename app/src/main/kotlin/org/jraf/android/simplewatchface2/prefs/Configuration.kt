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
package org.jraf.android.simplewatchface2.prefs

import android.graphics.Color
import org.jraf.android.prefs.DefaultBoolean
import org.jraf.android.prefs.DefaultInt
import org.jraf.android.prefs.DefaultString
import org.jraf.android.prefs.Prefs

@Prefs
class Configuration {
    enum class DialStyle {
        NOTHING,
        DOTS_4,
        DOTS_12,
        TICKS_4,
        TICKS_12,
        NUMBERS_4,
        NUMBERS_12,
    }

    @DefaultInt(0xFF220000.toInt())
    var colorBackground: Int? = null

    @DefaultInt(Color.WHITE)
    var colorHandHour: Int? = null

    @DefaultInt(Color.WHITE)
    var colorHandMinute: Int? = null

    @DefaultInt(Color.RED)
    var colorHandSecond: Int? = null

    @DefaultInt(Color.RED)
    var colorDial: Int? = null

    @DefaultInt(Color.WHITE)
    var colorComplicationsBase: Int? = null

    @DefaultInt(Color.RED)
    var colorComplicationsHighlight: Int? = null

    @DefaultString("TICKS_12")
    var dialStyle: String? = null

    @DefaultBoolean(false)
    var smartNumbers: Boolean? = false
}