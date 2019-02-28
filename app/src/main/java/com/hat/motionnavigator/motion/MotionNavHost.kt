package com.hat.motionnavigator.motion

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.Navigation
import com.hat.motionnavigator.R

class MotionNavHost(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs), NavHost {
    private val navController = NavController(context)
    private val graphResId: Int
    private var hasSetGraph = false

    init {
        Navigation.setViewNavController(this, navController)

        with (context.theme.obtainStyledAttributes(attrs, R.styleable.MotionNavHost, 0, 0)) {
            graphResId = getResourceId(R.styleable.MotionNavHost_navGraph, 0)
            if (graphResId == 0) throw IllegalStateException("No graph id set for ConstraintLayoutNavHost")
            recycle()
        }
    }

    infix fun createsViewsBy(viewFactory: (Int) -> MotionLayout) {
        val navigator = MotionNavigator(this, viewFactory)
        navController.navigatorProvider.addNavigator(navigator)

        if (!hasSetGraph) {
            hasSetGraph = true
            navController.setGraph(graphResId)
        }
    }

    override fun getNavController(): NavController = navController
}