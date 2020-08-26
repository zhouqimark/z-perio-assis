/*
 * Custom adapter for calendar entry list view
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
import android.content.res.Resources
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.z5i.periodical.PeriodicalDatabase.DayEntry
import com.z5i.periodical.context.ZApplication.Companion.context

/**
 * Custom adapter to populate calendar entry list items
 */
internal class DayEntryAdapter
/**
 * Constructor
 *
 * @param context     Application content
 * @param list        List with all calendar entries including details
 * @param packageName Application package from getPackageName()
 * @param resources   Global resources from getResources()
 */constructor (private val entryList: List<DayEntry>, private val packageName: String, private val resources: Resources) : ArrayAdapter<DayEntry?>(context, 0, entryList) {
    /**
     * Constructs a single item view
     *
     * @param position    Position of the item in the list
     * @param convertView Existing view to use (if null, a new one will be created)
     * @param parent      Group in which this view is inserted
     * @return View to be used for the item
     */
    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItem = convertView
        if (listItem == null) listItem = LayoutInflater.from(context).inflate(R.layout.listdetailsitem, parent, false)
        val currentEntry = entryList[position]
        var textEvents = ""
        var textMood = ""
        var textSymptoms = ""

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
            val resName = String.format("label_details_ev%d", eventId)
            val resId = resources.getIdentifier(resName, "string", packageName)
            if (resId != 0) {
                if (currentEntry.symptoms.contains(eventId)) {
                    if (num < 2) {
                        if (!textEvents.isEmpty()) textEvents += "\n"
                        textEvents += "\u2022 " + resources.getString(resId)
                    } else if (num > 1 && num < 7) {
                        if (!textMood.isEmpty()) textMood += "\n"
                        textMood += "\u2022 " + resources.getString(resId)
                    } else {
                        if (!textSymptoms.isEmpty()) textSymptoms += "\n"
                        textSymptoms += "\u2022 " + resources.getString(resId)
                    }
                }
            }
            num++
        }
        val dateFormat = DateFormat.getDateFormat(context)
        var view: TextView
        view = listItem!!.findViewById(R.id.item_date)
        when (currentEntry.type) {
            DayEntry.PERIOD_START -> view.text = dateFormat.format(currentEntry.date.time) + " \u2014 " +
                    resources.getString(R.string.event_periodstart)
            DayEntry.PERIOD_CONFIRMED -> view.text = dateFormat.format(currentEntry.date.time) + " \u2014 " + String.format(
                    resources.getString(R.string.label_period_day),
                    currentEntry.dayofcycle)
            else -> view.text = dateFormat.format(currentEntry.date.time)
        }
        view = listItem.findViewById(R.id.item_intensity)
        if (currentEntry.type == DayEntry.PERIOD_START ||
                currentEntry.type == DayEntry.PERIOD_CONFIRMED) {
            var intensity = "?"
            when (currentEntry.intensity) {
                1 -> intensity = resources.getString(R.string.label_details_intensity1)
                2 -> intensity = resources.getString(R.string.label_details_intensity2)
                3 -> intensity = resources.getString(R.string.label_details_intensity3)
                4 -> intensity = resources.getString(R.string.label_details_intensity4)
            }
            view.text = intensity
        } else {
            view.text = "\u2014"
        }
        view = listItem.findViewById(R.id.item_notes)
        if (currentEntry.notes.isEmpty()) view.text = "\u2014" else view.text = currentEntry.notes
        view = listItem.findViewById(R.id.item_event)
        if (textEvents.isEmpty()) view.text = "\u2014" else view.text = textEvents
        view = listItem.findViewById(R.id.item_mood)
        if (textMood.isEmpty()) view.text = "\u2014" else view.text = textMood
        view = listItem.findViewById(R.id.item_symptom)
        if (textSymptoms.isEmpty()) view.text = "\u2014" else view.text = textSymptoms
        return listItem
    }
}