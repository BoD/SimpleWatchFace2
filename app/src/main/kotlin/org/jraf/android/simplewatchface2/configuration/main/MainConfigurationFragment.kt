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
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationHelperActivity
import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity
import org.jraf.android.simplewatchface2.BuildConfig
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.prefs.ConfigurationConstants
import org.jraf.android.simplewatchface2.prefs.ConfigurationPrefs
import org.jraf.android.simplewatchface2.watchface.SimpleWatchFaceService
import org.jraf.android.util.about.AboutActivityIntentBuilder


class MainConfigurationFragment : PreferenceFragment() {
    companion object {
        private val COMPLICATION_TYPES_BACKGROUND = intArrayOf(ComplicationData.TYPE_LARGE_IMAGE)

        private val COMPLICATION_TYPES_SMALL = intArrayOf(
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_SHORT_TEXT
        )

        private val COMPLICATION_TYPES_BIG = intArrayOf(
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_LONG_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        )
    }

    private val prefs by lazy { ConfigurationPrefs.get(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.configuration_main)

        updateColorPreferences()

        // Colors
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_BACKGROUND, prefs.colorBackground)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_HOUR, prefs.colorHandHour)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_MINUTE, prefs.colorHandMinute)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_HAND_SECOND, prefs.colorHandSecond)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_DIAL, prefs.colorDial)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_COMPLICATIONS_BASE, prefs.colorComplicationsBase)
        setColorPrefClickListener(ConfigurationConstants.KEY_COLOR_COMPLICATIONS_HIGHLIGHT, prefs.colorComplicationsHighlight)

        // Complications
        setComplicationPrefClickListener("complicationBackground", SimpleWatchFaceService.COMPLICATION_ID_BACKGROUND, COMPLICATION_TYPES_BACKGROUND)
        setComplicationPrefClickListener("complicationLeft", SimpleWatchFaceService.COMPLICATION_ID_LEFT, COMPLICATION_TYPES_SMALL)
        setComplicationPrefClickListener("complicationTop", SimpleWatchFaceService.COMPLICATION_ID_TOP, COMPLICATION_TYPES_BIG)
        setComplicationPrefClickListener("complicationRight", SimpleWatchFaceService.COMPLICATION_ID_RIGHT, COMPLICATION_TYPES_SMALL)
        setComplicationPrefClickListener("complicationBottom", SimpleWatchFaceService.COMPLICATION_ID_BOTTOM, COMPLICATION_TYPES_BIG)

        // About
        findPreference("about").setOnPreferenceClickListener { _ ->
            startActivity(
                AboutActivityIntentBuilder()
                    .setAppName(getString(R.string.app_name))
                    .setBuildDate(BuildConfig.BUILD_DATE)
                    .setGitSha1(BuildConfig.GIT_SHA1)
                    .setAuthorCopyright(getString(R.string.about_authorCopyright))
                    .setLicense(getString(R.string.about_License))
                    .setShareTextSubject(getString(R.string.about_shareText_subject))
                    .setShareTextBody(getString(R.string.about_shareText_body))
                    .addLink(getString(R.string.about_email_uri), getString(R.string.about_email_text))
                    .addLink(getString(R.string.about_web_uri), getString(R.string.about_web_text))
                    .addLink(getString(R.string.about_sources_uri), getString(R.string.about_sources_text))
                    .build(context)
            )
            true
        }
    }

    private fun setComplicationPrefClickListener(prefKey: String, complicationId: Int, supportedTypes: IntArray) {
        findPreference(prefKey).setOnPreferenceClickListener { _ ->
            startActivity(
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                    activity,
                    ComponentName(
                        activity,
                        SimpleWatchFaceService::class.java
                    ),
                    complicationId,
                    *supportedTypes
                )
            )
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
        updatePrefColor(ConfigurationConstants.KEY_COLOR_BACKGROUND, prefs.colorBackground)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_HOUR, prefs.colorHandHour)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_MINUTE, prefs.colorHandMinute)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_HAND_SECOND, prefs.colorHandSecond)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_DIAL, prefs.colorDial)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_COMPLICATIONS_BASE, prefs.colorComplicationsBase)
        updatePrefColor(ConfigurationConstants.KEY_COLOR_COMPLICATIONS_HIGHLIGHT, prefs.colorComplicationsHighlight)

    }

    private fun updatePrefColor(prefKey: String, color: Int) {
        findPreference(prefKey).icon = (resources.getDrawable(R.drawable.configuration_list_item_color_indicator, null).mutate() as GradientDrawable).apply {
            setColor(color)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val pickedColor = data?.getIntExtra(ColorPickActivity.EXTRA_RESULT, 0)
        when (requestCode) {
            ConfigurationConstants.KEY_COLOR_BACKGROUND.asRequestCode() -> prefs.colorBackground = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_HOUR.asRequestCode() -> prefs.colorHandHour = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_MINUTE.asRequestCode() -> prefs.colorHandMinute = pickedColor
            ConfigurationConstants.KEY_COLOR_HAND_SECOND.asRequestCode() -> prefs.colorHandSecond = pickedColor
            ConfigurationConstants.KEY_COLOR_DIAL.asRequestCode() -> prefs.colorDial = pickedColor
            ConfigurationConstants.KEY_COLOR_COMPLICATIONS_BASE.asRequestCode() -> prefs.colorComplicationsBase = pickedColor
            ConfigurationConstants.KEY_COLOR_COMPLICATIONS_HIGHLIGHT.asRequestCode() -> prefs.colorComplicationsHighlight = pickedColor
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

