package com.heyanle.easybangumi4.ui.download.downloading

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heyanle.easybangumi4.bus.DownloadingBus
import com.heyanle.easybangumi4.cartoon_download.CartoonDownloadBus
import com.heyanle.easybangumi4.cartoon_download.CartoonDownloadDispatcher
import com.heyanle.easybangumi4.cartoon_download.entity.DownloadItem
import com.heyanle.easybangumi4.getter.DownloadItemGetter
import com.heyanle.injekt.core.Injekt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Created by heyanlin on 2023/11/2.
 */
class DownloadingViewModel : ViewModel() {

    private val cartoonDownloadBus: CartoonDownloadBus by Injekt.injectLazy()


    private val downloadItemGetter: DownloadItemGetter by Injekt.injectLazy()
    val downloadingFlow = downloadItemGetter.flowDownloadItem()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val cartoonDownloadDispatcher: CartoonDownloadDispatcher by Injekt.injectLazy()

    val selection = mutableStateMapOf<DownloadItem, Boolean>()

    val removeDownloadItem = mutableStateOf<Collection<DownloadItem>?>(null)

    fun onSelectExit() {
        selection.clear()
    }

    fun onSelectAll() {
        downloadingFlow.value.forEach {
            selection[it] = true
        }
    }

    fun onSelectInvert() {
        val current = selection.toMap()
        downloadingFlow.value.forEach {
            if (current.containsKey(it)) {
                selection.remove(it)
            } else {
                selection[it] = true
            }
        }
    }

    fun info(download: DownloadItem): DownloadingBus.DownloadingInfo {
        return cartoonDownloadBus.getInfo(download.uuid)
    }

    fun click(download: DownloadItem) {
        cartoonDownloadDispatcher.clickDownload(downloadItem = download)
    }

    fun remove(selection: Collection<DownloadItem>) {
        selection.forEach {
            cartoonDownloadDispatcher.removeDownload(it)
        }
    }
}