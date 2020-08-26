/*
 * Utility class to access shared preferences
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

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.preference.PreferenceManager

/**
 * Preference utilities
 */
internal class PreferenceUtils {
    /**
     * Private reference to shared preferences
     */
    private val preferences: SharedPreferences

    /**
     * Constructor, will try to create/open a writable database
     *
     * @param context Application context
     */
    constructor(context: Context?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Constructor, will use an existing shared preference object
     *
     * @param sharedPreferences Shared preferences to be used
     */
    constructor(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    /**
     * Get integer preference
     *
     * @param key      Name of the preference
     * @param defValue Default value
     * @return The preference
     */
    fun getInt(key: String?, defValue: Int): Int {
        val result: Int
        result = try {
            preferences.getString(key, defValue.toString())!!.toInt()
        } catch (e: NumberFormatException) {
            defValue
        } catch (e: ClassCastException) {
            defValue
        }
        return result
    }

    /**
     * Get string preference
     *
     * @param key      Name of the preference
     * @param defValue Default value
     * @return The preference
     */
    fun getString(key: String?, defValue: String?): String? {
        val result: String?
        result = try {
            preferences.getString(key, defValue)
        } catch (e: ClassCastException) {
            defValue
        }
        return result
    }

    /**
     * Get bool preference
     *
     * @param key      Name of the preference
     * @param defValue Default value
     * @return The preference
     */
    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val result: Boolean
        result = try {
            preferences.getBoolean(key, defValue)
        } catch (e: ClassCastException) {
            defValue
        }
        return result
    }

    /**
     * Get an editor for the shared preferences
     *
     * @return Editor for the shared preferences
     */
    fun edit(): Editor {
        return preferences.edit()
    }
}