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
package org.jraf.android.simplewatchface2.configuration.main

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.preference.PreferenceFragment
import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity
import org.jraf.android.simplewatchface2.BuildConfig
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.prefs.ConfigurationConstants
import org.jraf.android.simplewatchface2.prefs.ConfigurationPrefs
import org.jraf.android.util.about.AboutActivityIntentBuilder

class MainConfigurationFragment : PreferenceFragment() {
    private val mPrefs by lazy { ConfigurationPrefs.get(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.configuration_main)

        updateColorPreferences()

        // Colors
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_BACKGROUND, mPrefs.colorBackground)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_HOUR, mPrefs.colorHandHour)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_MINUTE, mPrefs.colorHandMinute)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_SECOND, mPrefs.colorHandSecond)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_TICKS, mPrefs.colorTicks)

        // About
        findPreference("about").setOnPreferenceClickListener { _ ->
            val builder = AboutActivityIntentBuilder()
            builder.setAppName(getString(R.string.app_name))
            builder.setBuildDate(BuildConfig.BUILD_DATE)
            builder.setGitSha1(BuildConfig.GIT_SHA1)
            builder.setAuthorCopyright(getString(R.string.about_authorCopyright))
            builder.setLicense(getString(R.string.about_License))
            builder.setShareTextSubject(getString(R.string.about_shareText_subject))
            builder.setShareTextBody(getString(R.string.about_shareText_body))
            builder.addLink(getString(R.string.about_email_uri), getString(R.string.about_email_text))
            builder.addLink(getString(R.string.about_web_uri), getString(R.string.about_web_text))
            builder.addLink(getString(R.string.about_sources_uri), getString(R.string.about_sources_text))
            startActivity(builder.build(context))
            true
        }
    }

    private fun setColorPrefClickListener(prefKey: String, color: Int) {
        findPreference(prefKey).setOnPreferenceClickListener { _ ->
            val intent = ColorPickActivity.IntentBuilder().oldColor(color).build(context)
            startActivityForResult(intent, prefKey.asRequestCode())
            true
        }
    }

    private fun updateColorPreferences() {
        updatePrefColor(ConfigurationConstants.KEY_COLOR_BACKGROUND, mPrefs.colorBackground)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_HOUR, mPrefs.colorHandHour)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_MINUTE, mPrefs.colorHandMinute)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_SECOND, mPrefs.colorHandSecond)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_TICKS, mPrefs.colorTicks)
    }

    private fun updatePrefColor(prefKey: String, color: Int) {
        findPreference(prefKey).icon = (resources.getDrawable(R.drawable.configuration_list_item_color_indicator, null).mutate() as GradientDrawable).apply {
            setColor(color)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val pickedColor = ColorPickActivity.getPickedColor(data)
        when (requestCode) {
            ConfigurationConstants.KEY_COLOR_BACKGROUND.asRequestCode() -> mPrefs.colorBackground = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_HOUR.asRequestCode() -> mPrefs.colorHandHour = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_MINUTE.asRequestCode() -> mPrefs.colorHandMinute = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_SECOND.asRequestCode() -> mPrefs.colorHandSecond = pickedColor
            ConfigurationConstants.KEY_COLOR_TICKS.asRequestCode() -> mPrefs.colorTicks = pickedColor
        }
        updateColorPreferences()
    }

    private val mRequestCodes: MutableList<Int> = mutableListOf()

    private fun Any.asRequestCode(): Int {
        val hashCode = hashCode()
        if (!mRequestCodes.contains(hashCode)) mRequestCodes.add(hashCode)
        return mRequestCodes.indexOf(hashCode)
    }
}

