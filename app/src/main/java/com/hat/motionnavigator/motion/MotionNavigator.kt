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

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        val previous = stack.peek()
        stack.push(destination)

        destination.enter()
        previous?.exit()

        return destination
    }

    override fun createDestination(): Destination =
        Destination(this, container, viewFactory)

    override fun popBackStack(): Boolean {
        val popped = stack.pollFirst()

        if (popped != null) {
            stack.peekFirst()?.popEnter(container)
            popped.popExit()
            return true
        }

        return false
    }


    class Destination(navigator: Navigator<*>, private val container: ViewGroup, private val viewFactory: (Int) -> MotionLayout): NavDestination(navigator){
        companion object {
            const val NOT_FOUND = 0
        }

        lateinit var view: MotionLayout

        private lateinit var sceneInfo: SceneInfo

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

                val sceneResIds = SceneResIds(enterResId, exitResId, popEnterResId, popExitResId)
                sceneInfo = SceneInfo.inferFromResourceIds(sceneResIds)

                recycle()
            }

            view = viewFactory(id)
        }

        private fun Int.require(attrName: String): Int = if (this == NOT_FOUND) throw IllegalStateException("MotionNavigator.Destination requires a motion scene for attribute $attrName") else this
        private fun Int.nullIfNotFound(): Int? = if (this == NOT_FOUND) null else this

        fun enter(){
            container.addView(view)
            transition(sceneInfo.enter)
        }

        fun exit(){
            removeOnComplete()
            transition(sceneInfo.exit.id, sceneInfo.exit.reverse)
        }

        fun popEnter(container: ViewGroup) {
            container.addView(view)
            transition(sceneInfo.popEnter)
        }

        fun popExit() {
            removeOnComplete()
            transition(sceneInfo.popExit.id, sceneInfo.popExit.reverse)
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

    private data class IdAndReverse(val id: Int, val reverse: Boolean)
    private data class SceneInfo(val enter: Int, val exit: IdAndReverse, val popEnter: Int, val popExit: IdAndReverse){
        companion object {
            fun inferFromResourceIds(sceneResIds: SceneResIds): SceneInfo = with (sceneResIds) {
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

                return SceneInfo(enterResId, exit, popEnter, popExit)
            }

            private fun Int.reversed() = IdAndReverse(this, true)
            private fun Int.notReversed() = IdAndReverse(this, false)
        }
    }

    data class SceneResIds(val enterResId: Int, val exitResId: Int? = null, val popEnterResId: Int? = null, val popExitResId: Int? = null)
    data class Extras(val sceneResIds: SceneResIds): Navigator.Extras{
        constructor(enterResId: Int, exitResId: Int? = null, popEnterResId: Int? = null, popExitResId: Int? = null): this(SceneResIds(enterResId, exitResId, popEnterResId, popExitResId))
    }
}