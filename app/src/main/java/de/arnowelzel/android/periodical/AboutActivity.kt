/*
 * Periodical "about" activity
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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity to handle the "About" command
 */
class AboutActivity : AppCompatActivity() {
    /**
     * Called when the activity starts
     */
    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up view
        setContentView(R.layout.webview)

        // Activate "back button" in Action Bar
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        val view = findViewById<WebView>(R.id.webView)
        view.settings.javaScriptEnabled = true
        view.webViewClient = object : WebViewClient() {
            // Update version and year after loading the document
            override fun onPageFinished(view: WebView, url: String) {
                val preferences = PreferenceUtils(applicationContext)
                var backupUriString = preferences.getString("backup_uri", "")
                if (backupUriString == "") {
                    backupUriString = "<em>(" + getString(R.string.backup_noruiyet) + ")</em>"
                }
                super.onPageFinished(view, url)
                view.loadUrl("javascript:replace('version', '" + BuildConfig.VERSION_NAME + "')")
                view.loadUrl("javascript:replace('year', '" + BuildConfig.VERSION_YEAR + "')")
                view.loadUrl("javascript:replace('backupfolder','$backupUriString')")
            }

            // Handle URLs always external links
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }
        }
        view.loadUrl("file:///android_asset/" + getString(R.string.asset_about))
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
}