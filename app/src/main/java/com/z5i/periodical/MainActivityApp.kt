/*
 * Periodical drawer_menu activity
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
import android.app.Activity
import android.app.AlertDialog
import android.app.backup.BackupManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.mxn.soul.flowingdrawer_core.ElasticDrawer
import com.mxn.soul.flowingdrawer_core.FlowingDrawer
import com.z5i.periodical.PeriodicalDatabase.DayEntry
import com.z5i.periodical.context.ZApplication
import com.z5i.periodical.fragment.MenuListFragment
import java.text.SimpleDateFormat
import java.util.*

/**
 * The drawer_menu activity of the app
 */
class MainActivityApp : AppCompatActivity() {
    private val calButtonIds = intArrayOf(R.id.cal01, R.id.cal02, R.id.cal03,
            R.id.cal04, R.id.cal05, R.id.cal06, R.id.cal07, R.id.cal08,
            R.id.cal09, R.id.cal10, R.id.cal11, R.id.cal12, R.id.cal13,
            R.id.cal14, R.id.cal15, R.id.cal16, R.id.cal17, R.id.cal18,
            R.id.cal19, R.id.cal20, R.id.cal21, R.id.cal22, R.id.cal23,
            R.id.cal24, R.id.cal25, R.id.cal26, R.id.cal27, R.id.cal28,
            R.id.cal29, R.id.cal30, R.id.cal31, R.id.cal32, R.id.cal33,
            R.id.cal34, R.id.cal35, R.id.cal36, R.id.cal37, R.id.cal38,
            R.id.cal39, R.id.cal40, R.id.cal41, R.id.cal42)
    private val calButtonIds_2 = intArrayOf(R.id.cal01_2, R.id.cal02_2, R.id.cal03_2,
            R.id.cal04_2, R.id.cal05_2, R.id.cal06_2, R.id.cal07_2,
            R.id.cal08_2, R.id.cal09_2, R.id.cal10_2, R.id.cal11_2,
            R.id.cal12_2, R.id.cal13_2, R.id.cal14_2, R.id.cal15_2,
            R.id.cal16_2, R.id.cal17_2, R.id.cal18_2, R.id.cal19_2,
            R.id.cal20_2, R.id.cal21_2, R.id.cal22_2, R.id.cal23_2,
            R.id.cal24_2, R.id.cal25_2, R.id.cal26_2, R.id.cal27_2,
            R.id.cal28_2, R.id.cal29_2, R.id.cal30_2, R.id.cal31_2,
            R.id.cal32_2, R.id.cal33_2, R.id.cal34_2, R.id.cal35_2,
            R.id.cal36_2, R.id.cal37_2, R.id.cal38_2, R.id.cal39_2,
            R.id.cal40_2, R.id.cal41_2, R.id.cal42_2)
    private val STATE_MONTH = "month"
    private val STATE_YEAR = "year"
    private var gestureDetector: GestureDetector? = null
    private var viewCurrent = R.id.calendar
    private var monthCurrent = 0
    private var yearCurrent = 0

    /* First day of the week (0 = sunday) */
    private var firstDayOfWeek = 0
    private var dbMain: PeriodicalDatabase? = null

    /* Status of the drawer_menu navigartion drawer */
    private var navigationDrawerActive = false

    private lateinit var flipper: ViewFlipper
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext!!

        // Setup drawer_menu view with navigation drawer
        setContentView(R.layout.activity_main)
        this.flipper = findViewById(R.id.mainwidget)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<FlowingDrawer>(R.id.drawer_layout)
        //val toggle = ActionBarDrawerToggle(
        //        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        //drawer.addDrawerListener(toggle)
        //toggle.syncState()
        drawer.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL)

        // Listener to detect when the navigation drawer is opening, so we
        // avoid the drawer_menu view to handle the swipe of the navigation drawer
        drawer.setOnDrawerStateChangeListener(object : ElasticDrawer.OnDrawerStateChangeListener {
            override fun onDrawerStateChange(oldState: Int, newState: Int) {
                this@MainActivityApp.navigationDrawerActive = if (newState == 0) false else true
            }

            override fun onDrawerSlide(openRatio: Float, offsetPixels: Int) {
                this@MainActivityApp.navigationDrawerActive = true
            }
        })
        this.setupFragment(MenuListFragment())

        // Setup gesture handling
        gestureDetector = GestureDetector(context, CalendarGestureDetector())
        val gestureListener = OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> gestureDetector!!.onTouchEvent(event)
                MotionEvent.ACTION_UP -> v.performClick()
                else -> {
                }
            }
            true
        }

        // Setup database
        dbMain = PeriodicalDatabase(context)

        // Restore preferences from database to make sure, we got the correct datatypes
        dbMain!!.restorePreferences()

        // If savedInstanceState exists, restore the last
        // instance state, otherwise use current month as start value
        if (savedInstanceState == null) {
            initMonth()
        } else {
            monthCurrent = savedInstanceState.getInt(STATE_MONTH)
            yearCurrent = savedInstanceState.getInt(STATE_YEAR)
        }

        // Update calculated values
        dbMain!!.loadCalculatedData()
    }

    /**
     * Called when the activity starts interacting with the user
     */
    override fun onResume() {
        super.onResume()

        // Update calendar view
        calendarUpdate()
    }

    /**
     * Called to save the current instance state
     *
     * @param outState Bundle to place the saved state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_MONTH, monthCurrent)
        outState.putInt(STATE_YEAR, yearCurrent)
    }

    /**
     * Called when the activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        if (dbMain != null) dbMain!!.close()
    }

    /**
     * Close draw when pressing "back"
     */
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().add(R.id.id_container_menu, fragment).commit()
    }

    /**
     * Handler for "Help" menu action
     */
    internal fun showHelp() {
        startActivityForResult(
                Intent(this@MainActivityApp, HelpActivity::class.java), HELP_CLOSED)
    }

    /**
     * Handler for "About" menu action
     */
    internal fun showAbout() {
        startActivityForResult(
                Intent(this@MainActivityApp, AboutActivity::class.java), ABOUT_CLOSED)
    }

    /**
     * Handler for "List" menu action
     */
    internal fun showList() {
        startActivityForResult(
                Intent(this@MainActivityApp, ListActivity::class.java), PICK_DATE)
    }

    /**
     * Handler for "List, details" menu action
     */
    internal fun showListDetails() {
        startActivityForResult(
                Intent(this@MainActivityApp, ListDetailsActivity::class.java), PICK_DATE)
    }

    /**
     * Handler for "Options" menu action
     */
    internal fun showOptions() {
        startActivityForResult(
                Intent(this@MainActivityApp, PreferenceActivity::class.java), SET_OPTIONS)
    }

    //创建toolbar的菜单选项
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    //分派菜单选项点击事件的响应
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.buttonprev -> {
                this.goPrev(null)
            }
            R.id.buttontoday -> {
                this.goCurrent(null)
            }
            R.id.buttonnext -> {
                this.goNext(null)
            }
        }

        return true
    }

    /**
     * Update calendar data and view
     */
    @SuppressLint("DefaultLocale")
    private fun calendarUpdate() {
        val context = applicationContext!!

        // Initialize control ids for the target view to be used
        val calendarCells: IntArray
        calendarCells = if (viewCurrent == R.id.calendar) {
            calButtonIds
        } else {
            calButtonIds_2
        }
        val preferences = PreferenceUtils(context)

        // Set weekday labels depending on selected start of week
        val startofweek = preferences.getInt("startofweek", 0)
        if (startofweek == 0) {
            findViewById<View>(R.id.rowcaldays0).visibility = View.VISIBLE
            findViewById<View>(R.id.rowcaldays0_2).visibility = View.VISIBLE
            findViewById<View>(R.id.rowcaldays1).visibility = View.GONE
            findViewById<View>(R.id.rowcaldays1_2).visibility = View.GONE
        } else {
            findViewById<View>(R.id.rowcaldays0).visibility = View.GONE
            findViewById<View>(R.id.rowcaldays0_2).visibility = View.GONE
            findViewById<View>(R.id.rowcaldays1).visibility = View.VISIBLE
            findViewById<View>(R.id.rowcaldays1_2).visibility = View.VISIBLE
        }

        // Show day of cycle?
        val show_cycle = preferences.getBoolean("show_cycle", true)

        // Create calendar object for current month
        val cal = GregorianCalendar(yearCurrent, monthCurrent - 1, 1)

        // Output current year/month
        val displayDate = findViewById<TextView>(R.id.displaydate)
        @SuppressLint("SimpleDateFormat") val dateFormat = SimpleDateFormat("MMMM yyyy")
        displayDate.text = String.format("%s\n平均周期%d 最短周期↓%d 最长周期↑%d",
                dateFormat.format(cal.time),
                dbMain!!.cycleAverage, dbMain!!.cycleShortest,
                dbMain!!.cycleLongest)
        displayDate.contentDescription = String.format("%s - %s %d - %s %d - %s %d",
                dateFormat.format(cal.time),
                resources.getString(R.string.label_average_cycle), dbMain!!.cycleAverage,
                resources.getString(R.string.label_shortest_cycle), dbMain!!.cycleShortest,
                resources.getString(R.string.label_longest_cycle), dbMain!!.cycleLongest)

        // Calculate first week day of month
        firstDayOfWeek = cal[Calendar.DAY_OF_WEEK]
        val daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // If the week should start on monday, adjust the first day of the month,
        // so every day moves one position to the left and sunday gets to the end
        if (startofweek == 1) {
            firstDayOfWeek--
            if (firstDayOfWeek == 0) firstDayOfWeek = 7
        }
        val calToday = GregorianCalendar()
        val dayToday = calToday[GregorianCalendar.DATE]
        val monthToday = calToday[GregorianCalendar.MONTH] + 1
        val yearToday = calToday[GregorianCalendar.YEAR]

        // Adjust calendar elements
        for (i in 1..42) {
            val cell = findViewById<CalendarCell>(calendarCells[i - 1])
            if (i < firstDayOfWeek || i >= firstDayOfWeek + daysCount) {
                cell.visibility = View.INVISIBLE
                // TODO Display days of previous/next month as "disabled" buttons
            } else {
                // This cell is part of the current month,
                // label text is the day of the month
                val day = i - firstDayOfWeek + 1
                cell.text = String.format("%d", day)
                cell.visibility = View.VISIBLE
                val entry = dbMain!!.getEntry(cal)
                var current = false
                if (day == dayToday && monthCurrent == monthToday && yearCurrent == yearToday) {
                    current = true
                }

                // Set other button attributes
                cell.year = yearCurrent
                cell.month = monthCurrent
                cell.day = day
                cell.setCurrent(current)
                cell.setIntercourse(false)
                cell.setNotes(false)
                if (entry != null) {
                    cell.setType(entry.type)
                    cell.setDayofcycle(if (show_cycle) entry.dayofcycle else 0)
                    cell.setIntensity(entry.intensity)
                    for (s in entry.symptoms) {
                        if (s == 1) cell.setIntercourse(true) else cell.setNotes(true)
                    }
                    if (entry.notes.isNotEmpty()) cell.setNotes(true)
                } else {
                    cell.setType(DayEntry.EMPTY)
                    cell.setDayofcycle(0)
                }

                // Set content description for TalkBack
                cell.updateContentDescription()
                cal.add(GregorianCalendar.DATE, 1)
            }
        }
    }

    /**
     * Handler for "previous month" button in drawer_menu view
     */
    fun goPrev(v: View?) {
        // Update calendar
        monthCurrent--
        if (monthCurrent < 1) {
            monthCurrent = 12
            yearCurrent--
        }
        viewCurrent = if (viewCurrent == R.id.calendar) {
            R.id.calendar_2
        } else {
            R.id.calendar
        }
        calendarUpdate()

        // 显示滑动动画，从左到右
        this.flipper.inAnimation = AnimationHelper.inFromLeftAnimation()
        this.flipper.outAnimation = AnimationHelper.outToRightAnimation()
        this.flipper.showNext()

        AnimationHelper.goPrevious()
    }

    /**
     * Handler for "next month" button in drawer_menu view
     */
    fun goNext(v: View?) {
        // Update calendar
        monthCurrent++
        if (monthCurrent > 12) {
            monthCurrent = 1
            yearCurrent++
        }
        viewCurrent = if (viewCurrent == R.id.calendar) {
            R.id.calendar_2
        } else {
            R.id.calendar
        }
        calendarUpdate()

        // 显示滑动动画，从右到左
        flipper.inAnimation = AnimationHelper.inFromRightAnimation()
        flipper.outAnimation = AnimationHelper.outToLeftAnimation()
        flipper.showPrevious()

        AnimationHelper.goNext()
    }

    /**
     * Handler for "current" button in drawer_menu view
     */
    fun goCurrent(v: View?) {
        initMonth()
        calendarUpdate()

        if(AnimationHelper.backFromNext()) {
            Log.d("MainActivityApp", "backfromnext" + AnimationHelper.getCounter().toString())
            while (AnimationHelper.getCounter() != 0) {
                this.goPrev(null)
            }
            Log.d("MainActivityApp", "backfromnext" + AnimationHelper.getCounter().toString())
        }

        if(AnimationHelper.backFromPrev()) {
            Log.d("MainActivityApp", "backfromprev" + AnimationHelper.getCounter().toString())
            while (AnimationHelper.getCounter() != 0) {
                this.goNext(null)
            }
            Log.d("MainActivityApp", "backfromnext" + AnimationHelper.getCounter().toString())
        }
    }

    /**
     * Change to current month
     */
    private fun initMonth() {
        val cal: Calendar = GregorianCalendar()
        monthCurrent = cal[Calendar.MONTH] + 1
        yearCurrent = cal[Calendar.YEAR]
    }

    /**
     * Handler for "backup" menu action
     */
    internal fun doBackup() {
        val context = applicationContext!!
        val preferences = PreferenceUtils(context)
        val backupUriString = preferences.getString("backup_uri", "")
        if (backupUriString != "") {
            // The backup location is already selected, just use this
            val uriBackup = Uri.parse(backupUriString)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.backup_title))
            builder.setMessage(resources.getString(R.string.backup_text))
            builder.setIcon(R.drawable.ic_warning_black_40dp)
            builder.setPositiveButton(resources.getString(R.string.backup_ok)
            ) { dialog, which ->
                val ok = dbMain!!.backupToUri(context, uriBackup)
                val text: String
                text = if (ok) {
                    resources.getString(R.string.backup_finished)
                } else {
                    resources.getString(R.string.backup_failed)
                }
                val toast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
                toast.show()
            }
            builder.setNeutralButton(
                    resources.getString(R.string.backup_newfolder)
            ) { dialog, which ->
                dbMain!!.setOption("backup_uri", "")
                dbMain!!.restorePreferences()
                doBackup()
            }
            builder.setNegativeButton(
                    resources.getString(R.string.backup_cancel)
            ) { dialog, which -> }
            builder.show()
        } else {
            // There is no backup location stored yet, ask the user to select one
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.backup_title))
            builder.setMessage(resources.getString(R.string.backup_selectfolder))
            builder.setIcon(R.drawable.ic_warning_black_40dp)
            builder.setPositiveButton(resources.getString(R.string.backup_ok)
            ) { dialog, which ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                startActivityForResult(intent, STORAGE_ACCESS_SELECTED_BACKUP)
            }
            builder.setNeutralButton(
                    resources.getString(R.string.backup_help)
            ) { dialog, which -> showHelp() }
            builder.setNegativeButton(
                    resources.getString(R.string.backup_cancel)
            ) { dialog, which -> }
            builder.show()
        }
    }

    /**
     * Handler for "restore" menu action
     */
    internal fun doRestore() {
        val context = applicationContext!!
        val preferences = PreferenceUtils(context)
        val backupUriString = preferences.getString("backup_uri", "")
        if (backupUriString != "") {
            // The backup folder is already selected, just use this
            val uriBackup = Uri.parse(backupUriString)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.restore_title))
            builder.setMessage(resources.getString(R.string.restore_text))
            builder.setIcon(R.drawable.ic_warning_black_40dp)
            builder.setPositiveButton(
                    resources.getString(R.string.restore_ok)
            ) { dialog, which ->
                val ok = dbMain!!.restoreFromUri(context, uriBackup)
                dbMain!!.loadCalculatedData()
                calendarUpdate()
                val text: String
                text = if (ok) {
                    resources.getString(R.string.restore_finished)
                } else {
                    resources.getString(R.string.restore_failed)
                }
                val toast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
                toast.show()
            }
            builder.setNeutralButton(
                    resources.getString(R.string.backup_newfolder)
            ) { dialog, which ->
                dbMain!!.setOption("backup_uri", "")
                dbMain!!.restorePreferences()
                doRestore()
            }
            builder.setNegativeButton(
                    resources.getString(R.string.restore_cancel)
            ) { dialog, which -> }
            builder.show()
        } else {
            // There is no backup location stored yet, ask the user to select one
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.restore_title))
            builder.setMessage(resources.getString(R.string.restore_selectfolder))
            builder.setIcon(R.drawable.ic_warning_black_40dp)
            builder.setPositiveButton(resources.getString(R.string.backup_ok)
            ) { dialog, which ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                startActivityForResult(intent, STORAGE_ACCESS_SELECTED_RESTORE)
            }
            builder.setNeutralButton(
                    resources.getString(R.string.backup_help)
            ) { dialog, which -> showHelp() }
            builder.setNegativeButton(
                    resources.getString(R.string.backup_cancel)
            ) { dialog, which -> }
            builder.show()
        }
    }

    /**
     * Handler for the selection of one day in the calendar
     */
    fun handleCalendarButton(v: View) {
        val context = ZApplication.context

        // Determine selected date
        val idButton = v.id
        var nButtonClicked = 0
        val calButtonIds: IntArray
        calButtonIds = if (viewCurrent == R.id.calendar) {
            this.calButtonIds
        } else {
            calButtonIds_2
        }
        while (nButtonClicked < 42) {
            if (calButtonIds[nButtonClicked] == idButton) break
            nButtonClicked++
        }
        val day = nButtonClicked - firstDayOfWeek + 2

        // If "direct details" is set by the user, just open the details
        val preferences = PreferenceUtils(context)
        if (preferences.getBoolean("direct_details", false)) {
            showDetailsActivity(yearCurrent, monthCurrent, day)
        } else {
            // Set or remove entry with confirmation
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources
                    .getString(R.string.calendaraction_title))
            val date = GregorianCalendar(yearCurrent, monthCurrent - 1, day)
            val type = dbMain!!.getEntryType(date)
            if (type != DayEntry.PERIOD_START && type != DayEntry.PERIOD_CONFIRMED) {
                builder.setMessage(resources.getString(
                        R.string.calendaraction_add))
                builder.setPositiveButton(
                        resources.getString(R.string.calendaraction_ok)
                ) { dialog, which ->
                    dbMain!!.addPeriod(date)
                    databaseChanged()
                }
                builder.setNegativeButton(
                        resources.getString(R.string.calendaraction_cancel)
                ) { dialog, which -> }
                builder.setNeutralButton(
                        resources.getString(R.string.calendaraction_details)
                ) { dialog, which -> showDetailsActivity(yearCurrent, monthCurrent, day) }
            } else {
                if (type == DayEntry.PERIOD_START) builder.setMessage(resources.getString(R.string.calendaraction_removeperiod)) else builder.setMessage(resources.getString(R.string.calendaraction_remove))
                builder.setPositiveButton(
                        resources.getString(R.string.calendaraction_ok)
                ) { dialog, which ->
                    dbMain!!.removePeriod(date)
                    databaseChanged()
                }
                builder.setNegativeButton(
                        resources.getString(R.string.calendaraction_cancel)
                ) { dialog, which -> }
                builder.setNeutralButton(
                        resources.getString(R.string.calendaraction_details)
                ) { dialog, which -> showDetailsActivity(yearCurrent, monthCurrent, day) }
            }
            builder.show()
        }
    }

    /**
     * Helper to show the details activity for a specific day
     */
    private fun showDetailsActivity(year: Int, month: Int, day: Int) {
        val details = Intent(this@MainActivityApp, DetailsActivity::class.java)
        details.putExtra("year", year)
        details.putExtra("month", month)
        details.putExtra("day", day)
        startActivityForResult(details, DETAILS_CLOSED)
    }

    /**
     * Helper to handle changes in the database
     */
    private fun databaseChanged() {
        // Update calculated values
        dbMain!!.loadCalculatedData()
        calendarUpdate()

        // Notify backup agent about the change and mark DB as clean
        val bm = BackupManager(this)
        bm.dataChanged()
    }

    /**
     * Handler of activity results (detail list, options)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var storageUri: Uri? = null
        when (requestCode) {
            PICK_DATE -> if (resultCode == RESULT_OK) {
                val extras = data!!.extras
                if (extras != null) {
                    monthCurrent = extras.getString("month")!!.toInt() + 1
                    yearCurrent = extras.getString("year")!!.toInt()
                    calendarUpdate()
                }
            }
            SET_OPTIONS -> {
                databaseChanged()
                calendarUpdate()
            }
            DETAILS_CLOSED -> {
                dbMain!!.loadCalculatedData()
                calendarUpdate()
            }
            STORAGE_ACCESS_SELECTED_BACKUP -> if (data != null) {
                storageUri = data.data
                dbMain!!.setOption("backup_uri", storageUri.toString())
                dbMain!!.restorePreferences()
                doBackup()
            }
            STORAGE_ACCESS_SELECTED_RESTORE -> if (data != null) {
                storageUri = data.data
                dbMain!!.setOption("backup_uri", storageUri.toString())
                dbMain!!.restorePreferences()
                doRestore()
            }
        }
    }

    /**
     * Touch dispatcher to pass events to the gesture detector to detect swipes on the UI
     */
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        val result = super.dispatchTouchEvent(e)

        // Only dispatch touch event to gesture detector,
        // if the navigation drawer is not active (opening, closing etc.)
        return if (!navigationDrawerActive) {
            gestureDetector!!.onTouchEvent(e)
        } else result
    }

    /**
     * Gesture detector to handle swipes on the UI
     */
    private inner class CalendarGestureDetector : SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                             velocityY: Float): Boolean {
            val SWIPE_MIN_DISTANCE = 120
            val SWIPE_MAX_OFF_PATH = 250
            val SWIPE_THRESHOLD_VELOCITY = 200
            try {
                // if swipe is not straight enough then ignore
                if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) {
                    return false
                }
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goNext(null)
                } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goPrev(null)
                }
            } catch (e: Exception) {
                // nothing
            }
            return false
        }
    }

    companion object {
        private const val PERMISSION_CONFIRM_BACKUP = 1
        private const val PERMISSION_CONFIRM_RESTORE = 2

        /* Request codes for other activities */
        private const val PICK_DATE = 1 // Detail list: Date selected in detail list
        private const val SET_OPTIONS = 2 // Preferences: Options changed
        private const val HELP_CLOSED = 3 // Help: closed
        private const val ABOUT_CLOSED = 4 // About: closed
        private const val DETAILS_CLOSED = 5 // Details: closed
        private const val STORAGE_ACCESS_SELECTED_BACKUP = 6 // Location for backup selected for backup
        private const val STORAGE_ACCESS_SELECTED_RESTORE = 7 // Location for backup selected for restore
    }
}