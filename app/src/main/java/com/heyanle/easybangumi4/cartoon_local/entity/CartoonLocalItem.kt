package com.heyanle.easybangumi4.cartoon_local.entity

import androidx.annotation.WorkerThread
import com.heyanle.easybangumi4.cartoon.entity.CartoonInfo
import com.heyanle.easybangumi4.cartoon.repository.db.dao.CartoonTagDao
import com.heyanle.easybangumi4.cartoon_local.source.LocalSource
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.utils.jsonTo
import com.heyanle.inject.api.get
import com.heyanle.inject.core.Inject
import com.hippo.unifile.UniFile
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by heyanle on 2024/7/7.
 * https://github.com/heyanLE
 */
data class CartoonLocalItem (
    // folder uri
    val folderUri: String,
    // tvshow.nfo file uri
    val nfoUri: String,
    // ext.json file uri
    val extUri: String?,

    val uuid: String,

    // form tvshow.nfo
    val title: String,
    val desc: String,
    val cover: String,
    val genre: List<String>,

    val episodes: List<CartoonLocalEpisode>,
){

    companion object {
        const val TAG = "CartoonLocalItem"
        const val TV_SHOW_NFO_FILE_NAME = "tvshow.nfo"
        const val EXT_JSON_FILE_NAME = "ext.json"

        @WorkerThread
        fun fromFolder(uniFile: UniFile): CartoonLocalItem? {
            if (!uniFile.isDirectory){
                return null
            }
            val nfoFile = uniFile.findFile(TV_SHOW_NFO_FILE_NAME)
            val extFile = uniFile.findFile(EXT_JSON_FILE_NAME)

            if (nfoFile == null){
                return null
            }

            val jsoup = Jsoup.parse(nfoFile.openInputStream().reader().readText())
            val tvShow = jsoup.getElementsByTag("tvshow").first() ?: return null
            val title = tvShow.getElementsByTag("title").first()?.text() ?: return null
            val desc = tvShow.getElementsByTag("plot").first()?.text() ?: ""
            val cover = tvShow.getElementsByTag("art").first()?.getElementsByTag("poster")?.text() ?: ""
            val genre = tvShow.getElementsByTag("tag").map { it.text() }

            val episodes = uniFile.listFiles()?.mapNotNull { CartoonLocalEpisode.fromFile(uniFile, it) } ?: emptyList()
            return CartoonLocalItem(
                folderUri = uniFile.uri.toString(),
                nfoUri = nfoFile.uri.toString(),
                extUri = extFile?.uri?.toString(),
                title = title,
                desc = desc,
                cover = cover,
                genre = genre,
                episodes = episodes,
                uuid = uniFile.name ?: return null
            )


        }
    }

}

data class CartoonLocalEpisode(
    val title: String,
    val episode: Int,
    val addTime: String,

    val mediaUri: String,
    val nfoUri: String,
){
    companion object {

        @WorkerThread
        fun fromFile(folder: UniFile, media: UniFile): CartoonLocalEpisode? {
            val mediaName = media.name ?: return null
            if (!mediaName.endsWith(".mp4") && !mediaName.endsWith(".mkv")){
                // 暂时只解析 MP4 和 MKV 结尾的文件
                return null
            }
            val n = mediaName.replace(".mp4", "").replace(".mkv", "")
            val nfo = folder.findFile("${n}.nfo")
            var title: String = ""
            var addTime: String = ""
            var episode: Int = 0
            var completely = false
            if (nfo?.exists() == true){
                // 获取 nfo 文件
                val jsoup = Jsoup.parse(nfo.openInputStream().reader().readText())
                val episodedetails = jsoup.getElementsByTag("episodedetails").first()
                if (episodedetails != null){
                    title = episodedetails.getElementsByTag("title").first()?.text() ?: ""
                    addTime = episodedetails.getElementsByTag("dateadded").first()?.text() ?: ""
                    episode = episodedetails.getElementsByTag("episode").first()?.text()?.toIntOrNull() ?: 0
                    completely = true
                }

            }
            if (!completely){
                // 根据名称刮削
                title = n.replace(folder.name?:"", "").trim()
                // 正则提取 n 中最后一个 E 字符后面的数字
                val e = n.substringAfterLast("E").trimEnd().toIntOrNull()
                if (e != null){
                    episode = e
                }
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date: String = format.format(media.lastModified())
                addTime = date
            }


            return CartoonLocalEpisode(
                title = title,
                episode = episode,
                addTime = addTime,
                mediaUri = media.uri.toString(),
                nfoUri = nfo?.uri?.toString() ?: ""
            )

        }
    }
}