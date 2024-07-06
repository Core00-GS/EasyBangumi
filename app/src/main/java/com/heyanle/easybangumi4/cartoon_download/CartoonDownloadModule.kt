package com.heyanle.easybangumi4.cartoon_download

import android.app.Application
import com.heyanle.easybangumi4.cartoon_download.step.AriaStep
import com.heyanle.easybangumi4.cartoon_download.step.BaseStep
import com.heyanle.easybangumi4.cartoon_download.step.CopyStep
import com.heyanle.easybangumi4.cartoon_download.step.ParseStep
import com.heyanle.easybangumi4.cartoon_download.step.TranscodeStep
import com.heyanle.inject.api.InjectModule
import com.heyanle.inject.api.InjectScope
import com.heyanle.inject.api.InjectionException
import com.heyanle.inject.api.addSingletonFactory
import com.heyanle.inject.api.get

/**
 * Created by heyanlin on 2023/10/2.
 */
class CartoonDownloadModule(
    private val application: Application
) : InjectModule {

    override fun InjectScope.registerInjectables() {
        addSingletonFactory {
            LocalCartoonController(application)
        }

        addSingletonFactory {
            CartoonDownloadController(application, get())
        }

        addSingletonFactory {
            CartoonDownloadDispatcher(application, get(), get())
        }

        addSingletonFactory {
            CartoonDownloadBus(get())
        }

        addScopedPerKeyFactory<BaseStep, String> {
            when(it){
                ParseStep.NAME -> ParseStep(get(), get(), get())
                AriaStep.NAME -> AriaStep(get(), get())
                TranscodeStep.NAME -> TranscodeStep(application, get(), get())
                CopyStep.NAME -> CopyStep(get(), get())
                else -> throw InjectionException("No registered BaseStep with ${it}")
            }
        }
    }
}