/*
 * Periodical calendar cell class
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.Button
import com.z5i.periodical.PeriodicalDatabase.DayEntry
import top.androidman.SuperButton

/**
 * Custom button class to display the calendar cells
 */
@SuppressLint("AppCompatCustomView")
class CalendarCell(context: Context, attrs: AttributeSet?) : Button(context, attrs) {
    /**
     * flag for "is current day"
     */
    private var isCurrent = false

    /**
     * entry type as in database
     */
    private var type: Int
    /**
     * Get the displayed day
     *
     * @return The day of the month (1-31)
     */
    /**
     * Set the day to be displayed
     *
     * @param day The day of the month (1-31)
     */
    /**
     * displayed day of month (1-31)
     */
    var day: Int
        set
    /**
     * Get the month
     *
     * @return The month (1-12)
     */
    /**
     * Set the month
     *
     * @param month The month (1-12)
     */
    /**
     * month (1-12)
     */
    var month: Int
        set
    /**
     * Get the year
     *
     * @return The year
     */
    /**
     * Set the year
     *
     * @param year The year
     */
    /**
     * year including century
     */
    var year: Int
        set

    /**
     * day of cycle (1-n, 0 to hide)
     */
    private var dayofcycle = 0

    /**
     * intensity during period (1-4)
     */
    private var intensity = 0

    /**
     * flag for intercourse
     */
    private var intercourse: Boolean

    /**
     * flag for notes
     */
    private var notes = false

    /**
     * Display metrics
     */
    private val metrics: DisplayMetrics

    /**
     * Rectangle of the cell canvas
     */
    private val rectCanvas: RectF

    /**
     * Paint for the label (day of month)
     */
    private val paintLabel: Paint

    /**
     * Paint for the intensity markers
     */
    private val paintIntensity: Paint

    /**
     * Background paint for the cell
     */
    private val paintBackground: Paint

    /**
     * Paint for the cell if it focused
     */
    private val paintFocus: Paint

    /**
     * Paint for the "is current day" oval marker
     */
    private val paintOval: Paint

    /**
     * First rectangle for the "is current day" oval marker
     */
    private val rectOval1: RectF

    /**
     * Second rectangle for the "is current day" oval marker
     */
    private val rectOval2: RectF

    /**
     * Rectangle for the label (day of month)
     */
    private val rectLabel: Rect

    private var gradientPressButton: LinearGradient

    /**
     * Gradient for entries of type "confirmed period"
     */
    private var gradientPeriodConfirmed: LinearGradient

    /**
     * Gradient for entries of type "predicted period"
     */
    private var gradientPeriodPredicted: LinearGradient

    /**
     * Gradient for entries of type "predicted fertility" and "ovulation"
     */
    private var gradientFertilityPredicted: LinearGradient

    /**
     * Gradient for entries of type "predicted fertility in the future" and "ovulation in the future
     */
    private var gradientFertilityFuture: LinearGradient

    /**
     * Gradient for entries of type "infertile day predicted"
     */
    private var gradientInfertilePredicted: LinearGradient

    /**
     * Gradient for entries of type "infertile day predicted in the future"
     */
    private var gradientInfertileFuture: LinearGradient

    /**
     * Gradient for empty entries
     */
    private val gradientEmpty: LinearGradient

    /**
     * Rectangle for overlays
     */
    private val rectOverlay: Rect

    /**
     * Bitmap for entries of type "period"  and "predicted period"
     */
    private val bitmapPeriod: Bitmap

    /**
     * Bitmap for entries of type "ovulation"
     */
    private val bitmapOvulation: Bitmap

    /**
     * Bitmap for entries of type "ovulation in the future"
     */
    private val bitmapOvulationFuture: Bitmap

    /**
     * Bitmap for entries with flag "intercourse"
     */
    private val bitmapIntercourse: Bitmap

    /**
     * Bitmap for entries with flag "intercourse" (black variant)
     */
    private val bitmapIntercourseBlack: Bitmap

    /**
     * Bitmap for entries with flag "notes"
     */
    private val bitmapNotes: Bitmap

    /**
     * Bitmap for entries with flag "notes" (black variant)
     */
    private val bitmapNotesBlack: Bitmap

    /**
     * Paint for bitmaps
     */
    private val paintBitmap: Paint

    /* Current view orientation (portrait, landscape) */
    private var orientation = 0

    /**
     * Handle size changes to adapt size specific elements
     *
     * @param w    Current width
     * @param h    Current height
     * @param oldw Old width
     * @param oldh Old height
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectCanvas[0f, 0f, w.toFloat()] = h.toFloat()
        //gradientPeriodConfirmed = makeCellGradient(-0xbbcca, -0xbbcca)
        makeCellGradient(0xfff093fb.toInt(), 0xfff5576c.toInt())
        gradientPeriodPredicted = makeCellGradient(-0x106566, -0x106566)
        gradientFertilityPredicted = makeCellGradient(-0xde690d, -0xde690d)
        gradientFertilityFuture = makeCellGradient(-0x6f3507, -0x6f3507)
        gradientInfertilePredicted = makeCellGradient(-0x11a8, -0x11a8)
        gradientInfertileFuture = makeCellGradient(-0xa63, -0xa63)
    }

    /**
     * Custom draw
     *
     * @param canvas The canvas to draw on
     */
    override fun onDraw(canvas: Canvas) {
        var gradient = gradientEmpty
        var colorLabel = -0x22000000
        var label: String

        // Adjust overlay size depending on orientation
        var overlaysize = 18
        if (orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270) {
            overlaysize = 14
        }

        // Draw background, depending on state
        if (isPressed) {
            // If cell is pressed, then fill with solid color
            //paintFocus.color = 0xff000000.toInt()
            paintFocus.isDither = true
            paintFocus.shader = gradientPressButton
            paintFocus.style = Paint.Style.FILL
            paintFocus.isAntiAlias = true
            canvas.drawCircle(rectCanvas.centerX(), rectCanvas.centerY(), 20 * metrics.density, paintFocus)
            colorLabel = -0x22000000
            Log.d("CalendarCell", colorLabel.toInt().toString())
        } else {
            // normal state (or focused), then draw color
            // depending on entry type
            when (type) {
                DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED -> {
                    gradient = gradientPeriodConfirmed
                    colorLabel = -0x1
                }
                DayEntry.PERIOD_PREDICTED -> {
                    gradient = gradientPeriodPredicted
                    colorLabel = -0x22000000
                }
                DayEntry.FERTILITY_PREDICTED, DayEntry.OVULATION_PREDICTED -> {
                    gradient = gradientFertilityPredicted
                    colorLabel = -0x1
                }
                DayEntry.FERTILITY_FUTURE, DayEntry.OVULATION_FUTURE -> {
                    gradient = gradientFertilityFuture
                    colorLabel = -0x22000000
                }
                DayEntry.INFERTILE_PREDICTED -> {
                    gradient = gradientInfertilePredicted
                    colorLabel = -0x22000000
                }
                DayEntry.INFERTILE_FUTURE -> {
                    gradient = gradientInfertileFuture
                    colorLabel = -0x22000000
                }
            }

            // Draw background
            paintBackground.isDither = true
            paintBackground.shader = gradient
            paintBackground.style = Paint.Style.FILL
            paintBackground.isAntiAlias = true
            canvas.drawCircle(rectCanvas.centerX(), rectCanvas.centerY(), 20 * metrics.density, paintBackground)
            // Draw period start indicator
            rectOverlay[(4 * metrics.density).toInt(), rectCanvas.height().toInt() - ((2 + overlaysize) * metrics.density).toInt(), ((overlaysize + 2) * metrics.density).toInt()] = rectCanvas.height().toInt() - (4 * metrics.density).toInt()

            if (type == DayEntry.PERIOD_START) {
                canvas.drawBitmap(bitmapPeriod, null, rectOverlay, paintBitmap)
            }

            // Draw ovulation indicator
            if (type == DayEntry.OVULATION_PREDICTED) {
                canvas.drawBitmap(bitmapOvulation, null, rectOverlay, paintBitmap)
            }
            if (type == DayEntry.OVULATION_FUTURE) {
                canvas.drawBitmap(bitmapOvulationFuture, null, rectOverlay, paintBitmap)
            }

            // Draw intensity indicator
            if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
                var i = 0
                while (i < intensity && i < 4) {
                    canvas.drawCircle((6 + i * 6) * metrics.density, 6 * metrics.density,
                            2 * metrics.density, paintIntensity)
                    i++
                }
            }

            // Draw intercourse indicator
            rectOverlay[rectCanvas.width().toInt() - (overlaysize * metrics.density).toInt(), (4 * metrics.density).toInt(), rectCanvas.width().toInt() - (4 * metrics.density).toInt()] = (overlaysize * metrics.density).toInt()
            if (intercourse) {
                if (colorLabel == -0x1) {
                    canvas.drawBitmap(bitmapIntercourse, null, rectOverlay, paintBitmap)
                } else {
                    canvas.drawBitmap(bitmapIntercourseBlack, null, rectOverlay, paintBitmap)
                }
            }

            // Draw notes indicator
            rectOverlay[(rectCanvas.width() / 2 - overlaysize * metrics.density / 2).toInt(), rectCanvas.height().toInt() - ((2 + overlaysize) * metrics.density).toInt(), (rectCanvas.width() / 2 + overlaysize * metrics.density / 2).toInt()] = rectCanvas.height().toInt() - (4 * metrics.density).toInt()
            if (notes) {
                if (colorLabel == -0x1) {
                    canvas.drawBitmap(bitmapNotes, null, rectOverlay, paintBitmap)
                } else {
                    canvas.drawBitmap(bitmapNotesBlack, null, rectOverlay, paintBitmap)
                }
            }
        }

        // Draw drawer_menu label
        label = text.toString()
        paintLabel.textSize = 10 * metrics.scaledDensity
        paintLabel.color = colorLabel
        paintLabel.getTextBounds(label, 0, label.length, rectLabel)
        canvas.drawText(label, (width - rectLabel.width()) / 2.toFloat(),
                rectLabel.height() + (height - rectLabel.height()) / 2.toFloat(), paintLabel)

        // Draw day of cycle, if applicable
        if (!isPressed && dayofcycle != 0) {
            label = dayofcycle.toString()
            paintLabel.textSize = 12 * metrics.scaledDensity
            paintLabel.color = colorLabel
            paintLabel.getTextBounds(label, 0, label.length, rectLabel)
            canvas.drawText(label,
                    rectCanvas.width() - rectLabel.width() - 4 * metrics.density,
                    rectCanvas.height() - rectLabel.height() / 2 - 1 * metrics.density,
                    paintLabel)
        }
        if (!isPressed) {
            // Draw the "current day" mark, if needed
            if (isCurrent) {
                paintOval.style = Paint.Style.STROKE
                paintOval.isAntiAlias = true
                rectOval1[rectCanvas.left + 22 * metrics.density, 4 * metrics.density, rectCanvas.right - 15 * metrics.density] = rectCanvas.bottom - 4 * metrics.density
                rectOval2[rectOval1.left - 6 * metrics.density, rectOval1.top - 1, rectOval1.right] = rectOval1.bottom

                // Center oval rectangle as a square
                val delta = (rectOval1.height() - rectOval1.width()) / 2
                if (delta > 0) {
                    rectOval1.top += delta
                    rectOval1.bottom -= delta
                    rectOval2.top += delta
                    rectOval2.bottom -= delta
                } else if (delta < 0) {
                    rectOval1.left -= delta
                    rectOval1.right += delta
                    rectOval2.left -= delta
                    rectOval2.right += delta
                }

                // Draw oval
                paintOval.color = 0xff00bfff.toInt()
                paintOval.strokeWidth = 2 * metrics.density
                canvas.drawArc(rectOval1, 200f, 160f, false, paintOval)
                canvas.drawArc(rectOval2, 0f, 240f, false, paintOval)
                paintOval.color = 0xff00bfff.toInt()
                paintOval.strokeWidth = 2 * metrics.density
                canvas.drawArc(rectOval1, 200f, 160f, false, paintOval)
                canvas.drawArc(rectOval2, 0f, 240f, false, paintOval)
            }
        }

        // Draw focused or pressed state, if the button is focused
        if (isFocused) {
            paintFocus.style = Paint.Style.STROKE
            paintFocus.strokeWidth = 4 * metrics.density
            paintFocus.color = -0x61f8
            canvas.drawRoundRect(rectCanvas, 3 * metrics.density, 3 * metrics.density, paintFocus)
        }
    }

    /**
     * Helper to create a linear gradient
     *
     * @param colorStart Color to start with
     * @param colorEnd   Color to end with
     * @return A LinearGradient with the given colors at a 45 degree angle
     */
    private fun makeCellGradient(colorStart: Int, colorEnd: Int): LinearGradient {
        return LinearGradient(0.0f, 0.0f,
                rectCanvas.width(), rectCanvas.height(),
                colorStart, colorEnd,
                Shader.TileMode.CLAMP)
    }

    /**
     * Helper to update content description on all calendar cells
     */
    fun updateContentDescription() {
        val cal = GregorianCalendarExt()
        cal[year, month - 1] = day
        var contentDescription = DateUtils.formatDateTime(context, cal.timeInMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)
        when (type) {
            DayEntry.PERIOD_START -> contentDescription += " - " + resources.getString(R.string.label_period_started)
            DayEntry.PERIOD_CONFIRMED -> contentDescription += " - " + resources.getString(R.string.label_period)
            DayEntry.PERIOD_PREDICTED -> contentDescription += " - " + resources.getString(R.string.label_period_predicted)
            DayEntry.FERTILITY_PREDICTED, DayEntry.FERTILITY_FUTURE -> contentDescription += " - " + resources.getString(R.string.label_fertile)
            DayEntry.OVULATION_PREDICTED, DayEntry.OVULATION_FUTURE -> contentDescription += " - " + resources.getString(R.string.label_ovulation)
            DayEntry.INFERTILE_PREDICTED, DayEntry.INFERTILE_FUTURE -> contentDescription += " - " + resources.getString(R.string.label_infertile)
        }
        setContentDescription(contentDescription)
    }

    /**
     * Set "is current day" flag
     *
     * @param current true if this is the current day, false otherwise
     */
    fun setCurrent(current: Boolean) {
        isCurrent = current
    }

    /**
     * Set current cell type
     *
     * @param type The type as stored in the database to define the look of the cell
     */
    fun setType(type: Int) {
        this.type = type
    }

    /**
     * Set day of cycle
     *
     * @param dayofcycle The type as stored in the database to define the look of the cell
     */
    fun setDayofcycle(dayofcycle: Int) {
        this.dayofcycle = dayofcycle
    }

    /**
     * Set intensity
     *
     * @param intensity Intensity of this day (1-4)
     */
    fun setIntensity(intensity: Int) {
        this.intensity = intensity
    }

    /**
     * Set "intercourse" flag
     *
     * @param intercourse true if intercourse, false otherwise
     */
    fun setIntercourse(intercourse: Boolean) {
        this.intercourse = intercourse
    }

    /**
     * Set "notes" flag
     *
     * @param notes true if notes exist, false otherwise
     */
    fun setNotes(notes: Boolean) {
        this.notes = notes
    }

    /**
     * Constructor
     *
     * @param context Application context
     * @param attrs   Resource attributes
     */
    init {
        type = DayEntry.EMPTY
        day = 1
        month = 1
        year = 1
        intercourse = false
        metrics = getContext().resources.displayMetrics

        // Get current size of the canvas
        rectCanvas = RectF()

        // Create resources needed for drawing
        paintLabel = Paint()
        paintLabel.isAntiAlias = true
        paintLabel.isSubpixelText = true
        paintLabel.color = Color.BLACK
        paintLabel.textAlign = Align.LEFT
        paintIntensity = Paint()
        paintIntensity.style = Paint.Style.FILL
        paintIntensity.color = -0x1
        paintBackground = Paint()
        paintOval = Paint()
        paintFocus = Paint()
        paintFocus.isAntiAlias = true
        rectOval1 = RectF()
        rectOval2 = RectF()
        rectLabel = Rect()
        gradientPressButton = makeCellGradient(0xff868f96.toInt(), 0xff596164.toInt())
        gradientPeriodConfirmed = makeCellGradient(0xfff5576c.toInt(), 0xfff093fb.toInt())
        gradientPeriodPredicted = makeCellGradient(-0x106566, -0x106566)
        gradientFertilityPredicted = makeCellGradient(-0xde690d, -0xde690d)
        gradientFertilityFuture = makeCellGradient(-0x6f3507, -0x6f3507)
        gradientInfertilePredicted = makeCellGradient(-0x11a8, -0x11a8)
        gradientInfertileFuture = makeCellGradient(-0xa63, -0xa63)
        //gradientEmpty = makeCellGradient(-0x8a8a8b, -0x8a8a8b)
        gradientEmpty = makeCellGradient( 0xffcfd9df.toInt(), 0xffe2ebf0.toInt());

        // Overlays
        rectOverlay = Rect()
        paintBitmap = Paint()
        paintBitmap.style = Paint.Style.FILL
        paintBitmap.isFilterBitmap = true
        bitmapPeriod = BitmapFactory.decodeResource(resources,
                R.drawable.ic_start)
        bitmapOvulation = BitmapFactory.decodeResource(resources,
                R.drawable.ic_ovulation)
        bitmapOvulationFuture = BitmapFactory.decodeResource(resources,
                R.drawable.ic_ovulation_predicted)
        bitmapIntercourse = BitmapFactory.decodeResource(resources,
                R.drawable.ic_intercourse)
        bitmapIntercourseBlack = BitmapFactory.decodeResource(resources,
                R.drawable.ic_intercourse_black)
        bitmapNotes = BitmapFactory.decodeResource(resources,
                R.drawable.ic_notes)
        bitmapNotesBlack = BitmapFactory.decodeResource(resources,
                R.drawable.ic_notes_black)

        // Get current screen orientation
        if (!isInEditMode) { // Don't try this in layout editor
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (wm != null) {
                val display = wm.defaultDisplay
                if (display != null) orientation = display.rotation
            }
        }
    }
}