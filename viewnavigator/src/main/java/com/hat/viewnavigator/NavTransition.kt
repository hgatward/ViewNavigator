package com.hat.viewnavigator

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.os.Handler
import android.transition.AutoTransition
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

interface NavTransition<From : View, To : View> : (NavTransition.Context, ViewGroup, From, To) -> Unit {
    override fun invoke(transitionContext: Context, container: ViewGroup, from: From, to: To) =
        transitionContext.run { transition(container, from, to) }

    fun Context.transition(container: ViewGroup, from: From, to: To)

    interface Context {
        fun dispatchTransitionStarted()
        fun dispatchTransitionEnded()

        open class TransitionListener(private val context: Context) :
            Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                context.dispatchTransitionStarted()
            }

            override fun onTransitionResume(transition: Transition) {}
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionCancel(transition: Transition) {}

            override fun onTransitionEnd(transition: Transition) {
                context.dispatchTransitionEnded()
            }
        }
    }

    class ToEndScene<From : View, To : View>(private val transition: Transition = AutoTransition()) :
        NavTransition<From, To> {
        override fun Context.transition(container: ViewGroup, from: From, to: To) {
            val scene = Scene(container, to)
            TransitionManager.go(scene, transition.addListener(Context.TransitionListener(this)))
        }
    }


    class ConstraintSets<From : ConstraintLayout, To : ConstraintLayout>(
        private val enterFromConstraints: ConstraintSet,
        private val exitToConstraints: ConstraintSet,
        private val transition: Transition = AutoTransition()
    ) : NavTransition<From, To> {
        override fun Context.transition(container: ViewGroup, from: From, to: To) {
            dispatchTransitionStarted()

            val fromConstraints = ConstraintSet().apply { clone(from) }
            val toConstraints = ConstraintSet().apply { clone(to) }

            container.addView(to)
            enterFromConstraints.applyTo(to)
            Handler().post {
                TransitionManager.beginDelayedTransition(
                    container,
                    transition.addListener(Listener(this, container, from, to, fromConstraints, toConstraints))
                )
                toConstraints.applyTo(to)
                exitToConstraints.applyTo(from)
            }
        }


        private class Listener(
            context: Context,
            private val container: ViewGroup,
            private val from: ConstraintLayout,
            private val to: ConstraintLayout,
            private val fromConstraints: ConstraintSet,
            private val toConstraints: ConstraintSet
        ) : Context.TransitionListener(context) {
            override fun onTransitionEnd(transition: Transition) {
                container.removeView(from)

                //Reset constraints
                fromConstraints.applyTo(from)
                toConstraints.applyTo(to)
                super.onTransitionEnd(transition)
            }
        }
    }

    class ToEndConstraints<From : ConstraintLayout, To : ConstraintLayout>(private val transition: Transition = AutoTransition()) :
        NavTransition<From, To> {
        override fun Context.transition(container: ViewGroup, from: From, to: To) {
            val fromConstraints = ConstraintSet().apply { clone(from) }
            val toConstraints = ConstraintSet().apply { clone(to) }

            TransitionManager.beginDelayedTransition(
                container,
                transition.addListener(Listener(this, container, from, to, fromConstraints))
            )
            toConstraints.applyTo(from)
        }

        private class Listener(
            context: Context,
            private val container: ViewGroup,
            private val from: ConstraintLayout,
            private val to: ConstraintLayout,
            private val fromConstraints: ConstraintSet
        ) : Context.TransitionListener(context) {
            override fun onTransitionEnd(transition: Transition) {
                super.onTransitionEnd(transition)
                transition.removeListener(this)

                container.addView(to)
                container.removeView(from)

                //Revert from constraints to what they were before the transition
                fromConstraints.applyTo(from)
            }
        }
    }

    class PropertyAnimators<From : View, To : View>(
        private val enterAnim: Animator,
        private val exitAnim: Animator
    ) : NavTransition<From, To> {
        constructor(context: android.content.Context, enterAnimId: Int, exitAnimId: Int) : this(
            AnimatorInflater.loadAnimator(
                context,
                enterAnimId
            ), AnimatorInflater.loadAnimator(context, exitAnimId)
        )

        override fun Context.transition(container: ViewGroup, from: From, to: To) {
            dispatchTransitionStarted()
            container.addView(to)

            enterAnim.setTarget(to)
            exitAnim.setTarget(from)

            exitAnim.addListener(ExitListener(this, container, from))

            enterAnim.start()
            exitAnim.start()
        }

        class ExitListener(
            private val context: Context,
            private val container: ViewGroup,
            private val view: View
        ) : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animation.removeListener(this)
                container.removeView(view)
                context.dispatchTransitionEnded()
            }
        }
    }

    class ViewTweens<From : View, To : View>(
        private val enterAnim: Animation,
        private val exitAnim: Animation
    ) :
        NavTransition<From, To> {
        constructor(context: android.content.Context, enterAnimId: Int, exitAnimId: Int) : this(
            AnimationUtils.loadAnimation(
                context,
                enterAnimId
            ), AnimationUtils.loadAnimation(context, exitAnimId)
        )

        override fun Context.transition(container: ViewGroup, from: From, to: To) {
            val contextDispatcher = ContextDispatcher(this)
            enterAnim.setAnimationListener(ContextDispatcher.Listener(contextDispatcher))
            exitAnim.setAnimationListener(ExitAnimListener(contextDispatcher, container, from))

            from.startAnimation(exitAnim)

            container.addView(to)
            to.startAnimation(enterAnim)
        }



        private class ExitAnimListener(
            contextDispatcher: ContextDispatcher,
            private val container: ViewGroup,
            private val view: View
        ) : ContextDispatcher.Listener(contextDispatcher) {
            override fun onAnimationEnd(animation: Animation) {
                Handler().post {
                    container.removeView(view)
                }

                super.onAnimationEnd(animation)
            }
        }

        private class ContextDispatcher(private val context: Context) {
            private val runningAnimations: MutableSet<Animation> = mutableSetOf()

            fun add(animation: Animation) {
                context.dispatchTransitionStarted()
                runningAnimations.add(animation)
            }

            fun remove(animation: Animation) {
                runningAnimations.remove(animation)
                if (runningAnimations.size == 0) context.dispatchTransitionEnded()
            }

            open class Listener(private val contextDispatcher: ContextDispatcher) :
                Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    contextDispatcher.remove(animation)
                }

                override fun onAnimationStart(animation: Animation) {
                    contextDispatcher.add(animation)
                }
            }
        }
    }
}

