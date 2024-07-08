package com.heyanle.easybangumi4.ui.cartoon_play.view_model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.heyanle.easybangumi4.cartoon.entity.CartoonInfo
import com.heyanle.easybangumi4.cartoon.entity.PlayLineWrapper
import com.heyanle.easybangumi4.cartoon_download.entity.CartoonDownloadReq
import com.heyanle.easybangumi4.cartoon_local.CartoonLocalController
import com.heyanle.easybangumi4.cartoon_local.entity.CartoonLocalItem
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.inject.core.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A 是否要下载到本地番源中的已有剧集 是->B 否->C
 * B 请选择本地番源中的目标剧集 -> C
 * C 请设定各视频对应刮削数据 设定各个视频对应的目标剧集中的目标集数和标题，这里目标集数不允许重复（全局加锁统一控制）
 * Created by heyanle on 2024/7/8.
 * https://github.com/heyanLE
 */
class CartoonDownloadReqModel(
    private val cartoonInfo: CartoonInfo,
    private val playerLineWrapper: PlayLineWrapper,
    private val episodes: List<Episode>,
) {

    private val scope = MainScope()

    data class State (
        // 1 -> 是否要下载到本地番源中的已有剧集
        // 2 -> 正在以当前番剧信息创建剧集
        // 3 -> 请选择本地番源中的目标剧集
        // 4 -> 请设定各视频对应刮削数据
        val status: Int,
        val loading: Boolean,

        val itemList: List<CartoonLocalItem>? = null,

        // 目标剧集
        val localItem: CartoonLocalItem? = null,
        // 目标剧集已有集数，防止重复下载
        val episodeList: Set<Int>? = null,

        // 番剧下载请求（包括刮削数据）
        val reqList: List<CartoonDownloadReq>? = null
    )
    private val _stateFlow = MutableStateFlow(State(1, true))
    val stateFlow = _stateFlow.asStateFlow()

    private val cartoonLocalController: CartoonLocalController by Inject.injectLazy()

    init {
        scope.launch {
            combine(
                stateFlow.filter { it.status == 3 }.map { it.status }.distinctUntilChanged(),
                cartoonLocalController.flowState
            ){status, localCartoonState ->
                localCartoonState
            }.collectLatest { state ->
                if (state.loading) {
                    _stateFlow.update {
                        it.copy(loading = true)
                    }
                } else {
                    _stateFlow.update {
                        it.copy(loading = false, itemList = state.localCartoonItem.values.toList())
                    }
                }
            }
        }
        scope.launch {
            stateFlow.filter { it.status == 4 && it.loading && it.localItem != null}.distinctUntilChanged().collectLatest {
                scope.launch(Dispatchers.IO) {
                    cartoonLocalController.getLocalEpisodes(it.localItem?.uuid?:return@launch).let { set ->
                        _stateFlow.update {
                            it.copy(loading = false, episodeList = set)
                        }
                    }
                }
            }
        }

    }


    fun newLocal(){
        _stateFlow.update {
            it.copy(status = 2, loading = true)
        }
        scope.launch {
            val item = cartoonLocalController.newLocal(cartoonInfo)
            selectLocal(item)
        }

    }

    fun prepareToSelectLocal() {
        _stateFlow.update {
            it.copy(status = 3, loading = true)
        }
    }

    fun selectLocal(cartoonLocalItem: CartoonLocalItem){
        _stateFlow.update {
            it.copy(
                status = 4,
                localItem = cartoonLocalItem,
                loading = true,
            )
        }
    }

    fun changeTitle(req: CartoonDownloadReq, title: String) {}
    fun changeEpisode(req: CartoonDownloadReq, episode: Int) {}



}