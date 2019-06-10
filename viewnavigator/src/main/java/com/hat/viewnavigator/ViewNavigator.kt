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
            val context = DestinationsContextImpl()
            context.init()

            return { destinationId ->
                context.map[destinationId] ?: throw IllegalStateException("No destination injector for id: $destinationId")
            }
        }
    }

    interface DestinationsContext{
        fun <V: View> destination(id: Int, init: DestinationContext<V>.() -> Unit)
    }

    interface DestinationContext<V: View>{
        fun view(view: () -> V)
        fun defaultTransition(useAsPop: Boolean = true, transition: () -> NavTransition<View, V>)
        fun defaultPopTransition(popTransition: () -> NavTransition<View, V>)
    }

    private class DestinationsContextImpl: DestinationsContext{
        val map: MutableMap<Int, ViewNavigator.Destination.Injector<out View>> = mutableMapOf()

        override fun <V: View> destination(id: Int, init: DestinationContext<V>.() -> Unit){
            val context = DestinationContextImpl<V>()
            context.init()
            map[id] = ViewNavigator.Destination.Injector(context.viewFactory, context.transitionsFactory)
        }
    }

    private class DestinationContextImpl<V: View>: DestinationContext<V>{
        lateinit var viewFactory: () -> V
        private var transitionFactory: (() -> NavTransition<View, V>)? = null
        private var popTransitionFactory: (() -> NavTransition<View, V>)? = null

        val transitionsFactory: (() -> ViewNavigator.Destination.Transitions<V>)
            get() = {
                ViewNavigator.Destination.Transitions(transitionFactory?.invoke() ?: NavTransition.NoTransition(), popTransitionFactory?.invoke() ?: NavTransition.NoTransition())
            }

        override fun view(view: () -> V){
            viewFactory = view
        }

        override fun defaultTransition(useAsPop: Boolean, transition: () -> NavTransition<View, V>) {
            transitionFactory = transition
            if (useAsPop) popTransitionFactory = transition
        }

        override fun defaultPopTransition(popTransition: () -> NavTransition<View, V>) {
            popTransitionFactory = popTransition
        }
    }

    private val stack: Deque<Destination> = ArrayDeque()
    private val popTransitions: MutableMap<Pair<Destination, Destination>, NavTransition<View, View>> =
        mutableMapOf()

    private val noTransition = NavTransition.NoTransition<View, View>()

    @Suppress("unchecked_cast")
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
        //lateinit var view: View
        private var _view: View? = null

        val view: View
            get() {
                val currentView = _view

                return if (currentView == null) {
                    val newView = viewFactory()
                    _view = newView
                    newView
                } else currentView
            }

        private lateinit var viewFactory: () -> View
        var defaultTransitions: Transitions<View>? = null
        var isInTransition: Boolean = false

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            onInflateListener(this)
        }

        fun releaseView(){
            _view = null
        }

        class Transitions<V : View>(
            val enterTransition: NavTransition<View, V>,
            val popEnterTransition: NavTransition<View, V> = enterTransition
        )

        class Injector<V : View>(
            private val viewFactory: () -> V,
            private val defaultTransitionFactory: (() -> Transitions<V>)? = null
        ) {
            @Suppress("unchecked_cast")
            fun inject(destination: Destination) {
                destination.viewFactory = viewFactory
                destination.defaultTransitions = defaultTransitionFactory?.invoke() as Transitions<View>?
            }
        }
    }

    private class DispatchDestinationState(private val from: Destination, private val to: Destination) :
        NavTransition.Context {
        override fun dispatchTransitionStarted() {
            from.isInTransition = true
            to.isInTransition = true
        }

        override fun dispatchTransitionEnded() {
            from.isInTransition = false
            to.isInTransition = false
            from.releaseView()
        }
    }

    interface BackPressBinding{
        fun bind()
        fun unBind()
    }
}