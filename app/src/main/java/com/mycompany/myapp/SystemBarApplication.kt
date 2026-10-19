package com.md.hasenstro.app

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class SystemBarApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {
                    if (activity.javaClass.simpleName == "MainActivity") return

                    val window = activity.window

                    @Suppress("DEPRECATION")
                    window.clearFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                    )

                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility =
                        window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv() and
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION.inv() and
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE.inv()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.setDecorFitsSystemWindows(true)
                    }
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }
}
