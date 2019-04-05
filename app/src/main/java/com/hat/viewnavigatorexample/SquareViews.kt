package com.hat.viewnavigatorexample

import android.animation.AnimatorInflater
import android.content.Context
import android.transition.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.findNavController
import com.hat.viewnavigator.NavTransitions
import kotlinx.android.synthetic.main.square_little.view.*

class SquareLittle(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs){
    init {
        View.inflate(context, R.layout.square_little, this)
        val originalConstraints = ConstraintSet().apply{ clone(this@SquareLittle) }

        val extras = NavTransitions.sceneTransitions<SquareLittle, SquareBig>(ChangeBounds())
        val constraintSetExtras = NavTransitions.constraintSetTransitions<SquareLittle, SquareBig>(ChangeBounds())

        val enterAnim = AnimationUtils.loadAnimation(context, R.anim.view_nav_default_enter_anim)
        val exitAnim = AnimationUtils.loadAnimation(context, R.anim.view_nav_default_exit_anim)

        val animationExtras = NavTransitions.viewAnimations<SquareLittle, SquareBig>(enterAnim, exitAnim, enterAnim, exitAnim)

        val enterAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_enter_anim)
        val exitAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_exit_anim)
        val popEnterAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_pop_enter_anim)
        val popExitAnimator = AnimatorInflater.loadAnimator(context, R.animator.little_to_big_pop_exit_anim)

        val animatorExtras = NavTransitions.animators<SquareLittle, SquareBig>(enterAnimator, exitAnimator, popEnterAnimator, popExitAnimator)

        val bigSquareEntersFromConstraints = ConstraintSet().apply { clone(context, R.layout.square_big_out_constraints) }
        val littleSquareExitsToConstrains = ConstraintSet().apply { clone(context, R.layout.square_little_out_constraints) }
        val constraintSet2Extras = NavTransitions.constraintSetTransitions2<SquareLittle, SquareBig>(AutoTransition(), AutoTransition(), bigSquareEntersFromConstraints, littleSquareExitsToConstrains, littleSquareExitsToConstrains, bigSquareEntersFromConstraints)

        square.setOnClickListener { findNavController().navigate(R.id.squareBig) } //, null, null, constraintSetExtras) }
    }
}

