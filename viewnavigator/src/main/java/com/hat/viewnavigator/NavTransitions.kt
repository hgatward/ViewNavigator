package com.hat.viewnavigator

import android.animation.Animator
import android.transition.AutoTransition
import android.view.View
import android.view.animation.Animation
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.Navigator

open class NavTransitions<From : View, To : View>(
    val transition: NavTransition<From, To>,
    val popTransition: NavTransition<To, From>
) : Navigator.Extras {
    companion object {
        fun <From : View, To : View> toEndScene(
            transition: android.transition.Transition = AutoTransition(),
            popTransition: android.transition.Transition = transition.clone()
        ) = NavTransitions<From, To>(NavTransition.ToEndScene(transition), NavTransition.ToEndScene(popTransition))

        fun <From : ConstraintLayout, To : ConstraintLayout> toEndConstraints(
            transition: android.transition.Transition = AutoTransition(),
            popTransition: android.transition.Transition = transition.clone()
        ) = NavTransitions<From, To>(NavTransition.ToEndConstraints(transition), NavTransition.ToEndConstraints(popTransition))

        fun <From: ConstraintLayout, To: ConstraintLayout> constraints(
            enterFromConstraints: ConstraintSet,
            exitToConstraints: ConstraintSet,
            popEnterFromConstraints: ConstraintSet,
            popExitConstraints: ConstraintSet,
            transition: android.transition.Transition = AutoTransition(),
            popTransition: android.transition.Transition = transition.clone()
        ) = NavTransitions<From, To>(
            NavTransition.ConstraintSets(enterFromConstraints, exitToConstraints, transition),
            NavTransition.ConstraintSets(popEnterFromConstraints, popExitConstraints, popTransition)
        )

        fun <From : View, To : View> viewTweens(
            enterAnim: Animation,
            exitAnim: Animation,
            popEnterAnim: Animation,
            popExitAnim: Animation
        ) = NavTransitions<From, To>(
            NavTransition.ViewTweens(enterAnim, exitAnim),
            NavTransition.ViewTweens(popEnterAnim, popExitAnim)
        )

        fun <From : View, To : View> animators(
            enterAnim: Animator,
            exitAnim: Animator,
            popEnterAnim: Animator,
            popExitAnim: Animator
        ) = NavTransitions<From, To>(
            NavTransition.PropertyAnimators(enterAnim, exitAnim),
            NavTransition.PropertyAnimators(popEnterAnim, popExitAnim)
        )
    }
}