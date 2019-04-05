package com.hat.viewnavigator

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.Navigation

class CompositeNavHost : Fragment(), NavHost {
    companion object {
        const val KEY_NAV_CONTROLLER_STATE = "navControllerState"
    }

    private lateinit var navController: NavController

    private var hasSetGraph = false
    private var graphResId: Int? = null

    fun install(
        activity: AppCompatActivity,
        destinations: (Int) -> ViewNavigator.Destination.Injector<out View>,
        fallbackTransition: NavTransition<View, View>? = null,
        fallbackPopTransition: NavTransition<View, View>? = null
    ) {
        install(ActivityBackPressBinding(activity, navController), destinations, fallbackTransition, fallbackPopTransition)
    }

    fun install(
        backPressBinding: ViewNavigator.BackPressBinding,
        destinations: (Int) -> ViewNavigator.Destination.Injector<out View>,
        fallbackTransition: NavTransition<View, View>? = null,
        fallbackPopTransition: NavTransition<View, View>? = null
    ) {
        val graphResId = graphResId ?: throw IllegalStateException("setGraph can't be called before onInflate of NavHostFragment")
        val view = view as FrameLayout? ?: throw IllegalStateException("setGraph can't be called before onCreateView of NavHostFragment")

        val viewNavigator = ViewNavigator(view, destinations, fallbackTransition, fallbackPopTransition, backPressBinding)
        val fragmentNavigator = FullyPoppableFragmentNavigator(requireContext(), childFragmentManager, id)

        navController.navigatorProvider.addNavigator(viewNavigator)
        navController.navigatorProvider.addNavigator(fragmentNavigator)

        if (!hasSetGraph) {
            hasSetGraph = true
            navController.setGraph(graphResId)
        }
    }

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)

        with(context.obtainStyledAttributes(attrs, R.styleable.CompositeNavHost)) {
            graphResId = getResourceId(R.styleable.CompositeNavHost_navGraph, 0)
            if (graphResId == 0) throw IllegalStateException("No graph id set for CompositeNavHost")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController = NavController(requireContext())
        restoreState(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = FrameLayout(inflater.context).apply { id = this@CompositeNavHost.id }
        Navigation.setViewNavController(view, navController)
        return view
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val state = savedInstanceState.getBundle(KEY_NAV_CONTROLLER_STATE)
            if (state != null) navController.restoreState(state)
        }
    }

    override fun getNavController(): NavController = navController

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = navController.saveState()
        if (state != null) outState.putBundle(KEY_NAV_CONTROLLER_STATE, state)
    }
}