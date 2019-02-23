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
package org.jraf.android.simplewatchface2.configuration.complicationprovider

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.annotation.ColorInt
import org.jraf.android.androidwearcolorpicker.ColorPickActivity
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.prefs.ComplicationProviderConstants
import org.jraf.android.simplewatchface2.prefs.ComplicationProviderPrefs

class ComplicationProviderConfigurationFragment : PreferenceFragment() {

    private val prefs by lazy { ComplicationProviderPrefs.get(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.configuration_complication_provider)

        // Colors
        setColorPrefClickListener(ComplicationProviderConstants.KEY_COLOR1, ComplicationProviderConstants.DEFAULT_COLOR1)

        prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        onSharedPreferenceChangeListener.onSharedPreferenceChanged(null, null)
    }

    private val onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            findPreference(ComplicationProviderConstants.KEY_COLOR1).isEnabled = !prefs.randomColor
            updateColorPreferences()
        }

    private fun setColorPrefClickListener(prefKey: String, @ColorInt defaultColor: Int) {
        findPreference(prefKey).setOnPreferenceClickListener {
            val intent = ColorPickActivity.IntentBuilder()
                .colors(createListOfColors())
                .oldColor(prefs.getInt(prefKey, defaultColor))
                .build(context)
            startActivityForResult(intent, prefKey.asRequestCode())
            true
        }
    }

    private fun createListOfColors(): List<Int> {
        val colorCount = 4 * 12
        val res = mutableListOf<Int>()
        for (i in 0 until colorCount) {
            res += Color.HSVToColor(floatArrayOf((360F / colorCount) * i.toFloat(), 1F, 1F))
        }
        return res
    }

    private fun updateColorPreferences() {
        updatePrefColor(ComplicationProviderConstants.KEY_COLOR1, if (prefs.randomColor) 0x80B0B0B0.toInt() else prefs.color1)
    }

    private fun updatePrefColor(prefKey: String, @ColorInt color: Int) {
        findPreference(prefKey).icon = (resources.getDrawable(R.drawable.configuration_list_item_color_indicator, null).mutate() as GradientDrawable).apply {
            setColor(color)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val pickedColor = data?.getIntExtra(ColorPickActivity.EXTRA_RESULT, 0)
        when (requestCode) {
            ComplicationProviderConstants.KEY_COLOR1.asRequestCode() -> prefs.color1 = pickedColor
        }
        updateColorPreferences()
    }

    private val requestCodes: MutableList<Int> = mutableListOf()

    private fun Any.asRequestCode(): Int {
        val hashCode = hashCode()
        if (!requestCodes.contains(hashCode)) requestCodes.add(hashCode)
        return requestCodes.indexOf(hashCode)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        super.onDestroy()
    }
}

