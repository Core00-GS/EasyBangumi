package com.heyanle.easybangumi4.cartoon_local

import android.net.Uri
import com.heyanle.easybangumi4.APP
import com.heyanle.easybangumi4.R
import com.heyanle.easybangumi4.cartoon_local.entity.CartoonLocalItem
import com.heyanle.easybangumi4.ui.common.moeSnackBar
import com.heyanle.easybangumi4.utils.CoroutineProvider
import com.heyanle.easybangumi4.utils.stringRes
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 本地番源
 * Created by heyanlin on 2024/7/4.
 */
class CartoonLocalController(
    private val localCartoonPreference: LocalCartoonPreference,
) {

    data class State(
        val loading: Boolean = true,
        val errorMsg: String? = null,
        val error: Throwable? = null,

        val localCartoonItem: Map<String, CartoonLocalItem> = mapOf()
    )

    private val _flowState = MutableStateFlow(State())
    val flowState = _flowState.asStateFlow()

    private val loadSingleDispatcher = CoroutineProvider.CUSTOM_SINGLE
    private val scope = MainScope()

    private var lastLoadJob: Job? = null

    init {
        scope.launch {
            localCartoonPreference.realLocalUri.collectLatest {
                refresh(it)
            }
        }

    }

    fun refresh(
        uri: Uri? = null
    ) {
        lastLoadJob?.cancel()
        lastLoadJob = scope.launch(loadSingleDispatcher) {
            innerRefresh(uri)
        }
    }

    suspend fun innerRefresh(
        uri: Uri? = null
    ) {
        _flowState.update {
            it.copy(loading = true)
        }
        try {
            val ru = uri ?: localCartoonPreference.realLocalUri.value
            val uniFile = UniFile.fromUri(APP, ru)
            if (uniFile == null) {
                _flowState.update {
                    it.copy(
                        loading = false,
                        errorMsg = "无法打开文件夹"
                    )
                }
                stringRes(com.heyanle.easy_i18n.R.string.local_folder_error).moeSnackBar()
                return
            }
            val items = uniFile.listFiles()?.mapNotNull {
                CartoonLocalItem.fromFolder(it)
            } ?: emptyList()
            _flowState.update {
                it.copy(
                    loading = false,
                    errorMsg = null,
                    error = null,
                    localCartoonItem = items.associateBy { it.folderUri }
                )
            }
        }catch (e: Throwable){
            _flowState.update {
                it.copy(
                    loading = false,
                    errorMsg = e.message,
                    error = e
                )
            }
            e.printStackTrace()
        }



    }


}