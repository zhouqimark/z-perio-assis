package com.z5i.periodical.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.mxn.soul.flowingdrawer_core.FlowingDrawer
import com.z5i.periodical.MainActivityApp
import com.z5i.periodical.R

class MenuListFragment: Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        val navigationView = view.findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener {
            menuItem ->
            val mActivity = activity!! as MainActivityApp
            val drawer = mActivity.findViewById<FlowingDrawer>(R.id.drawer_layout)
            drawer.toggleMenu()
            when(menuItem.itemId) {
                R.id.list -> {
                    mActivity.showList()
                }
                R.id.listdetails -> {
                    mActivity.showListDetails()
                }
                R.id.help -> {
                    mActivity.showHelp()
                }
                R.id.about -> {
                    mActivity.showAbout()
                }
                R.id.copy -> {
                    mActivity.doBackup()
                }
                R.id.restore -> {
                    mActivity.doRestore()
                }
                R.id.options -> {
                    mActivity.showOptions()
                }
                R.id.exit -> {
                    mActivity.finish()
                }
            }
            true
        }

        return view
    }
}