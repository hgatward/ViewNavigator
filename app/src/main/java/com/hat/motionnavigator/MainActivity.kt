package com.hat.motionnavigator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.first_page.view.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navHost.createsViewsBy { destinationId ->
            when (destinationId) {
                R.id.first_page -> {
                    (LayoutInflater.from(this).inflate(R.layout.first_page, navHost, false) as MotionLayout).apply {
                        secondPageButton.setOnClickListener { findNavController().navigate(R.id.second_page) }
                    }
                }

                R.id.second_page -> {
                    (LayoutInflater.from(this).inflate(R.layout.second_page, navHost, false) as MotionLayout)
                }

                else -> throw IllegalStateException("No view for destination: $destinationId")
            }
        }
    }

    override fun onBackPressed() {
        with (findNavController(R.id.navHost)){
            popBackStack()
            if (currentDestination == null) finishAfterTransition()
        }
    }
}
