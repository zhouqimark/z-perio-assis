/*
 * Periodical options activity
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

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceGroup
import android.view.MenuItem
import android.widget.Toast

/**
 * Activity to handle the "Preferences" command
 */
class PreferenceActivity : AppCompatPreferenceActivity(), OnSharedPreferenceChangeListener {
    private var dbMain: PeriodicalDatabase? = null

    /**
     * Called when activity starts
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext!!

        // We get/store preferences in the database
        dbMain = PeriodicalDatabase(context)
        addPreferencesFromResource(R.xml.preferences)
        initSummary(preferenceScreen)

        // Add validation for period length
        findPreference("period_length").onPreferenceChangeListener = OnPreferenceChangeListener { preference, newValue ->
            val value: Int
            value = try {
                newValue.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            if (value < 1 || value > 14) {
                Toast.makeText(context,
                        resources.getString(R.string.invalid_period_length),
                        Toast.LENGTH_SHORT).show()
                return@OnPreferenceChangeListener false
            }
            true
        }

        // Add validation for luteal length
        findPreference("luteal_length").onPreferenceChangeListener = OnPreferenceChangeListener { preference, newValue ->
            val value: Int
            value = try {
                newValue.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            if (value < 1) {
                Toast.makeText(context,
                        resources.getString(R.string.invalid_luteal_length),
                        Toast.LENGTH_SHORT).show()
                return@OnPreferenceChangeListener false
            }
            true
        }

        // Add validation for cycle length filter
        findPreference("maximum_cycle_length").onPreferenceChangeListener = OnPreferenceChangeListener { preference, newValue ->
            val value: Int
            value = try {
                newValue.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            if (value < 60) {
                Toast.makeText(context,
                        resources.getString(R.string.invalid_maximum_cycle_length),
                        Toast.LENGTH_SHORT).show()
                return@OnPreferenceChangeListener false
            }
            true
        }

        // Activate "back button" in Action Bar
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Called when the activity starts interacting with the user
     */
    override fun onResume() {
        super.onResume()

        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    /**
     * Called when activity pauses
     */
    override fun onPause() {
        super.onPause()

        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Handle preference changes
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val preferenceUtils = PreferenceUtils(sharedPreferences)
        val pref = findPreference(key)
        updatePrefSummary(pref)
        when (key) {
            "period_length" -> dbMain!!.setOption(key, preferenceUtils.getInt(key, dbMain!!.DEFAULT_PERIOD_LENGTH))
            "luteal_length" -> dbMain!!.setOption(key, preferenceUtils.getInt(key, dbMain!!.DEFAULT_LUTEAL_LENGTH))
            "startofweek" -> dbMain!!.setOption(key, preferenceUtils.getInt(key, dbMain!!.DEFAULT_START_OF_WEEK))
            "maximum_cycle_length" -> dbMain!!.setOption(key, preferenceUtils.getInt(key, dbMain!!.DEFAULT_CYCLE_LENGTH))
            "direct_details" -> dbMain!!.setOption(key, preferenceUtils.getBoolean(key, dbMain!!.DEFAULT_DIRECT_DETAILS))
            "show_cycle" -> dbMain!!.setOption(key, preferenceUtils.getBoolean(key, dbMain!!.DEFAULT_SHOW_CYCLE))
        }
    }

    /**
     * Set initial summary texts
     */
    private fun initSummary(p: Preference) {
        if (p is PreferenceGroup) {
            val pGrp = p
            for (i in 0 until pGrp.preferenceCount) {
                initSummary(pGrp.getPreference(i))
            }
        } else {
            updatePrefSummary(p)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * Update summary text for a preference
         */
        private fun updatePrefSummary(p: Preference) {
            if (p is ListPreference) {
                p.setSummary(p.entry)
            }
            if (p is EditTextPreference) {
                if (p.getTitle().toString().contains("assword")) {
                    p.setSummary("******")
                } else {
                    p.setSummary(p.text)
                }
            }
        }
    }
}