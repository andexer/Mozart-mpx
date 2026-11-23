package com.mozartmpx.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mozartmpx.app.ui.navigation.MozartApp
import com.mozartmpx.app.ui.theme.MozartMpxTheme
import com.mozartmpx.app.ui.viewmodel.MozartViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MozartViewModel by viewModels {
        MozartViewModel.factory((application as MozartApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashView ->
                val fadeOut = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)
                val iconView = splashView.iconView
                val animators = buildList {
                    add(fadeOut)
                    if (iconView != null) {
                        add(ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.12f))
                        add(ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.12f))
                    }
                }
                AnimatorSet().apply {
                    playTogether(animators)
                    duration = 260L
                    interpolator = DecelerateInterpolator()
                    doOnEnd { splashView.remove() }
                    start()
                }
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val albumPalette by viewModel.albumPalette.collectAsStateWithLifecycle()
            MozartMpxTheme(albumPalette = albumPalette) {
                MozartApp(viewModel = viewModel)
            }
        }
    }
}
