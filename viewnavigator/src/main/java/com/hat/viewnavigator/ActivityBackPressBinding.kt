package com.hat.viewnavigator

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController

class ActivityBackPressBinding(private val activity: AppCompatActivity, private val navController: NavController): ViewNavigator.BackPressBinding {
    private var backPressCallback: (() -> Boolean)? = null

    override fun bind() {
        val backPressCallback = {
            navController.popBackStack()
            navController.currentDestination != null
        }

        this.backPressCallback = backPressCallback
        activity.addOnBackPressedCallback(backPressCallback)
    }

    override fun unBind() {
        backPressCallback?.run { activity.removeOnBackPressedCallback(this) }
    }
}