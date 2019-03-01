package com.hat.motionnavigator.motion

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.hat.motionnavigator.R
import java.util.*

@Navigator.Name("motion_layout")
class MotionNavigator(private val container: ViewGroup, private val viewFactory: (Int) -> MotionLayout): Navigator<MotionNavigator.Destination>() {
    private val stack: Deque<Destination> = ArrayDeque()
    private val popEnterScenes: MutableMap<Destination, SceneInfo> = mutableMapOf()
    private val popExitScenes: MutableMap<Destination, SceneInfo> = mutableMapOf()

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        if (destination.isActive()) return null

        val previous = stack.peek()
        stack.push(destination)

        val extras = (navigatorExtras as? Extras)

        destination.enter(extras?.enter)
        previous?.exit(extras?.exit)

        extras?.run {
            if (popEnter != null) popEnterScenes[destination] = popEnter
            if (popExit != null) popExitScenes[destination] = popExit
        }

        return destination
    }

    override fun createDestination(): Destination =
        Destination(this, container, viewFactory)

    override fun popBackStack(): Boolean {
        val enterDestination: Destination? = stack.elementAtOrNull(1)
        if (enterDestination != null && enterDestination.isActive()) return false

        val poppedDestination: Destination? = stack.pollFirst()

        if (poppedDestination != null) {
            //val enterDestination: Destination? = stack.peekFirst()
            enterDestination?.popEnter(popEnterScenes[enterDestination])

            poppedDestination.popExit(popExitScenes[poppedDestination])
            return true
        }

        return false
    }

    class Destination(navigator: Navigator<*>, private val container: ViewGroup, private val viewFactory: (Int) -> MotionLayout): NavDestination(navigator){
        companion object {
            const val NOT_FOUND = 0
        }

        lateinit var view: MotionLayout

        private lateinit var defaultScenes: DefaultScenes

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            with (context.theme.obtainStyledAttributes(attrs, R.styleable.MotionNavigator, 0, 0)){
                val enterResId = getResourceId(
                    R.styleable.MotionNavigator_defaultEnterScene,
                    NOT_FOUND
                ).require("defaultEnterScene")

                val exitResId: Int? = getResourceId(
                    R.styleable.MotionNavigator_defaultExitScene,
                    NOT_FOUND
                ).nullIfNotFound()

                val popEnterResId: Int? = getResourceId(
                    R.styleable.MotionNavigator_defaultPopEnterScene,
                    NOT_FOUND
                ).nullIfNotFound()

                val popExitResId: Int? = getResourceId(
                    R.styleable.MotionNavigator_defaultPopExitScene,
                    NOT_FOUND
                ).nullIfNotFound()

                defaultScenes = DefaultScenes.inferFromResourceIds(enterResId, exitResId, popEnterResId, popExitResId)

                recycle()
            }

            view = viewFactory(id)
        }

        private fun Int.require(attrName: String): Int = if (this == NOT_FOUND) throw IllegalStateException("MotionNavigator.Destination requires a motion scene for attribute $attrName") else this
        private fun Int.nullIfNotFound(): Int? = if (this == NOT_FOUND) null else this

        fun enter(sceneInfo: SceneInfo? = null){
            container.addView(view)
            transition(sceneInfo ?: defaultScenes.enter)
        }

        fun exit(sceneInfo: SceneInfo? = null){
            removeOnComplete()
            transition(sceneInfo ?: defaultScenes.exit)
        }

        fun popEnter(sceneInfo: SceneInfo? = null) {
            container.addView(view)
            transition(sceneInfo ?: defaultScenes.popEnter)
        }

        fun popExit(sceneInfo: SceneInfo? = null) {
            removeOnComplete()
            transition(sceneInfo ?: defaultScenes.popExit)
        }

        fun isActive(): Boolean{
            for (index in container.childCount downTo 0) {
                if (container.getChildAt(index) == view) return true
            }
            return false
        }

        private fun removeOnComplete(){
            view.setTransitionListener(object : TransitionAdapter() {
                //onTransitionCompleted only triggers when transitionToEnd() is used i.e. when progress is 1 so we use onTransitionChange instead checking for 0 and 1 progress
                override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, progress: Float) {
                    if (progress <= 0f || progress >= 1f) {
                        Handler().post {
                            view.setTransitionListener(null)
                            container.removeView(view)
                        }
                    }
                }
            })
        }

        private fun transition(sceneInfo: SceneInfo) = transition(sceneInfo.id, sceneInfo.reverse)

        private fun transition(sceneResId: Int, reverse: Boolean = false){
            with (view) {
                loadLayoutDescription(sceneResId)

                if (reverse) {
                    transitionToStart()
                } else {
                    transitionToEnd()
                }
            }
        }

    }

    data class SceneInfo(val id: Int, val reverse: Boolean)
    private data class DefaultScenes(val enter: SceneInfo, val exit: SceneInfo, val popEnter: SceneInfo, val popExit: SceneInfo){
        companion object {
            fun inferFromResourceIds(enterResId: Int, exitResId: Int? = null, popEnterResId: Int? = null, popExitResId: Int? = null): DefaultScenes {
                //If not found use defaultEnterScene reversed
                val exit = exitResId?.notReversed() ?: enterResId.reversed()

                //If not found use defaultEnterScene
                val popEnter = popEnterResId ?: enterResId

                /**
                 * When defaultPopExitScene is not found
                 *  - Use defaultPopEnterScene reversed
                 *  - Or use defaultExitScene
                 *  - Or use defaultEnterScene reversed
                 */
                val popExit = popExitResId?.notReversed() ?: popEnterResId?.reversed() ?: exit

                return DefaultScenes(enterResId.notReversed(), exit, popEnter.notReversed(), popExit)
            }

            private fun Int.reversed() = SceneInfo(this, true)
            private fun Int.notReversed() = SceneInfo(this, false)
        }
    }

    data class Extras(val enter: SceneInfo? = null, val exit: SceneInfo? = null, val popEnter: SceneInfo? = null, val popExit: SceneInfo? = null): Navigator.Extras
}