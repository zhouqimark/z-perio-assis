/*
 * Periodical database class
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
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Database of the app
 */
@SuppressLint("DefaultLocale")
internal class PeriodicalDatabase(
        /**
         * Private reference to application context
         */
        private val context: Context) {
    @JvmField
    val DEFAULT_PERIOD_LENGTH = 4
    @JvmField
    val DEFAULT_LUTEAL_LENGTH = 14
    @JvmField
    val DEFAULT_CYCLE_LENGTH = 183
    @JvmField
    val DEFAULT_START_OF_WEEK = 0
    @JvmField
    val DEFAULT_DIRECT_DETAILS = false
    @JvmField
    val DEFAULT_SHOW_CYCLE = true

    val DATABASE_NAME = "main.db"
    val DATABASE_VERSION = 4

    private var db: SQLiteDatabase? = null

    /**
     * 数据库帮助类
     */
    private inner class PeriodicalDataOpenHelper
    /**
     * Create a new database for the app
     *
     * @param context Application context
     */
    internal constructor(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        /**
         * Create tables if needed
         *
         * @param db The database
         */
        override fun onCreate(db: SQLiteDatabase) {
            db.beginTransaction()
            db.execSQL("create table data (" +
                    "eventtype integer(3), " +
                    "eventdate varchar(8), " +
                    "eventcvx integer(3), " +
                    "eventtemp real, " +
                    "intensity integer(3)" +
                    ");")
            db.execSQL("create table options (" +
                    "name varchar(100), " +
                    "value varchar(500)" +
                    ");")
            db.execSQL("create table notes (" +
                    "eventdate varchar(8), " +
                    "content text" +
                    ");")
            db.execSQL("create table symptoms (" +
                    "eventdate varchar(8), " +
                    "symptom integer(3)" +
                    ");")
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        /**
         * Execute schema updates if needed
         *
         * @param db         The database
         * @param oldVersion The old version which is being updated
         * @param newVersion The new version to update to
         */
        @SuppressLint("DefaultLocale")
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2 && newVersion >= 2) {
                // Version 2 introduces additional data columns
                db.beginTransaction()
                db.execSQL("alter table data add column eventcvx integer(3)")
                db.execSQL("alter table data add column eventtemp real")
                db.setTransactionSuccessful()
                db.endTransaction()
            }
            if (oldVersion < 3 && newVersion >= 3) {
                // Version 3 introduces options
                db.beginTransaction()
                db.execSQL("create table options (" +
                        "name varchar(100), " +
                        "value varchar(500)" +
                        ");")
                db.setTransactionSuccessful()
                db.endTransaction()
            }
            if (oldVersion < 4 && newVersion >= 4) {
                // Version 4 introduces details and stores periods in a different way
                db.beginTransaction()
                db.execSQL("create table notes (" +
                        "eventdate varchar(8), " +
                        "content text" +
                        ");")
                db.execSQL("create table symptoms (" +
                        "eventdate varchar(8), " +
                        "symptom integer(3)" +
                        ");")

                // We don't need a primary ID column any longer but add intensity as property
                db.execSQL("alter table data add column intensity integer(3)")
                db.execSQL("alter table data rename to data_old;")
                db.execSQL("create table data (" +
                        "eventtype integer(3), " +
                        "eventdate varchar(8), " +
                        "eventcvx integer(3), " +
                        "eventtemp real, " +
                        "intensity integer(3) " +
                        ");")
                db.execSQL("insert into data (eventtype, eventdate) " +
                        "select eventtype, eventdate from data_old;")
                db.execSQL("drop table data_old;")

                // Create records for existing confirmed period entries
                // based on the global period length setting
                val preferences = PreferenceUtils(context)
                val periodlength: Int
                periodlength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH)
                var statement: String

                // Workaround for a bug introduced in release 0.35 which stored the
                // maximum cycle as "period length", so it is not usable at all :-(
                val option = "maximum_cycle_length"
                val editor = preferences.edit()
                editor.putInt(option, DEFAULT_CYCLE_LENGTH)
                editor.apply()
                statement = "delete from options where name = ?"
                db.execSQL(statement, arrayOf(option))
                statement = "insert into options (name, value) values (?, ?)"
                db.execSQL(statement, arrayOf(option, DEFAULT_CYCLE_LENGTH.toString()))

                // Fill database with additional entries for the period days
                statement = "select eventtype, eventdate from data order by eventdate desc"
                val result = db.rawQuery(statement, null)
                while (result.moveToNext()) {
                    var eventtype = result.getInt(0)
                    val dbdate = result.getString(1)!!
                    val eventyear = dbdate.substring(0, 4).toInt(10)
                    val eventmonth = dbdate.substring(4, 6).toInt(10)
                    val eventday = dbdate.substring(6, 8).toInt(10)
                    val eventdate = GregorianCalendar(eventyear,
                            eventmonth - 1, eventday)

                    // Add default intensity for existing period
                    var intensity = 2
                    statement = String.format(
                            "update data set intensity=%d where eventdate='%s'",
                            intensity, String.format(Locale.getDefault(), "%04d%02d%02d",
                            eventdate[GregorianCalendar.YEAR],
                            eventdate[GregorianCalendar.MONTH] + 1,
                            eventdate[GregorianCalendar.DAY_OF_MONTH]))
                    db.execSQL(statement)

                    // Add additional entries for each day of the period
                    if (eventtype == DayEntry.PERIOD_START) {
                        eventtype = DayEntry.PERIOD_CONFIRMED

                        // Start second day with higher intensity which will be reduced every day
                        intensity = 4
                        for (day in 2..periodlength) {
                            eventdate.add(GregorianCalendar.DATE, 1)
                            statement = String.format(
                                    "insert into data (eventdate, eventtype, intensity) values ('%s', %d, %d)", String.format(Locale.getDefault(), "%04d%02d%02d",
                                    eventdate[GregorianCalendar.YEAR],
                                    eventdate[GregorianCalendar.MONTH] + 1,
                                    eventdate[GregorianCalendar.DAY_OF_MONTH]),
                                    eventtype,
                                    intensity)
                            db.execSQL(statement)
                            if (intensity > 1) intensity--
                        }
                    }
                }
                result.close()
                db.setTransactionSuccessful()
                db.endTransaction()
            }
        }
    }

    /**
     * 推算日历条目管理员
     */
    class DayEntry {
        var type: Int
        val date: GregorianCalendarExt
        var dayofcycle: Int
        var intensity: Int
        var notes: String
        var symptoms: MutableList<Int>

        /**
         * Construct a new day entry with parameters
         *
         * @param type       Entry type (DayEntry.EMPTY, DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED, ...)
         * @param date       Entry date
         * @param dayofcycle Day within current cycle (beginning with 1)
         * @param intensity  Intensity of the period (1-4)
         */
        internal constructor(type: Int, date: GregorianCalendar, dayofcycle: Int, intensity: Int) {
            this.type = type
            this.date = GregorianCalendarExt()
            this.date.time = date.time
            this.dayofcycle = dayofcycle
            this.intensity = intensity
            notes = ""
            symptoms = ArrayList()
        }

        /**
         * Construct a new day entry
         */
        internal constructor() {
            type = EMPTY
            date = GregorianCalendarExt()
            dayofcycle = 0
            intensity = 1
            notes = ""
            symptoms = ArrayList()
        }

        companion object {
            const val EMPTY = 0
            const val PERIOD_START = 1
            const val PERIOD_CONFIRMED = 2
            const val PERIOD_PREDICTED = 3
            const val FERTILITY_PREDICTED = 4
            const val OVULATION_PREDICTED = 5
            const val FERTILITY_FUTURE = 6
            const val OVULATION_FUTURE = 7
            const val INFERTILE_PREDICTED = 8
            const val INFERTILE_FUTURE = 9
        }
    }

    /**
     * Calculated day entries
     */
    val dayEntries: ArrayList<DayEntry>

    /**
     * 平均周期长度
     */
    var cycleAverage = 0

    /**
     * 最长周期长度
     */
    var cycleLongest = 0

    /**
     * 最短周期长度
     */
    var cycleShortest = 0

    /**
     * 打开数据库
     */
    @SuppressLint("Recycle")
    private fun open() {
        val dataOpenHelper = PeriodicalDataOpenHelper(context)
        this.db = dataOpenHelper.writableDatabase
        if (BuildConfig.DEBUG && db == null) {
            error("Assertion failed")
        }
    }

    /**
     * 关闭数据库
     */
    fun close() {
        db?.close()
    }

    /**
     * Add a period entry for a specific day to the database
     *
     * @param date Date of the entry
     */
    @SuppressLint("DefaultLocale")
    fun addPeriod(date: GregorianCalendar) {
        var statement: String
        val dateLocal = GregorianCalendar()
        dateLocal.time = date.time
        dateLocal.add(GregorianCalendar.DATE, -1)
        var type = getEntryType(dateLocal)
        if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
            // The day before was a confirmed day of the period, then add the current day
            val datestring = String.format(Locale.getDefault(), "%04d%02d%02d",
                    date[GregorianCalendar.YEAR],
                    date[GregorianCalendar.MONTH] + 1,
                    date[GregorianCalendar.DAY_OF_MONTH])
            type = DayEntry.PERIOD_CONFIRMED
            db!!.beginTransaction()
            statement = String.format(
                    "delete from data where eventdate = '%s'",
                    datestring)
            db!!.execSQL(statement)
            statement = String.format(
                    "insert into data (eventdate, eventtype, intensity) values ('%s', %d, 1)",
                    datestring,
                    type)
            db!!.execSQL(statement)
            db!!.setTransactionSuccessful()
            db!!.endTransaction()
        } else {
            // Probably start a new period
            val dateString = String.format(Locale.getDefault(), "%04d%02d%02d",
                    date[GregorianCalendar.YEAR],
                    date[GregorianCalendar.MONTH] + 1,
                    date[GregorianCalendar.DAY_OF_MONTH])
            dateLocal.time = date.time
            dateLocal.add(GregorianCalendar.DATE, 1)
            type = getEntryType(dateLocal)
            if (type == DayEntry.PERIOD_START) {
                // The next day is already marked as new period then move the period start
                // to this day
                db!!.beginTransaction()

                // First insert a new start
                statement = String.format(
                        "delete from data where eventdate = '%s'",
                        dateString)
                db!!.execSQL(statement)
                statement = String.format(
                        "insert into data (eventdate, eventtype, intensity) values ('%s', %d, 2)",
                        dateString,
                        type)
                db!!.execSQL(statement)

                // Update old start to be a confirmed day
                statement = String.format(
                        "update data set eventtype=%d where eventdate = '%s'",
                        DayEntry.PERIOD_CONFIRMED, String.format(Locale.getDefault(), "%04d%02d%02d",
                        dateLocal[GregorianCalendar.YEAR],
                        dateLocal[GregorianCalendar.MONTH] + 1,
                        dateLocal[GregorianCalendar.DAY_OF_MONTH]))
                db!!.execSQL(statement)
                db!!.setTransactionSuccessful()
                db!!.endTransaction()
            } else {
                // This day is a regular new period
                val periodLength: Int
                val preferences = PreferenceUtils(context)
                periodLength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH)
                type = DayEntry.PERIOD_START
                dateLocal.time = date.time
                var intensity = 2
                db!!.beginTransaction()
                for (day in 0 until periodLength) {
                    val datestringlocal = String.format(Locale.getDefault(), "%04d%02d%02d",
                            dateLocal[GregorianCalendar.YEAR],
                            dateLocal[GregorianCalendar.MONTH] + 1,
                            dateLocal[GregorianCalendar.DAY_OF_MONTH])
                    statement = String.format(
                            "insert into data (eventdate, eventtype, intensity) values ('%s', %d, %d)",
                            datestringlocal,
                            type,
                            intensity)
                    db!!.execSQL(statement)
                    type = DayEntry.PERIOD_CONFIRMED

                    // Second day gets a higher intensity, the following ones decrease it every day
                    if (day == 0) intensity = 4 else {
                        if (intensity > 1) intensity--
                    }
                    dateLocal.add(GregorianCalendar.DATE, 1)
                }
                db!!.setTransactionSuccessful()
                db!!.endTransaction()
            }
        }
    }

    /**
     * Remove an entry for a specific day from the database
     *
     * @param date Date of the entry
     */
    fun removePeriod(date: GregorianCalendar) {
        var statement: String
        val dateLocal = GregorianCalendar()
        dateLocal.time = date.time
        db!!.beginTransaction()

        // Remove period entry for the selected and all following days
        while (true) {
            val type = getEntryType(dateLocal)
            if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
                statement = String.format(
                        "delete from data where eventdate = '%s'", String.format(Locale.getDefault(), "%04d%02d%02d",
                        dateLocal[GregorianCalendar.YEAR],
                        dateLocal[GregorianCalendar.MONTH] + 1,
                        dateLocal[GregorianCalendar.DAY_OF_MONTH]))
                db!!.execSQL(statement)
                dateLocal.add(GregorianCalendar.DATE, 1)
            } else {
                break
            }
        }
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    /**
     * Update the calculation based on the entries in the database
     */
    @SuppressLint("DefaultLocale")
    fun loadCalculatedData() {
        var entry: DayEntry? = null
        var entryPrevious: DayEntry? = null
        var entryPreviousStart: DayEntry? = null
        var isFirst = true
        var count = 0
        var countlimit = 1
        cycleAverage = 0
        cycleLongest = 28
        cycleShortest = 28
        var ovulationday = 0
        var result: Cursor
        val periodlength: Int
        val luteallength: Int
        var maximumcyclelength: Int
        var dayofcycle = 1

        // Get default values from preferences
        val preferences = PreferenceUtils(context)
        periodlength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH)
        luteallength = preferences.getInt("luteal_length", DEFAULT_LUTEAL_LENGTH)
        maximumcyclelength = preferences.getInt("maximum_cycle_length", DEFAULT_CYCLE_LENGTH)

        // Just a safety measure: limit maximum cycle lengths to the allowed minimum value
        if (maximumcyclelength < 60) maximumcyclelength = 60

        // Clean up existing data
        dayEntries.clear()

        // Determine minimum entry count for
        // shortest/longest period calculation
        result = db!!.rawQuery(String.format("select count(*) from data where eventtype = %d", DayEntry.PERIOD_START), null)
        if (result.moveToNext()) {
            countlimit = result.getInt(0)
            countlimit -= 13
            if (countlimit < 1) countlimit = 1
        }
        result.close()

        // Get all period related entries from the database to fill the calendar
        result = db!!.rawQuery(String.format("select eventdate, eventtype, intensity from data " +
                "where " +
                "eventtype in(%d, %d) order by eventdate",
                DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED),
                null)
        while (result.moveToNext()) {
            val dbdate = result.getString(0)
            val eventtype = result.getInt(1)
            val intensity = result.getInt(2)
            val eventyear = dbdate.substring(0, 4).toInt(10)
            val eventmonth = dbdate.substring(4, 6).toInt(10)
            val eventday = dbdate.substring(6, 8).toInt(10)
            val eventdate = GregorianCalendar(eventyear,
                    eventmonth - 1, eventday)
            when (eventtype) {
                DayEntry.PERIOD_START -> if (isFirst) {
                    // First event at all - just create an initial start entry
                    dayofcycle = 1
                    entryPrevious = DayEntry(eventtype, eventdate, 1, intensity)
                    entryPreviousStart = entryPrevious
                    dayEntries.add(entryPrevious)
                    isFirst = false
                } else {
                    // Create new day entry
                    entry = DayEntry(eventtype, eventdate, 1, intensity)
                    val length = entryPreviousStart!!.date.diffDayPeriods(entry.date)

                    // Add calculated values from the last date to this day, if the period has not
                    // unusual lengths (e.g. after a longer pause because of pregnancy etc.)
                    if (length <= maximumcyclelength) {
                        count++

                        // Update values which are used to calculate the fertility
                        // window for the last 12 entries
                        if (count == countlimit) {
                            // If we have at least one period the shortest and
                            // and longest value is automatically the current length
                            cycleShortest = length
                            cycleLongest = length
                        } else if (count > countlimit) {
                            // We have more than two values, then update
                            // longest/shortest
                            // values
                            if (length < cycleShortest) cycleShortest = length
                            if (length > cycleLongest) cycleLongest = length
                        }

                        // Update average sum
                        cycleAverage += length

                        // Calculate a predicted ovulation date
                        var average = cycleAverage
                        if (count > 0) average /= count
                        ovulationday = length - luteallength

                        // Calculate days from the last event until now
                        val datePrevious = GregorianCalendar()
                        datePrevious.time = entryPrevious!!.date.time
                        var day = dayofcycle
                        while (day < length) {
                            datePrevious.add(GregorianCalendar.DATE, 1)
                            dayofcycle++
                            var type: Int
                            type = if (day == ovulationday) {
                                // Day of ovulation
                                DayEntry.OVULATION_PREDICTED
                            } else if (day >= cycleShortest - luteallength - 4
                                    && day <= cycleLongest - luteallength + 3) {
                                // Fertile days
                                DayEntry.FERTILITY_PREDICTED
                            } else {
                                // Infertile days
                                DayEntry.INFERTILE_PREDICTED
                            }
                            val entryCalculated = DayEntry(type, datePrevious, dayofcycle, 1)
                            dayEntries.add(entryCalculated)
                            day++
                        }
                    }

                    // Finally add the entry
                    dayofcycle = 1
                    entryPrevious = entry
                    entryPreviousStart = entry
                    dayEntries.add(entry)
                }
                DayEntry.PERIOD_CONFIRMED -> {
                    dayofcycle++
                    entry = DayEntry(eventtype, eventdate, dayofcycle, intensity)
                    dayEntries.add(entry)
                    entryPrevious = entry
                }
            }
        }
        result.close()

        // Calculate global average and prediction if possible
        if (count > 0) {
            cycleAverage /= count
            val datePredicted = GregorianCalendar()
            datePredicted.time = entry!!.date.time
            dayofcycle++
            for (cycles in 0..2) {
                for (day in (if (cycles == 0) dayofcycle else 1)..cycleAverage) {
                    datePredicted.add(GregorianCalendar.DATE, 1)
                    var type: Int
                    type = if (day <= periodlength) {
                        // Predicted days of period
                        DayEntry.PERIOD_PREDICTED
                    } else if (day == ovulationday) {
                        // Day of ovulation
                        DayEntry.OVULATION_FUTURE
                    } else if (day >= cycleShortest - luteallength - 4
                            && day <= cycleLongest - luteallength + 3) {
                        // Fertile days
                        DayEntry.FERTILITY_FUTURE
                    } else {
                        // Infertile days
                        DayEntry.INFERTILE_FUTURE
                    }
                    val entryCalculated = DayEntry(type, datePredicted, dayofcycle, 1)
                    dayEntries.add(entryCalculated)
                    dayofcycle++
                }
                dayofcycle = 1
            }
        }

        // Fill details for each day
        var index = 0
        var entryTarget: DayEntry? = null
        result = db!!.rawQuery("select notes.eventdate, content, symptom from " +
                "notes " +
                "left outer join symptoms on notes.eventdate=symptoms.eventdate " +
                "order by notes.eventdate",
                null)
        while (result.moveToNext()) {
            val dbdate = result.getString(0)!!
            val eventyear = dbdate.substring(0, 4).toInt(10)
            val eventmonth = dbdate.substring(4, 6).toInt(10)
            val eventday = dbdate.substring(6, 8).toInt(10)
            val eventdate = GregorianCalendar(eventyear, eventmonth - 1, eventday)
            var notes = result.getString(1)
            if (notes == null) notes = ""
            val symptom = result.getInt(2)
            if (dayEntries.size == 0) {
                // If we don't have any entries yet, create an empty entry for the details
                entryTarget = DayEntry(DayEntry.EMPTY, eventdate, 0, 0)
                dayEntries.add(entryTarget)
                index = 0
            } else {
                // We have at least ony entry, but it may be in the future
                if (dayEntries.size > index) {
                    entry = dayEntries[index]
                    if (entry.date.timeInMillis > eventdate.timeInMillis) {
                        // The existing entry is in the future, so create a new blank entry before
                        // this day to hold the details
                        entryTarget = DayEntry(DayEntry.EMPTY, eventdate, 0, 0)
                        dayEntries.add(index, entryTarget)
                    } else {
                        // Skip existing entries, until a matching one is found
                        do {
                            entry = dayEntries[index]
                            index++
                        } while (entry!!.date.timeInMillis < eventdate.timeInMillis && index < dayEntries.size)
                        index--
                        if (entry.date == eventdate) {
                            entryTarget = entry
                        } else {
                            // No matching entry found, so add a new one for this day
                            entryTarget = DayEntry(DayEntry.EMPTY, eventdate, 0, 0)
                            dayEntries.add(entryTarget)
                        }
                    }
                }

                // Add symptom to the current entry
                if (null != entryTarget) {
                    if (symptom != 0) {
                        entryTarget.symptoms.add(symptom)
                    }
                    entryTarget.notes = notes
                }
            }
        }
        result.close()
        System.gc()
    }

    /**
     * Load data for statistics and overview without calculating anything.
     */
    fun loadRawData() {
        var entry: DayEntry

        // Clean up existing data
        dayEntries.clear()

        // Get all entries from the database
        val statement = "select eventtype, eventdate from data where eventtype=" + String.format("%d", DayEntry.PERIOD_START) +
                " order by eventdate desc"
        val result = db!!.rawQuery(statement, null)
        while (result.moveToNext()) {
            val dbdate = result.getString(1)!!
            val eventyear = dbdate.substring(0, 4).toInt(10)
            val eventmonth = dbdate.substring(4, 6).toInt(10)
            val eventday = dbdate.substring(6, 8).toInt(10)
            val eventdate = GregorianCalendar(eventyear,
                    eventmonth - 1, eventday)

            // Create new day entry
            entry = DayEntry(DayEntry.PERIOD_START, eventdate, 1, 0)
            dayEntries.add(entry)
        }
        result.close()
        System.gc()
    }

    /**
     * Load data and details without calculating anything.
     */
    @SuppressLint("DefaultLocale")
    fun loadRawDataWithDetails() {
        // Clean up existing data
        dayEntries.clear()

        // Get all entries with details from the database
        val statement = "select data.eventdate, eventtype, intensity, content, symptom from " +
                "data " +
                "left outer join notes on data.eventdate=notes.eventdate " +
                "left outer join symptoms on data.eventdate=symptoms.eventdate " +
                "order by data.eventdate"
        val result = db!!.rawQuery(statement, null)
        var entry: DayEntry? = null
        var dbdate: String? = ""
        var symptoms: MutableList<Int> = ArrayList()
        var dayofcycle = 1
        while (result.moveToNext()) {
            // New day?
            if (dbdate != result.getString(0)) {
                // Store pending entry if it is not a total empty day
                if (entry != null) {
                    entry.dayofcycle = dayofcycle
                    entry.symptoms = symptoms
                    if (entry.type != DayEntry.EMPTY || !entry.notes.isEmpty() || entry.symptoms.size > 0) {
                        dayEntries.add(entry)
                    }
                }
                dbdate = result.getString(0)
                assert(dbdate != null)
                val eventtype = result.getInt(1)
                val eventyear = dbdate.substring(0, 4).toInt(10)
                val eventmonth = dbdate.substring(4, 6).toInt(10)
                val eventday = dbdate.substring(6, 8).toInt(10)
                val eventdate = GregorianCalendar(eventyear, eventmonth - 1, eventday)
                val intensity = result.getInt(2)
                var notes = result.getString(3)
                if (notes == null) notes = ""
                entry = DayEntry()
                entry.type = eventtype
                entry.date.time = eventdate.time
                entry.intensity = if (intensity > 0) intensity else 1
                entry.notes = notes
                symptoms = ArrayList()
                if (result.getInt(4) != 0) {
                    symptoms.add(result.getInt(4))
                }
                if (eventtype == DayEntry.PERIOD_START) dayofcycle = 1 else dayofcycle++
            } else {
                symptoms.add(result.getInt(4))
            }
        }
        result.close()
        if (entry != null) {
            entry.symptoms = symptoms
            entry.dayofcycle = dayofcycle
            if (entry.type != DayEntry.EMPTY || !entry.notes.isEmpty() || entry.symptoms.size > 0) {
                dayEntries.add(entry)
            }
        }
        System.gc()
    }

    /**
     * Get entry type for a specific day
     *
     * @param date Date of the entry
     */
    fun getEntryType(date: GregorianCalendar): Int {
        for (entry in dayEntries) {
            // If entry was found, then return type
            if (entry.date == date) {
                return entry.type
            }
        }

        // Fall back if month was not found, then return "empty" as type
        return 0
    }

    /**
     * Get entry for a specific day
     *
     * @param year  Year including century
     * @param month Month (1-12)
     * @param day   Day of the month (1-31)
     */
    private fun getEntry(year: Int, month: Int, day: Int): DayEntry? {
        for (entry in dayEntries) {
            // If entry was found, then return entry
            if (entry.date[GregorianCalendar.YEAR] == year && entry.date[GregorianCalendar.MONTH] == month - 1 && entry.date[GregorianCalendar.DATE] == day) {
                return entry
            }
        }

        // No entry was found
        return null
    }

    /**
     * Get entry for a specific day
     *
     * @param date Date of the entry
     */
    fun getEntry(date: GregorianCalendar): DayEntry? {
        for (entry in dayEntries) {
            // If entry was found, then return entry
            if (entry.date == date) {
                return entry
            }
        }

        // No entry was found
        return null
    }

    /**
     * Get a specific day including all details
     *
     * @param year  Year including century
     * @param month Month (1-12)
     * @param day   Day of the month (1-31)
     */
    fun getEntryWithDetails(year: Int, month: Int, day: Int): DayEntry {
        var entry = getEntry(year, month, day)
        if (entry == null) {
            entry = DayEntry()

            // Set chosen date
            val date = GregorianCalendar(year, month - 1, day)
            entry.date.time = date.time
        }
        val statementNotes = String.format(
                "select content from notes where eventdate = '%04d%02d%02d'",
                year, month, day)
        val resultNotes = db!!.rawQuery(statementNotes, null)
        if (resultNotes.moveToNext()) {
            entry.notes = resultNotes.getString(0)
        }
        resultNotes.close()
        val statementSymptoms = String.format(
                "select symptom from symptoms where eventdate = '%04d%02d%02d'",
                year, month, day)
        val resultSymptoms = db!!.rawQuery(statementSymptoms, null)
        val symptoms: MutableList<Int> = ArrayList()
        while (resultSymptoms.moveToNext()) {
            symptoms.add(resultSymptoms.getInt(0))
        }
        entry.symptoms = symptoms
        resultSymptoms.close()
        return entry
    }

    /**
     * Store details for a specific day
     *
     * @param entry The details to be stored
     */
    @SuppressLint("DefaultLocale")
    fun addEntryDetails(entry: DayEntry) {
        var statement: String
        val dateString = String.format(Locale.getDefault(), "%04d%02d%02d",
                entry.date[GregorianCalendar.YEAR],
                entry.date[GregorianCalendar.MONTH] + 1,
                entry.date[GregorianCalendar.DAY_OF_MONTH])
        db!!.beginTransaction()

        // Delete existing details, if any
        statement = String.format(
                "delete from notes where eventdate = '%s'",
                dateString)
        db!!.execSQL(statement)
        statement = String.format(
                "delete from symptoms where eventdate = '%s'",
                dateString)
        db!!.execSQL(statement)

        // If there is no calendar entry for this day yet, then add one first
        var addNew = false
        statement = String.format(
                "select eventtype from data where eventdate='%s'",
                dateString)
        val result = db!!.rawQuery(statement, null)
        if (!result.moveToNext()) addNew = true
        result.close()
        if (addNew) {
            statement = String.format(
                    "insert into data (eventdate, eventtype) values ('%s', %d)",
                    dateString,
                    DayEntry.EMPTY)
            db!!.execSQL(statement)
        }

        // Store new details
        statement = String.format(
                "update data set intensity = %d where eventdate='%s'",
                entry.intensity,
                dateString)
        db!!.execSQL(statement)
        statement = String.format(
                "insert into notes (eventdate, content) values ('%s', ?)",
                dateString)
        db!!.execSQL(statement, arrayOf(entry.notes))
        var count = 0
        while (count < entry.symptoms.size) {
            statement = String.format(
                    "insert into symptoms (eventdate, symptom) values ('%s', %d)",
                    dateString,
                    entry.symptoms[count])
            db!!.execSQL(statement)
            count++
        }
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    /**
     * Get a named option from the options table
     *
     * @param name         Name of the option to retrieve
     * @param defaultvalue Default value to be used if the option is not stored yet
     */
    private fun getOption(name: String, defaultvalue: String?): String? {
        var value = defaultvalue
        val statement = "select value from options where name = ?"
        val result = db!!.rawQuery(statement, arrayOf(name))
        if (result.moveToNext()) {
            value = result.getString(0)
        }
        result.close()
        return value
    }

    private fun getOption(name: String, defaultvalue: Int): Int {
        var value = defaultvalue
        val statement = "select value from options where name = ?"
        val result = db!!.rawQuery(statement, arrayOf(name))
        if (result.moveToNext()) {
            value = result.getInt(0)
        }
        result.close()
        return value
    }

    private fun getOption(name: String, defaultvalue: Boolean): Boolean {
        var value = defaultvalue
        val statement = "select value from options where name = ?"
        val result = db!!.rawQuery(statement, arrayOf(name))
        if (result.moveToNext()) {
            value = result.getString(0) == "1"
        }
        result.close()
        return value
    }

    /**
     * Set a named option to be stored in the options table
     *
     * @param name  Name of the option to store
     * @param value Value of the option to store
     */
    fun setOption(name: String, value: String) {
        var statement: String
        db!!.beginTransaction()

        // Delete existing value
        statement = "delete from options where name = ?"
        db!!.execSQL(statement, arrayOf(name))

        // Save option
        statement = "insert into options (name, value) values (?, ?)"
        db!!.execSQL(statement, arrayOf(name, value))
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    fun setOption(name: String, value: Int) {
        var statement: String
        val valueStr: String
        db!!.beginTransaction()

        // Delete existing value
        statement = "delete from options where name = ?"
        db!!.execSQL(statement, arrayOf(name))

        // Save option
        valueStr = value.toString()
        statement = "insert into options (name, value) values (?, ?)"
        db!!.execSQL(statement, arrayOf(name, valueStr))
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    fun setOption(name: String, value: Boolean) {
        var statement: String
        db!!.beginTransaction()

        // Delete existing value
        statement = "delete from options where name = ?"
        db!!.execSQL(statement, arrayOf(name))

        // Save option
        statement = "insert into options (name, value) values (?, ?)"
        db!!.execSQL(statement, arrayOf(name, if (value) "1" else "0"))
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    /**
     * Backup database to a given URI
     */
    fun backupToUri(context: Context, uri: Uri?): Boolean {
        var result = false

        // Check if uri is accessible
        val directory = DocumentFile.fromTreeUri(context, uri!!)
        if (!directory!!.isDirectory) {
            return false
        }

        // First we need to create a directory where the backup will be stored
        val destinationDirectoryName = context.packageName
        var destinationDirectory = directory.findFile(destinationDirectoryName)
        if (null == destinationDirectory || !destinationDirectory.isDirectory) {
            destinationDirectory = directory.createDirectory(destinationDirectoryName)
        }

        // If the directory could not be created, stop now
        if (null == destinationDirectory) {
            return false
        }

        // Backup database file
        var sourceFile = File(db!!.path)
        var destinationFileName = sourceFile.name
        var destinationFile = destinationDirectory.findFile(destinationFileName)
        destinationFile?.delete()
        destinationFile = destinationDirectory.createFile("application/octet-stream", destinationFileName)

        // Close the database
        db!!.close()
        try {
            val destinationStream = context.contentResolver.openOutputStream(destinationFile!!.uri)
            val sourceStream = FileInputStream(sourceFile)
            var byteRead = 0
            val buffer = ByteArray(8192)
            while (sourceStream.read(buffer, 0, 8192).also { byteRead = it } != -1) {
                destinationStream!!.write(buffer, 0, byteRead)
            }
            sourceStream.close()
            destinationStream!!.close()
            result = true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Backup journal if required and the previous backup was successful
        sourceFile = File(db!!.path + "-journal")
        if (result && sourceFile.exists()) {
            result = false
            destinationFileName = sourceFile.name
            destinationFile = destinationDirectory.findFile(destinationFileName)
            destinationFile?.delete()
            destinationFile = destinationDirectory.createFile("application/octet-stream", destinationFileName)
            try {
                val outputStream = context.contentResolver.openOutputStream(destinationFile!!.uri)
                val sourceStream = FileInputStream(sourceFile)
                var byteRead = 0
                val buffer = ByteArray(8192)
                while (sourceStream.read(buffer, 0, 8192).also { byteRead = it } != -1) {
                    outputStream!!.write(buffer, 0, byteRead)
                }
                sourceStream.close()
                outputStream!!.close()
                result = true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Open the database again
        open()
        return result
    }

    /**
     * Restore database from a given URI
     */
    fun restoreFromUri(context: Context, uri: Uri?): Boolean {
        var result = false

        // Check if uri exists
        val directory = DocumentFile.fromTreeUri(context, uri!!)
        if (!directory!!.isDirectory) {
            return false
        }

        // Check if subfolder with backup exists
        val sourceDirectoryName = context.packageName
        val sourceDirectory = directory.findFile(sourceDirectoryName)
        if (null == sourceDirectory || !sourceDirectory.isDirectory) {
            return false
        }

        // Restore database file
        var destinationFile = File(db!!.path)
        var destinationFileName = destinationFile.name
        var sourceFile: DocumentFile? = sourceDirectory.findFile(destinationFileName)
                ?: return false

        // Close the database
        db!!.close()
        try {
            val sourceStream = sourceFile?.uri?.let { context.contentResolver.openInputStream(it) }
            val destinationStream = FileOutputStream(destinationFile)
            var byteRead = 0
            val buffer = ByteArray(8192)
            while (sourceStream!!.read(buffer, 0, 8192).also { byteRead = it } != -1) {
                destinationStream.write(buffer, 0, byteRead)
            }
            sourceStream.close()
            destinationStream.close()
            result = true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Restore journal if required
        destinationFile = File(db!!.path + "-journal")
        destinationFileName = destinationFile.name
        sourceFile = sourceDirectory.findFile(destinationFileName)
        if (null != sourceFile) {
            result = false
            try {
                val sourceStream = context.contentResolver.openInputStream(sourceFile.uri)
                val destinationStream = FileOutputStream(destinationFile)
                var byteRead = 0
                val buffer = ByteArray(8192)
                while (sourceStream!!.read(buffer, 0, 8192).also { byteRead = it } != -1) {
                    destinationStream.write(buffer, 0, byteRead)
                }
                sourceStream.close()
                destinationStream.close()
                result = true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Open the database again
        open()
        return result
    }

    /**
     * Restore application preferences from the database
     *
     * <br></br><br></br>*(Just a hack for now - in the future we might want to get rid of shared preferences)*
     */
    fun restorePreferences() {
        val period_length = getOption("period_length", DEFAULT_PERIOD_LENGTH)
        val luteal_length = getOption("luteal_length", DEFAULT_LUTEAL_LENGTH)
        var startofweek = getOption("startofweek", DEFAULT_START_OF_WEEK)
        if (startofweek !== DEFAULT_START_OF_WEEK && startofweek != 1) startofweek = DEFAULT_START_OF_WEEK
        val maximum_cycle_length = getOption("maximum_cycle_length", DEFAULT_CYCLE_LENGTH)
        val direct_details = getOption("direct_details", DEFAULT_DIRECT_DETAILS)
        val show_cycle = getOption("show_cycle", DEFAULT_SHOW_CYCLE)
        val backup_uri = getOption("backup_uri", null)
        val preferences = PreferenceUtils(context)
        val editor = preferences.edit()

        // Make sure, there are no existing values which may cause problems
        editor.remove("period_length")
        editor.remove("luteal_length")
        editor.remove("startofweek")
        editor.remove("maximum_cycle_length")
        editor.remove("direct_details")
        editor.remove("show_cycle")
        editor.remove("backup_uri")

        // Store values
        editor.putString("period_length", period_length.toString())
        editor.putString("luteal_length", luteal_length.toString())
        editor.putString("startofweek", startofweek.toString())
        editor.putString("maximum_cycle_length", maximum_cycle_length.toString())
        editor.putBoolean("direct_details", direct_details)
        editor.putBoolean("show_cycle", show_cycle)
        editor.putString("backup_uri", backup_uri)
        editor.apply()
    }

    /**
     * Constructor, will try to create/open a writable database
     *
     * @param context Application context
     */
    init {
        open()
        dayEntries = ArrayList()
    }
}