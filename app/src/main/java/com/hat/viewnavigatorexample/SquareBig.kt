package com.hat.viewnavigatorexample

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.square_big.view.*

class SquareBig(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs){
    init {
        View.inflate(context, R.layout.square_big, this)
        square.setOnClickListener { findNavController().navigate(R.id.action_squareBig_to_aFragment) }
    }
}