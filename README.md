# ViewNavigator
A navigator for custom views with transition support.

### Quick example

#### Nav Host
Add this to your root layout (e.g. activity_main)
```xml
<fragment
    android:id="@+id/navHost"
    android:name="com.hat.viewnavigator.CompositeNavHost"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:navGraph="@navigation/view_nav_graph" />
```

#### Nav Graph
Create your nav_graph
```xml
<navigation
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/view_nav_graph"
    app:startDestination="@+id/first_page">

    <view
        android:id="@+id/squareLittle"
        tools:layout="@layout/square_little" />

    <view
        android:id="@+id/squareBig"
        tools:layout="@layout/square_big">    
</navigation>
```

#### Destinations
Set up your destinations and install them on the nav host
```kotlin
val destinations = ViewNavigator.destinations {
    destination<SquareLittle>(R.id.squareLittle) {
        view { SquareLittle(this@MainActivity) }
        defaultTransition { NavTransition.ToEndScene(ChangeBounds()) }
    }

    destination<SquareBig>(R.id.squareBig) {
        view { SquareBig(this@MainActivity) }
    }
}

val fallbackTransition = NavTransition.ViewTweens<View, View>(
    this, 
    R.anim.view_nav_default_enter_anim, 
    R.anim.view_nav_default_exit_anim
)

(navHost as CompositeNavHost).install(this, destinations, fallbackTransition)
```

#### Navigate
```kotlin
val transitions = NavTransitions.toEndScene<SquareLittle, SquareBig>(transition)
findNavController().navigate(R.id.squareBig, null, null, transitions)
```

### Transitions
The NavTransition interface defines a transition between two views.
NavTransitions takes 2 NavTransition instances: transition and popTransition.

val transitions: NavTransitions = ...
Pretend we're on destination A and we call navigate(B, null, null, transitions)

'transition' will be used to transition from A to B
'popTransition' will be stored to be used when the user pops from B back to A.

|Transition|Description|
|----------|-----------|
|NoTransition|Simply adds/removes the new/previous destination views|
|ToEndScene|Transitions from the current scene to the new destination|
|ConstraintSets|Transitions the previous destination to the exitToConstraints and transitions to the new destination from enterFromConstraints|
|ToEndConstraints|Gets the constraints from the new destination. Transitions the previous destination to these constraints before adding the new destination|
|PropertyAnimators|Transitions the new destination using enterAnim and the previous destination using exitAnim|
|ViewTweens|Uses view tween animations|

Often we want to use the same transition and popTransitions. NavTransitions defines a set of static functions to use the above transitions for both the transition and popTransition, i.e.

- toEndScene()
- toEndConstraints()
- constraints()
- viewTweens()
- animators()

#### Custom Transition
You can create your own custom NavTransition classes:
```kotlin
private class CustomTransition: NavTransition<SquareLittle, SquareBig> {
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
```
- Call dispatchTransitionStarted() when the transition starts so the view navigator knows which destinations are mid-transition
- Add 'to' to the container and remove 'from' from the container before the transition ends.
- Call dispatchTransitionEnded() when the transition ends.

### Description


### WIP
This is a work in progress... 
