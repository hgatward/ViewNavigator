package com.hat.viewnavigator

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import java.util.*

typealias Destinations = (Int) -> ViewNavigator.Destination.Injector<out View>

@Navigator.Name("view")
class ViewNavigator(
    private val container: ViewGroup,
    private val destinations: Destinations,
    private val fallbackTransition: NavTransition<View, View>?,
    private val fallbackPopTransition: NavTransition<View, View>?,
    private val backPressBinding: BackPressBinding
) : Navigator<ViewNavigator.Destination>() {
    companion object {
        fun destinations(init: DestinationsContext.() -> Unit): Destinations{
            val context = DestinationsContext()
            context.init()

            return { destinationId ->
                context.map[destinationId] ?: throw IllegalStateException("No destination injector for id: $destinationId")
            }
        }

        class DestinationsContext{
            val map: MutableMap<Int, ViewNavigator.Destination.Injector<out View>> = mutableMapOf()

            fun <V: View> destination(id: Int, init: DestinationContext<V>.() -> Unit){
                val context = DestinationContext<V>()
                context.init()
                map[id] = ViewNavigator.Destination.Injector(context.viewFactory, context.transitionsFactory)
            }
        }

        class DestinationContext<V: View>{
            lateinit var viewFactory: () -> V
            var transitionsFactory: (() -> ViewNavigator.Destination.Transitions<V>)? = null

            fun view(view: V){
                viewFactory = { view }
            }

            fun defaultTransitions(transition: NavTransition<View, V>, popTransition: NavTransition<View, V> = transition){
                transitionsFactory = { ViewNavigator.Destination.Transitions(transition, popTransition) }
            }
        }
    }

    private val stack: Deque<Destination> = ArrayDeque()
    private val popTransitions: MutableMap<Pair<Destination, Destination>, NavTransition<View, View>> =
        mutableMapOf()

    private val noTransition = NoTransition()

    override fun navigate(
        to: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        if (to.isInTransition) return null

        val from: Destination? = stack.peek()
        stack.push(to)

        val extras = navigatorExtras as? NavTransitions<View, View>

        if (from == null) {
            container.addView(to.view)
        } else {
            val transition = extras?.transition ?: to.defaultTransitions?.enterTransition ?: fallbackTransition ?: noTransition
            transition(DispatchDestinationState(from, to), container, from.view, to.view)

            if (extras != null) popTransitions[to to from] = extras.popTransition
        }

        return to
    }

    override fun createDestination(): Destination = Destination(this) { destination ->
        destinations(destination.id).inject(destination)
    }

    override fun popBackStack(): Boolean {
        val to: Destination? = stack.elementAtOrNull(1)
        if (to != null && to.isInTransition) return false

        val from: Destination = stack.pollFirst() ?: return false

        if (to != null) {
            val transition = popTransitions[from to to] ?: to.defaultTransitions?.popEnterTransition ?: fallbackPopTransition ?: noTransition
            transition(DispatchDestinationState(from, to), container, from.view, to.view)
        }

        return true
    }

    override fun onBackPressAdded() {
        backPressBinding.bind()
    }

    override fun onBackPressRemoved() {
        backPressBinding.unBind()
    }

    class Destination(
        navigator: Navigator<Destination>,
        private val onInflateListener: (Destination) -> Unit
    ) : NavDestination(navigator) {
        lateinit var view: View
        var defaultTransitions: Transitions<View>? = null
        var isInTransition: Boolean = false

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            onInflateListener(this)
        }

        class Transitions<V : View>(
            val enterTransition: NavTransition<View, V>,
            val popEnterTransition: NavTransition<View, V> = enterTransition
        )

        class Injector<V : View>(
            private val viewFactory: () -> V,
            private val defaultTransitionFactory: (() -> Transitions<V>)? = null
        ) {
            fun inject(destination: Destination) {
                with(destination) {
                    view = viewFactory()
                    defaultTransitions = defaultTransitionFactory?.invoke() as Transitions<View>?
                }
            }
        }
    }

    private class DispatchDestinationState(private val first: Destination, private val second: Destination) :
        NavTransition.Context {
        override fun dispatchTransitionStarted() {
            first.isInTransition = true
            second.isInTransition = true
        }

        override fun dispatchTransitionEnded() {
            first.isInTransition = false
            second.isInTransition = false
        }
    }

    private class NoTransition : NavTransition<View, View> {
        override fun NavTransition.Context.transition(container: ViewGroup, from: View, to: View) {
            dispatchTransitionStarted()
            container.addView(to)
            container.removeView(from)
            dispatchTransitionEnded()
        }
    }

    interface BackPressBinding{
        fun bind()
        fun unBind()
    }
}