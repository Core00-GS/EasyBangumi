package com.heyanle.easybangumi4.extension

import android.app.Application
import com.heyanle.easybangumi4.extension.store.ExtensionStoreController
import com.heyanle.easybangumi4.extension.store.ExtensionStoreDispatcher
import com.heyanle.easybangumi4.extension.store.ExtensionStoreInfoRepository
import com.heyanle.easybangumi4.utils.getCachePath
import com.heyanle.easybangumi4.utils.getFilePath
import com.heyanle.easybangumi4.utils.getInnerFilePath
import com.heyanle.extension_api.IconFactory
import com.heyanle.inject.api.InjectModule
import com.heyanle.inject.api.InjectScope
import com.heyanle.inject.api.addAlias
import com.heyanle.inject.api.addSingletonFactory
import com.heyanle.inject.api.get

/**
 * Created by heyanlin on 2023/11/1.
 */
class ExtensionModule(
    private val application: Application
) : InjectModule {

    private val extensionPath = application.getInnerFilePath("extension")
    private val extensionCachePath = application.getCachePath("extension")
    private val storePath = application.getFilePath("extension-store")
    private val storeCachePath = application.getCachePath("extension-store")
    override fun InjectScope.registerInjectables() {
        addSingletonFactory {
            ExtensionController(application, extensionPath, extensionCachePath)
        }

        addSingletonFactory {
            ExtensionIconFactoryImpl(get())
        }
        addAlias<ExtensionIconFactoryImpl, IconFactory>()

        addSingletonFactory {
            ExtensionStoreInfoRepository()
        }

        addSingletonFactory {
            ExtensionStoreDispatcher(storeCachePath, storePath, extensionPath, get(), get())
        }

        addSingletonFactory {
            ExtensionStoreController(get(), get())
        }
    }
}