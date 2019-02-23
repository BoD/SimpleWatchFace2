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

import android.graphics.Color
import org.jraf.android.prefs.DefaultBoolean
import org.jraf.android.prefs.DefaultInt
import org.jraf.android.prefs.DefaultString
import org.jraf.android.prefs.Prefs

@Prefs(useAndroidX = true/*, fileName = "complication_provider"*/)
class ComplicationProvider {
    enum class Style {
        CLASSIC,
        PIXELATED,
        SQUARES,
        CIRCLES
    }

    @DefaultBoolean(true)
    var randomColor: Boolean? = true

    @DefaultInt(Color.RED)
    var color1: Int? = null

    @DefaultString("CLASSIC")
    var style: String? = null
}