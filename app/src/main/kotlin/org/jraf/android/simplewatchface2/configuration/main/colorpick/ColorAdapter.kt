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
package org.jraf.android.simplewatchface2.configuration.main.colorpick

import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.databinding.ColorPickItemBinding

class ColorAdapter(context: Context, private val mColorPickCallbacks: (Int, ImageView) -> Unit) :
    RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
    companion object {
        const val HUE_COUNT = 12
        const val SATURATION_COUNT = 3
        const val VALUE_COUNT = 3

        const val MID_POSITION =
            Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % (HUE_COUNT * SATURATION_COUNT + 1))
    }

    private val mLayoutInflater: LayoutInflater = LayoutInflater.from(context)

    class ViewHolder(val binding: ColorPickItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ColorPickItemBinding>(
            mLayoutInflater,
            R.layout.color_pick_item,
            parent,
            false
        )!!
        for (i in 0 until VALUE_COUNT) {
            binding.ctnColors.addView(
                mLayoutInflater.inflate(R.layout.color_pick_item_color, binding.ctnColors, false)
            )!!
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, realPosition: Int) {
        val position = realPosition % (HUE_COUNT * SATURATION_COUNT + 1)
        val hue =
            if (position == 0) 0F else (((position - 1) / SATURATION_COUNT) / HUE_COUNT.toFloat()) * 360f
        val saturation =
            if (position == 0) 0F else (((position - 1) % SATURATION_COUNT) + 1) / (SATURATION_COUNT.toFloat())
        for (i in 0 until VALUE_COUNT) {
            var value =
                if (position == 0) i / (VALUE_COUNT - 1).toFloat() else (i + 1) / VALUE_COUNT.toFloat()
            // Make the first values darker
            value = Math.pow(value.toDouble(), 1.5).toFloat()

            val imgColor = holder.binding.ctnColors.getChildAt(i) as ImageView
            val color = Color.HSVToColor(floatArrayOf(hue, saturation, value))
            (imgColor.drawable as GradientDrawable).setColor(color)
            imgColor.setOnClickListener { mColorPickCallbacks(color, imgColor) }
        }
    }

    override fun getItemCount() = Int.MAX_VALUE
}

