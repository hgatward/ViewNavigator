package com.hat.viewnavigatorexample

import android.animation.*
import android.content.Context
import android.transition.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.hat.viewnavigator.NavTransition
import com.hat.viewnavigator.NavTransitions
import kotlinx.android.synthetic.main.square_little.view.*

class SquareLittle(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.square_little, this)

        val enterAnim = AnimationUtils.loadAnimation(context, R.anim.view_nav_default_enter_anim)
        val exitAnim = AnimationUtils.loadAnimation(context, R.anim.view_nav_default_exit_anim)

        val enterAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_enter_anim)
        val exitAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_exit_anim)
        val popEnterAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_pop_enter_anim)
        val popExitAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_pop_exit_anim)

        val bigSquareEntersFromConstraints =
            ConstraintSet().apply { clone(context, R.layout.square_big_out_constraints) }
        val littleSquareExitsToConstraints =
            ConstraintSet().apply { clone(context, R.layout.square_little_out_constraints) }

        val transition = TransitionSet().addTransition(ChangeBounds().addTarget(R.id.square))
            .addTransition(Fade().setDuration(300))

        navigateButton.setOnClickListener {
            val extras = when (transitionChoiceGroup.checkedChipId) {
                R.id.toEndSceneChip -> NavTransitions.toEndScene<SquareLittle, SquareBig>(transition)
                R.id.toEndConstraints -> NavTransitions.toEndConstraints()
                R.id.constraints -> NavTransitions.constraints(
                    bigSquareEntersFromConstraints,
                    littleSquareExitsToConstraints,
                    littleSquareExitsToConstraints,
                    bigSquareEntersFromConstraints
                )
                R.id.animators -> NavTransitions.animators(
                    enterAnimator,
                    exitAnimator,
                    popEnterAnimator,
                    popExitAnimator
                )
                R.id.viewTweens -> NavTransitions.viewTweens(enterAnim, exitAnim, enterAnim, exitAnim)
                R.id.customAnimator -> NavTransitions(ExitTransition(), PopEnterTransition())
                else -> null
            }

            if (extras == null) Snackbar.make(this, R.string.noTransitionMessage, Snackbar.LENGTH_SHORT).show()
            else findNavController().navigate(R.id.squareBig, null, null, extras)
        }
    }

    private class ExitTransition: NavTransition<SquareLittle, SquareBig> {
        override fun NavTransition.Context.transition(container: ViewGroup, from: SquareLittle, to: SquareBig) {
            dispatchTransitionStarted()
            container.addView(to)

            val exitAnim = ObjectAnimator.ofPropertyValuesHolder(
                from.square,
                PropertyValuesHolder.ofFloat(View.X, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f)
            ).apply {
                duration = 300
            }

            val enterAnim = ObjectAnimator.ofPropertyValuesHolder(
                to.square,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 500f, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
            ).apply {
                duration = 300
            }

            val fadeOutChipGroup = ObjectAnimator.ofFloat(from.transitionChoiceGroup, View.ALPHA, 0f)

            AnimatorSet().apply {
                playTogether(exitAnim, enterAnim, fadeOutChipGroup)
                addListener(NavTransition.PropertyAnimators.ExitListener(this@transition, container, from))
                start()
            }
        }
    }

    private class PopEnterTransition: NavTransition<SquareBig, SquareLittle> {
        override fun NavTransition.Context.transition(container: ViewGroup, from: SquareBig, to: SquareLittle) {
            dispatchTransitionStarted()
            container.addView(to)

            val popEnterAnim = ObjectAnimator.ofPropertyValuesHolder(
                to.square,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
            ).apply {
                duration = 150
            }

            val popExitAnim = ObjectAnimator.ofPropertyValuesHolder(
                from.square,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 500f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
            ).apply {
                duration = 150
            }

            val slideUpAndFadeInChipGroup = ObjectAnimator.ofPropertyValuesHolder(
                to.transitionChoiceGroup,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 300f, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
            )


            AnimatorSet().apply {
                playTogether(popEnterAnim, popExitAnim, slideUpAndFadeInChipGroup)
                addListener(NavTransition.PropertyAnimators.ExitListener(this@transition, container, from))
                start()
            }
        }
    }
}

