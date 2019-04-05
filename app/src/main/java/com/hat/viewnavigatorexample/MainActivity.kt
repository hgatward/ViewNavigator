package com.hat.viewnavigatorexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.ChangeBounds
import com.hat.viewnavigator.CompositeNavHostFragment
import com.hat.viewnavigator.NavTransition
import com.hat.viewnavigator.ViewNavigator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val destinations = ViewNavigator.destinations {
            destination<SquareLittle>(R.id.squareLittle) {
                view(SquareLittle(this@MainActivity))
                defaultTransitions(NavTransition.ToEndScene(ChangeBounds()))
            }

            destination<SquareBig>(R.id.squareBig) {
                view(SquareBig(this@MainActivity))
                //defaultTransitions(NavTransition.ToEndScene(ChangeBounds()))
            }
        }

        (navHost as CompositeNavHostFragment).install(this, destinations, NavTransition.ViewTweens(this, R.anim.view_nav_default_enter_anim, R.anim.view_nav_default_exit_anim))
        //navHost.setGraph(this, destinations, NavTransition.ViewTweens(this, R.anim.view_nav_default_enter_anim, R.anim.view_nav_default_exit_anim))
    }
}
