/*
 * Periodical "help" activity
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
import android.app.backup.BackupManager
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import com.z5i.periodical.PeriodicalDatabase.DayEntry
import com.z5i.periodical.context.ZApplication
import java.text.DateFormat

/**
 * Activity to handle the "Help" command
 */
class DetailsActivity : AppCompatActivity(), View.OnClickListener, TextWatcher {
    private var dbMain: PeriodicalDatabase? = null
    private var entry: DayEntry? = null
    private var buttonPeriodIntensity1: RadioButton? = null
    private var buttonPeriodIntensity2: RadioButton? = null
    private var buttonPeriodIntensity3: RadioButton? = null
    private var buttonPeriodIntensity4: RadioButton? = null

    /**
     * Called when the activity starts
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        val context = ZApplication.context
        super.onCreate(savedInstanceState)

        // Set up view
        setContentView(R.layout.activity_details)

        // Activate "back button" in Action Bar
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Get details
        val intent = intent
        val year = intent.getIntExtra("year", 1970)
        val month = intent.getIntExtra("month", 1)
        val day = intent.getIntExtra("day", 1)
        dbMain = PeriodicalDatabase(context)
        dbMain!!.loadCalculatedData()
        entry = dbMain!!.getEntryWithDetails(year, month, day)

        // Set header using the entry date
        val dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
        (findViewById<View>(R.id.labelDetailsHeader) as TextView).text = String.format("%s", dateFormat.format(entry!!.date.time))

        // Set period status
        val buttonPeriodYes = findViewById<RadioButton>(R.id.periodYes)
        val buttonPeriodNo = findViewById<RadioButton>(R.id.periodNo)
        var intensityEnabled = false
        when (entry!!.type) {
            DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED -> {
                buttonPeriodYes.isChecked = true
                intensityEnabled = true
            }
            else -> {
                buttonPeriodNo.isChecked = true
                // Default intensity for new period days
                entry!!.intensity = 2
            }
        }
        buttonPeriodYes.setOnClickListener(this)
        buttonPeriodNo.setOnClickListener(this)

        // Set period intensity
        buttonPeriodIntensity1 = findViewById(R.id.periodIntensity1)
        buttonPeriodIntensity2 = findViewById(R.id.periodIntensity2)
        buttonPeriodIntensity3 = findViewById(R.id.periodIntensity3)
        buttonPeriodIntensity4 = findViewById(R.id.periodIntensity4)
        when (entry!!.intensity) {
            1 -> buttonPeriodIntensity1?.isChecked = true
            2 -> buttonPeriodIntensity2?.isChecked = true
            3 -> buttonPeriodIntensity3?.isChecked = true
            4 -> buttonPeriodIntensity4?.isChecked = true
        }
        buttonPeriodIntensity1?.isEnabled = intensityEnabled
        buttonPeriodIntensity2?.isEnabled = intensityEnabled
        buttonPeriodIntensity3?.isEnabled = intensityEnabled
        buttonPeriodIntensity4?.isEnabled = intensityEnabled
        buttonPeriodIntensity1?.setOnClickListener(this)
        buttonPeriodIntensity2?.setOnClickListener(this)
        buttonPeriodIntensity3?.setOnClickListener(this)
        buttonPeriodIntensity4?.setOnClickListener(this)

        // Transfer notes
        val editNotes = findViewById<MultiAutoCompleteTextView>(R.id.editNotes)
        editNotes.setText(entry!!.notes)
        editNotes.addTextChangedListener(this)

        // Build list of events and symptoms
        val groupEvents = findViewById<LinearLayout>(R.id.groupEvents)
        val groupMood = findViewById<LinearLayout>(R.id.groupMood)
        val groupSymptoms = findViewById<LinearLayout>(R.id.groupSymptoms)
        val packageName = packageName
        val resources = resources
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        val marginLeft = (12 * Resources.getSystem().displayMetrics.density).toInt()
        val marginRight = (12 * Resources.getSystem().displayMetrics.density).toInt()
        layoutParams.setMargins(marginLeft, 0, marginRight, 0)

        // Elements 0-1 are events, 2-6 moods, 7-22 are symptoms
        val eventIds = intArrayOf(
                1,  // Intercourse
                18,  // Contraceptive pill
                20,  // Tired
                21,  // Energized
                22,  // Sad
                14,  // Grumpiness
                23,  // Edgy
                19,  // Spotting
                9,  // Intense bleeding
                2,  // Cramps
                17,  // Headache/migraine
                3,  // Back pain
                4,  // Middle pain left
                5,  // Middle pain right
                6,  // Breast pain/dragging pain
                7,  // Thrush/candida
                8,  // Discharge
                10,  // Temperature fluctuations
                11,  // Pimples
                12,  // Bloating
                13,  // Fainting
                15,  // Nausea
                16)
        var num = 0
        for (eventId in eventIds) {
            @SuppressLint("DefaultLocale") val resName = String.format("label_details_ev%d", eventId)
            val resId = resources.getIdentifier(resName, "string", packageName)
            if (resId != 0) {
                val option = AppCompatCheckBox(this)
                option.layoutParams = layoutParams
                option.textSize = 18f
                option.setText(resId)
                option.id = resId
                if (entry!!.symptoms.contains(eventId)) option.isChecked = true
                option.setOnClickListener(this)
                if (num < 2) {
                    groupEvents.addView(option)
                } else if (num > 1 && num < 7) {
                    groupMood.addView(option)
                } else {
                    groupSymptoms.addView(option)
                }
            }
            num++
        }
    }

    /**
     * Handler for ICS "home" button
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Home icon in action bar clicked, then close activity
                finish()
                true
            }
            else -> true
        }
    }

    /**
     * Listener for clicks on the radio buttons and checkboxes
     */
    override fun onClick(v: View) {
        val id = v.id
        when (id) {
            R.id.periodYes -> {
                dbMain!!.addPeriod(entry!!.date)
                databaseChanged()
                buttonPeriodIntensity1!!.isEnabled = true
                buttonPeriodIntensity2!!.isEnabled = true
                buttonPeriodIntensity3!!.isEnabled = true
                buttonPeriodIntensity4!!.isEnabled = true
            }
            R.id.periodNo -> {
                dbMain!!.removePeriod(entry!!.date)
                databaseChanged()
                buttonPeriodIntensity1!!.isEnabled = false
                buttonPeriodIntensity2!!.isEnabled = false
                buttonPeriodIntensity3!!.isEnabled = false
                buttonPeriodIntensity4!!.isEnabled = false
            }
            R.id.periodIntensity1 -> {
                entry!!.intensity = 1
                dbMain!!.addEntryDetails(entry!!)
                databaseChanged()
            }
            R.id.periodIntensity2 -> {
                entry!!.intensity = 2
                dbMain!!.addEntryDetails(entry!!)
                databaseChanged()
            }
            R.id.periodIntensity3 -> {
                entry!!.intensity = 3
                dbMain!!.addEntryDetails(entry!!)
                databaseChanged()
            }
            R.id.periodIntensity4 -> {
                entry!!.intensity = 4
                dbMain!!.addEntryDetails(entry!!)
                databaseChanged()
            }
            else -> {
                val packageName = packageName
                var resId: Int
                entry!!.symptoms.clear()
                var num = 1
                while (num < 24) {
                    @SuppressLint("DefaultLocale") val resName = String.format("label_details_ev%d", num)
                    resId = resources.getIdentifier(resName, "string", packageName)
                    if (resId != 0) {
                        val option = findViewById<CheckBox>(resId)
                        if (option.isChecked) entry!!.symptoms.add(num)
                    }
                    num++
                }
                dbMain!!.addEntryDetails(entry!!)
                databaseChanged()
            }
        }
    }

    /**
     * Handler for text changes in edit fields
     */
    override fun beforeTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        entry!!.notes = (findViewById<View>(R.id.editNotes) as MultiAutoCompleteTextView).text.toString()
        dbMain!!.addEntryDetails(entry!!)
        databaseChanged()
    }

    /**
     * Called when the activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        if (dbMain != null) dbMain!!.close()
    }

    /**
     * Helper to handle changes in the database
     */
    private fun databaseChanged() {
        dbMain!!.loadCalculatedData()
        val bm = BackupManager(this)
        bm.dataChanged()
    }
}