package com.heyanle.easybangumi4.source

import android.os.Build
import com.heyanle.easybangumi4.cartoon.repository.db.dao.CartoonInfoDao
import com.heyanle.easybangumi4.case.ExtensionCase
import com.heyanle.easybangumi4.crash.SourceCrashController
import com.heyanle.easybangumi4.extension.ExtensionInfo
import com.heyanle.easybangumi4.source.bundle.ComponentBundle
import com.heyanle.easybangumi4.source.bundle.SourceBundle
import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.utils.TimeLogUtils
import com.heyanle.easybangumi4.utils.logi
import com.heyanle.extension_api.NativeSupportedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/**
 * 源业务层
 * Created by heyanlin on 2023/10/27.
 */
class SourceController(
    private val extensionCase: ExtensionCase,
    private val sourcePreferences: SourcePreferences,
    private val cartoonInfoDao: CartoonInfoDao,
) {

    companion object {
        val TAG = "SourceController"
    }

    sealed class SourceInfoState {
        data object Loading : SourceInfoState()

        class Info(val info: List<SourceInfo>) : SourceInfoState()
    }

    private val _sourceInfo = MutableStateFlow<SourceInfoState>(SourceInfoState.Loading)
    val sourceInfo = _sourceInfo.asStateFlow()

    private val _configSource = MutableStateFlow<List<ConfigSource>>(emptyList())
    val configSource = _configSource.asStateFlow()

    private val _sourceBundle = MutableStateFlow<SourceBundle?>(null)
    val sourceBundle = _sourceBundle.asStateFlow()


    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val migrateScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)



    init {
        scope.launch {
            extensionCase.flowExtensionState().collectLatest { sta ->
                if(sta.isLoading){
                    _sourceInfo.update {
                        SourceInfoState.Loading
                    }
                }else{
                    TimeLogUtils.i("loadSource start")
                    val it = sta.appExtensions.values + sta.fileExtensionInfo.values
                    val map = hashMapOf<String, Pair<ExtensionInfo.Installed, Source>>()
                    it.filterIsInstance<ExtensionInfo.Installed>().flatMap { exten ->
                        exten.sources.map {
                            exten to it
                        }
                    }.forEach {
                        val old = map[it.second.key]
                        if (old == null || old.second.versionCode <= it.second.versionCode) {
                            map[it.second.key] = it
                        }
                    }
                    val n = map.values.map {
                        val res = loadSource(it.second, it.first)
                        res
                    }
                    _sourceInfo.update {
                        SourceInfoState.Info(n)
                    }
                }
            }
        }
//        scope.launch {
//            _sourceInfo.filterIsInstance<SourceInfoState.Info>()
//                .map { it.info.filterIsInstance<SourceInfo.Migrating>() }
//                .collectLatest {
//                    migrateScope.launch {
//                        it.forEach {
//                            migrate(it)
//                        }
//                    }
//                }
//        }
        scope.launch {
            combine(
                _sourceInfo.filterIsInstance<SourceInfoState.Info>().map { it.info },
                sourcePreferences.configs.stateIn(scope)
            ) { sourceInfo, config ->
                val d = sourceInfo.map {
                    val con =
                        config[it.source.key] ?: SourceConfig(it.source.key, Int.MAX_VALUE, true)
                    ConfigSource(it, con)
                }
                d
            }.collectLatest { list ->
                _configSource.update {
                    list
                }
                _sourceBundle.update {
                    SourceBundle(list)
                }
            }
        }

    }

    private suspend fun loadSource(source: Source, extensionInfo: ExtensionInfo.Installed): SourceInfo {
        TimeLogUtils.i("loadSource ${source.key} start")
        return try {
            val bundle = ComponentBundle(source)
            bundle.init()

//            // 加载 So 咯
            if (source is NativeSupportedSource){
                return SourceInfo.Error(source, extensionInfo, "NativeSupportedSource 已过时，请在 onInit 中加载 so")
            }

            SourceInfo.Loaded(source, extensionInfo, bundle)
        } catch (e: SourceException) {
            SourceInfo.Error(source, extensionInfo, e.msg, e)
        } catch (e: Exception) {
            e.printStackTrace()
            SourceInfo.Error(source, extensionInfo, "加载错误：${e.message}", e)
        }
    }




}