/*
 * Periodical list with details activity
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
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.z5i.periodical.PeriodicalDatabase.DayEntry
import java.util.*

/**
 * Activity to handle the "List, details" command
 */
class ListDetailsActivity : AppCompatActivity(), OnItemClickListener {
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

        // Set up database and string array for the list
        dbMain = PeriodicalDatabase(context)
        dbMain!!.loadRawDataWithDetails()
        val dayList = ArrayList<DayEntry>()
        val dayIterator: Iterator<DayEntry> = dbMain!!.dayEntries.iterator()
        var day: DayEntry
        while (dayIterator.hasNext()) {
            day = dayIterator.next()
            dayList.add(0, day)
        }

        // Set custom view
        setContentView(R.layout.activity_list_details)

        // Activate "back button" in Action Bar
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        val listView = findViewById<ListView>(R.id.listview_details)
        listView.adapter = DayEntryAdapter(dayList, packageName, resources)
        listView.onItemClickListener = this
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
     * Handler for opening a list item which will return to the drawer_menu view
     *
     * @param adapterView The ListView where the click happened
     * @param v           The view that was clicked within the ListView
     * @param position    The position of the view in the list
     * @param id          The row id of the item that was clicked
     */
    override fun onItemClick(adapterView: AdapterView<*>?, v: View, position: Int, id: Long) {
        // Determine date of clicked item
        val listsize = dbMain!!.dayEntries.size
        if (position >= 0 && position < listsize) {
            val selectedEntry = dbMain!!.dayEntries[listsize - position - 1]
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