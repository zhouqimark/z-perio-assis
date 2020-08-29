/*
 * Helper to create an animated calendar view
 * Copyright (C) 2012-2020 Arno Welzel
 *
 * This code is free software: you can redistribute it and/or modify
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
package com.z5i.periodical

import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

/**
 * Helper for view animations
 */
internal class AnimationHelper {
    companion object {
        @JvmStatic
        private var prevCounter = 0
        @JvmStatic
        private var nextCounter = 0
        fun goNext() {
            if (prevCounter == 0) {
                nextCounter += 1
            } else {
                prevCounter -= 1
            }
        }
        fun goPrevious() {
            if(nextCounter == 0) {
                prevCounter += 1
            } else {
                nextCounter -= 1
            }
        }
        fun backFromNext(): Boolean {
            return nextCounter > 0 && prevCounter == 0
        }

        fun backFromPrev(): Boolean {
            return prevCounter > 0 && nextCounter == 0
        }
        fun getCounter(): Int {
            return prevCounter + nextCounter
        }
        /**
         * Create an animation in from the right
         *
         * @return Animation into the view from the right
         */
        fun inFromRightAnimation(): Animation {
            val inFromRight: Animation = TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f)
            inFromRight.duration = 250
            inFromRight.interpolator = AccelerateInterpolator()
            return inFromRight
        }

        /**
         * Create an animation out to the left
         *
         * @return Animation out of the view to the left
         */
        fun outToLeftAnimation(): Animation {
            val outtoLeft: Animation = TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f)
            outtoLeft.duration = 250
            outtoLeft.interpolator = AccelerateInterpolator()
            return outtoLeft
        }

        /**
         * Create an animation in from the left
         *
         * @return Animation into the view from the left
         */
        fun inFromLeftAnimation(): Animation {
            val inFromLeft: Animation = TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f)
            inFromLeft.duration = 250
            inFromLeft.interpolator = AccelerateInterpolator()
            return inFromLeft
        }

        /**
         * Create an animation out to the right
         *
         * @return Animation out of the view to the right
         */
        fun outToRightAnimation(): Animation {
            val outtoRight: Animation = TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, +1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f)
            outtoRight.duration = 250
            outtoRight.interpolator = AccelerateInterpolator()
            return outtoRight
        }
    }
}