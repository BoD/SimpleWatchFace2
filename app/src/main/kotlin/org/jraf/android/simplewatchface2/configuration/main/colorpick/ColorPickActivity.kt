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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.support.wear.widget.CurvingLayoutCallback
import android.support.wear.widget.WearableLinearLayoutManager
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import org.jraf.android.simplewatchface2.R
import org.jraf.android.simplewatchface2.databinding.ColorPickBinding
import org.jraf.android.util.handler.HandlerUtil
import org.jraf.android.util.ui.screenshape.ScreenShapeHelper


class ColorPickActivity : Activity() {
    companion object {
        const val EXTRA_RESULT = "EXTRA_RESULT"
    }

    private lateinit var mBinding: ColorPickBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.color_pick)!!
        mBinding.rclList.setHasFixedSize(true)
        mBinding.rclList.isEdgeItemsCenteringEnabled = true

        // Apply an offset + scale on the items depending on their distance from the center (only for Round screens)
        if (ScreenShapeHelper.get(this).isRound) {
            mBinding.rclList.layoutManager =
                    WearableLinearLayoutManager(this, object : CurvingLayoutCallback(this) {

                        override fun onLayoutFinished(child: View, parent: RecyclerView) {
                            super.onLayoutFinished(child, parent)

                            val childTop = child.y + child.height / 2f
                            val childOffsetFromCenter = childTop - parent.height / 2f

                            child.pivotX = 1f
                            child.rotation = -15f * (childOffsetFromCenter / parent.height)
                        }
                    })

            // Also snaps
            LinearSnapHelper().attachToRecyclerView(mBinding.rclList)
        } else {
            // Square screen: no scale effect and no snapping
            mBinding.rclList.layoutManager = WearableLinearLayoutManager(this)
        }

        mBinding.rclList.adapter = ColorAdapter(this) { colorArgb, clickedView ->
            mBinding.vieRevealedColor.setBackgroundColor(colorArgb)

            val rect = Rect()
            clickedView.getGlobalVisibleRect(rect)

            val centerX = rect.left + clickedView.width / 2
            val centerY = rect.top + clickedView.height / 2

            val finalRadius = Math.hypot(
                mBinding.vieRevealedColor.width.toDouble(),
                mBinding.vieRevealedColor.height.toDouble()
            )
                .toFloat()

            val anim = ViewAnimationUtils.createCircularReveal(
                mBinding.vieRevealedColor,
                centerX,
                centerY,
                clickedView.width / 2F,
                finalRadius
            )

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, colorArgb))
                    finish()
                }
            })

            mBinding.vieRevealedColor.visibility = View.VISIBLE
            anim.start()
        }

        // For some unknown reason, this must be posted - if done right away, it doesn't work
        mBinding.rclList.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                HandlerUtil.getMainHandler().post {
                    mBinding.rclList.scrollToPosition(ColorAdapter.MID_POSITION)
                }
                mBinding.rclList.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
}