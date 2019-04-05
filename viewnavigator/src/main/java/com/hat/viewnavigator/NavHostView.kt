package com.hat.viewnavigator

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.Navigation

class NavHostView(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs), NavHost {
    companion object {
        const val KEY_VIEW_STATE = "viewState"
        const val KEY_NAV_CONTROLLER_STATE = "navControllerState"
    }

    private val navController = NavController(context)
    private val graphResId: Int
    private var hasSetGraph = false

    init {
        Navigation.setViewNavController(this, navController)

        with (context.theme.obtainStyledAttributes(attrs, R.styleable.NavHostView, 0, 0)) {
            graphResId = getResourceId(R.styleable.NavHostView_navGraph, 0)
            if (graphResId == 0) throw IllegalStateException("No graph id set for NavHostView")
            recycle()
        }
    }

    fun install(activity: AppCompatActivity, destinations: (Int) -> ViewNavigator.Destination.Injector<out View>, fallbackTransition: NavTransition<View, View>? = null, fallbackPopTransition: NavTransition<View, View>? = null){
        install(ActivityBackPressBinding(activity, navController), destinations, fallbackTransition, fallbackPopTransition)
    }

    fun install(backPressBinding: ViewNavigator.BackPressBinding, destinations: (Int) -> ViewNavigator.Destination.Injector<out View>, fallbackTransition: NavTransition<View, View>? = null, fallbackPopTransition: NavTransition<View, View>? = null){
        val navigator = ViewNavigator(this, destinations, fallbackTransition, fallbackPopTransition, backPressBinding)

        navController.navigatorProvider.addNavigator(navigator)

        if (!hasSetGraph) {
            hasSetGraph = true
            navController.setGraph(graphResId)
        }
    }


    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(KEY_VIEW_STATE, super.onSaveInstanceState())
            putParcelable(KEY_NAV_CONTROLLER_STATE, navController.saveState())
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        with (state as Bundle) {
            super.onRestoreInstanceState(getParcelable(KEY_VIEW_STATE))
            navController.restoreState(getParcelable(KEY_NAV_CONTROLLER_STATE))
        }
    }

    override fun getNavController(): NavController = navController
}