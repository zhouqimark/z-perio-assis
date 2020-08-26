/*
 * Periodical list activity
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
package de.arnowelzel.android.periodical

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry
import java.util.*

/**
 * Activity to handle the "List" command
 */
class ListActivity : AppCompatActivity(), OnItemClickListener {
    /**
     * Database for calendar data
     */
    private var dbMain: PeriodicalDatabase? = null

    /**
     * Called when activity starts
     */
    @SuppressLint("NewApi")
    public override fun onCreate(savedInstanceState: Bundle?) {
        val context = applicationContext!!
        super.onCreate(savedInstanceState)
        val maximumcyclelength: Int
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        maximumcyclelength = try {
            preferences.getString("maximum_cycle_length", "183")!!.toInt()
        } catch (e: NumberFormatException) {
            183
        }

        // Set up database and string array for the list
        dbMain = PeriodicalDatabase(context)
        dbMain!!.loadRawData()
        val entries = arrayOfNulls<String>(dbMain!!.dayEntries.size)
        val dateFormat = DateFormat
                .getDateFormat(context)
        val dayIterator: Iterator<DayEntry> = dbMain!!.dayEntries.iterator()
        var pos = 0
        var dayPrevious: DayEntry? = null
        var day: DayEntry? = null
        var isFirst = true
        while (dayIterator.hasNext()) {
            if (isFirst) {
                isFirst = false
            } else {
                dayPrevious = day
            }
            day = dayIterator.next()
            entries[pos] = dateFormat.format(day.date.time)
            when (day.type) {
                DayEntry.PERIOD_START -> {
                    entries[pos] = entries[pos].toString() + " \u2014 " + getString(R.string.event_periodstart)
                    if (dayPrevious != null) {
                        // If we have a previous day, then update the previous
                        // days length description
                        val length = day.date.diffDayPeriods(dayPrevious.date)
                        if (length <= maximumcyclelength) {
                            entries[pos - 1] += ("\n"
                                    + String.format(
                                    getString(R.string.event_periodlength),
                                    length.toString()))
                        } else {
                            entries[pos - 1] += String.format("\n%s", getString(R.string.event_ignored))
                        }
                    }
                }
            }
            pos++
        }
        // If we have at least one entry, update the last days length
        // description to "first entry"
        if (pos > 0) {
            entries[pos - 1] += """

                ${getString(R.string.event_periodfirst)}
                """.trimIndent()
        }


        // Set custom view
        setContentView(R.layout.activity_list)
        val listView = findViewById<ListView>(R.id.listview)
        listView.adapter = ArrayAdapter(this, R.layout.listitem,
                entries)
        listView.onItemClickListener = this

        // Activate "back button" in Action Bar if possible
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Called when the activity is destroyed
     */
    override fun onDestroy() {
        // Close database
        dbMain!!.close()
        super.onDestroy()
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Handler for opening a list item which will return to the main view
     *
     * @param adapterView The ListView where the click happened
     * @param v           The view that was clicked within the ListView
     * @param position    The position of the view in the list
     * @param id          The row id of the item that was clicked
     */
    override fun onItemClick(adapterView: AdapterView<*>?, v: View, position: Int, id: Long) {
        // Determine date of clicked item
        if (dbMain != null && position >= 0 && position < dbMain!!.dayEntries.size) {
            val selectedEntry = dbMain!!.dayEntries[position]
            val month = selectedEntry.date[Calendar.MONTH]
            val year = selectedEntry.date[Calendar.YEAR]
            val intent = intent
            intent.putExtra("month", month.toString())
            intent.putExtra("year", year.toString())
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}