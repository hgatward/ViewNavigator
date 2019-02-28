# MotionNavigator
A navigator for motion layouts.

### Quick example

Add this to your root layout (e.g. activity_main)
```xml
<com.hat.motionnavigator.motion.MotionNavHost
    android:id="@+id/navHost"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:navGraph="@navigation/nav_graph" />
```

Create your nav_graph
```xml
<navigation
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/motion_nav_graph"
    app:startDestination="@+id/first_page">

    <motion_layout
        android:id="@+id/first_page"
        app:defaultEnterScene="@xml/first_page_enter"/>


    <motion_layout
        android:id="@+id/second_page"
        app:defaultEnterScene="@xml/second_page_enter"/>
</navigation>
```

Create views (MotionLayouts) when given destinations
```kotlin
navHost.createsViewsBy { destinationId ->
    when (destinationId) {
        R.id.first_page -> //create view for first_page
        R.id.second_page -> //create view for second_page
        else -> throw IllegalStateException("No view for destination: $destinationId")
    }
}
```

### Description
Each motion_layout tag has MotionScene's referred to by the following attributes:
- defaultEnterScene
- defaultExitScene
- defaultPopEnterScene
- defaultPopExitScene

defaultEnterScene is required. All other attribubtes are inferred if not set.
- defaultExitScene will use defaultEnterScene reversed
- defaultPopEnterScene will use defaultEnterScene
- defaultPopExitScene will use defaultPopEnterScene reversed if found, or defaultExitScene (which may be the reverse of defaultEnterScene)

### WIP
This is a work in progress... 

#### TODO:
- Provide MotionScenes as Extras when navigating.
To allow for different transitions when navigating to certain destinations (rather than the default/fallback scenes listed in the nav graph)
- Save/restore state in MotionNavHost
- Testtt - it isn't tested (and breaks)
- ...
