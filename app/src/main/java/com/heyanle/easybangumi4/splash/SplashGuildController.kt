package com.heyanle.easybangumi4.splash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.heyanle.easybangumi4.base.preferences.android.AndroidPreferenceStore
import com.heyanle.easybangumi4.cartoon_local.CartoonLocalController
import com.heyanle.easybangumi4.splash.step.BaseStep
import com.heyanle.easybangumi4.splash.step.LocalStep
import com.heyanle.easybangumi4.splash.step.PermissionStep
import com.heyanle.easybangumi4.splash.step.ThemeStep
import com.heyanle.inject.api.get
import com.heyanle.inject.core.Inject
import com.heyanle.okkv2.core.okkv

/**
 * Created by heyanlin on 2024/7/4.
 */
class SplashGuildController(
    private val androidPreferenceStore: AndroidPreferenceStore,
    private val localController: CartoonLocalController,
) {

    val stepList = listOf<BaseStep>(
        ThemeStep(this),
        LocalStep(this, localController),
        PermissionStep(this)
    )


    var firstGuild by okkv("first_guild", def = true)
    var currentStep by mutableStateOf(0)
    var canNextStep by mutableStateOf(false)

    fun stepCompletely(step: Int){

    }



}