package com.heyanle.easybangumi4.cartoon.star

import com.heyanle.easy_i18n.R
import com.heyanle.easybangumi4.base.preferences.android.AndroidPreferenceStore
import com.heyanle.easybangumi4.cartoon.entity.CartoonInfo
import com.heyanle.easybangumi4.cartoon.entity.CartoonTag
import com.heyanle.easybangumi4.cartoon.entity.CartoonTagWrapper
import com.heyanle.easybangumi4.cartoon.repository.db.dao.CartoonInfoDao
import com.heyanle.easybangumi4.cartoon.tag.CartoonTagsController
import com.heyanle.easybangumi4.ui.common.proc.FilterState
import com.heyanle.easybangumi4.utils.jsonTo
import com.heyanle.easybangumi4.utils.stringRes
import com.heyanle.easybangumi4.utils.toJson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Created by heyanle on 2024/6/8.
 * https://github.com/heyanLE
 */
class CartoonStarController(
    private val cartoonInfoDao: CartoonInfoDao,
    private val cartoonTagsController: CartoonTagsController,
    private val sharePreferenceStore: AndroidPreferenceStore,
) {

    companion object {
        const val DEFAULT_STATE_ID = -3
    }


    private val scope = MainScope()

    private val tagSortFilterStateItemShare =
        sharePreferenceStore.getString("cartoon_tag_sort_filter_state_map", "{}")

    val tagSortFilterStateItem = tagSortFilterStateItemShare.flow().map {
        it.jsonTo<Map<Int, CartoonTagWrapper.TagSortFilterStateItem>>() ?: emptyMap()
    }.map {
        if (it.isEmpty()) {
            it + (DEFAULT_STATE_ID to CartoonTagWrapper.TagSortFilterStateItem.default)
        } else {
            it
        }
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        tagSortFilterStateItemShare.get().jsonTo<Map<Int, CartoonTagWrapper.TagSortFilterStateItem>>() ?: mapOf((DEFAULT_STATE_ID to CartoonTagWrapper.TagSortFilterStateItem.default))
    )


    val flowCartoonTag: Flow<Map<CartoonTagWrapper, List<CartoonInfo>>> = combine(
        tagSortFilterStateItem,
        cartoonTagsController.tagsList.distinctUntilChanged(),
        cartoonInfoDao.flowAllStar().distinctUntilChanged(),
    ) { tagSortFilterState, tagList, cartoonInfoList ->
        val defaultSortFiler =
            tagSortFilterState[DEFAULT_STATE_ID] ?: CartoonTagWrapper.TagSortFilterStateItem.default

        val tagMap = HashMap<Int, CartoonTag>()
        tagList.forEach {
            tagMap[it.id] = it
        }

        if (!tagMap.containsKey(CartoonTagsController.ALL_TAG_ID)){
            tagMap[CartoonTagsController.ALL_TAG_ID] =  CartoonTag(
                CartoonTagsController.ALL_TAG_ID, stringRes(
                    R.string.all_word), -1)
        }
        val allTag = tagMap[CartoonTagsController.ALL_TAG_ID] ?: CartoonTag(
            CartoonTagsController.ALL_TAG_ID, stringRes(
                R.string.all_word), -1)
        val temp = HashMap<CartoonTag, ArrayList<CartoonInfo>>()
        val res = HashMap<CartoonTagWrapper, List<CartoonInfo>>()



        for (entry in tagMap.entries) {
            // 空列表也加上
            temp[entry.value] = arrayListOf()
        }

        val allList = temp[allTag]?: arrayListOf()
        allList.addAll(cartoonInfoList)
        temp[allTag] = allList

        // 打包
        cartoonInfoList.forEach { cartoon ->
            cartoon.tagsIdList.forEach { tagId ->
                val tag = tagMap[tagId]
                if (tag != null) {
                    val oldList = temp[tag] ?: arrayListOf()
                    oldList.add(cartoon)
                    temp[tag] = oldList
                }

            }
        }

        // 排序 过滤 置顶
        for (cartoonItem in temp.entries) {
            val tag = cartoonItem.key
            var cartoonList: List<CartoonInfo> = cartoonItem.value


            var state = tagSortFilterState[tag.id]
            if (state == null || !state.isCustomSetting){
                state = defaultSortFiler.copy(tagId = tag.id, isCustomSetting = false)
            }
            state = state.copy(tagId = tag.id)

            val currentSort = CartoonInfoSortFilterConst.sortByList.firstOrNull() {
                it.id == state.sortId
            } ?: CartoonInfoSortFilterConst.sortByStarTime
            val isSortReverse = state.isReverse

            val onFilter = CartoonInfoSortFilterConst.filterWithList.filter {
                state.filterState[it.id] == FilterState.STATUS_ON
            }
            val excludeFilter = CartoonInfoSortFilterConst.filterWithList.filter {
                state.filterState[it.id] == FilterState.STATUS_EXCLUDE
            }

            cartoonList = cartoonList.filter {
                var check = true
                for (filterWith in onFilter) {
                    if (!filterWith.filter(it)) {
                        check = false
                        break
                    }
                }
                if (!check) {
                    return@filter false
                }
                for (filterWith in excludeFilter) {
                    if (filterWith.filter(it)) {
                        check = false
                        break
                    }
                }
                check
            }

            val upList =
                cartoonList.filter { it.isUp() }
                    .sortedWith { o1, o2 -> (o2.upTime - o1.upTime).toInt() }
            val normalList = cartoonList.filter { !it.isUp() }.sortedWith() { o1, o2 ->
                val res = currentSort.comparator.compare(o1, o2)
                if (isSortReverse) -res else res
            }

            res[CartoonTagWrapper(tag, state)] = upList + normalList
        }
        res
    }



    fun changeState(tagId: Int, item: CartoonTagWrapper.TagSortFilterStateItem) {
        val current = tagSortFilterStateItem.value.toMutableMap()
        if (item.isCustomSetting) {
            current[tagId] = item.copy(tagId = tagId)
            tagSortFilterStateItemShare.set(current.toJson())
        }else{
            current[DEFAULT_STATE_ID] = item
            current[tagId] = item
            tagSortFilterStateItemShare.set(current.toJson())
        }
    }


}