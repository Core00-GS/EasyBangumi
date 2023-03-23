package com.heyanle.easybangumi4.db.entity

import androidx.room.Entity

/**
 * Created by HeYanLe on 2023/3/7 14:55.
 * https://github.com/heyanLE
 */
@Entity(primaryKeys = ["id", "source", "url"])
data class CartoonHistory(

    val id: String,
    val url: String,
    val source: String,

    val name: String,
    val cover: String,
    val intro: String,

    val lastLinesIndex: Int,
    val lastEpisodeIndex: Int,
    val lastLineTitle: String,
    val lastEpisodeTitle: String,
    val lastProcessTime: Long,
    val createTime: Long,
)