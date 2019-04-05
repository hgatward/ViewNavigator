package com.hat.viewnavigator

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator

@Navigator.Name("fragment")
class FullyPoppableFragmentNavigator(context: Context, private val manager: FragmentManager, containerId: Int) :
    FragmentNavigator(context, manager, containerId) {
    override fun popBackStack(): Boolean {
        val popped = super.popBackStack()

        if (popped && manager.backStackEntryCount == 0){
            if (manager.fragments.size != 1) throw IllegalStateException("There should only be 1 fragment here")

            manager.beginTransaction().remove(manager.fragments[0]).commit()
        }

        return popped
    }
}