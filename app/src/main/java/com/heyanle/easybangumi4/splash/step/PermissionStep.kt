package com.heyanle.easybangumi4.splash.step

import androidx.compose.runtime.Composable
import com.heyanle.easybangumi4.APP
import com.heyanle.easybangumi4.App
import com.heyanle.easybangumi4.splash.SplashGuildController

/**
 * Created by heyanlin on 2024/7/5.
 */
class PermissionStep(
    private val splashGuildController: SplashGuildController
) : BaseStep {

    override fun need(first: Boolean): Boolean {
        return first
    }

    @Composable
    override fun Compose() {

    }
}