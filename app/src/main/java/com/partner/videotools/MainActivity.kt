package com.partner.videotools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ToastUtils
import com.partner.videotools.utils.KWebView
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView.setHtmlCallback {
            parseHtml(it)
        }

//        val urlVideo = "http://v29-dy.ixigua.com/8a48e6c2d81fdc27afcfd92cf854ac0e/5f356fbd/video/tos/cn/tos-cn-ve-15/f8b6de113fcd456eb81e66a9ff4b3624/?a=1128&br=4080&bt=1360&cr=0&cs=0&dr=0&ds=3&er=&l=20200813235206010198066215042E7185&lr=aweme&mime_type=video_mp4&qs=0&rc=MzU8ZjR2aXlzcjMzZmkzM0ApODs6OzozaWVnN2g0aGQ8aGc0Ml9jcS5pZmBfLS1iLS9zc2MvMGBfNWJgL14tMmA1NTI6Yw%3D%3D&vl=&vr="
//        startAutoPlay(this, videoPlayer, urlVideo, "", "DYVIDEO",
//            object : GSYSampleCallBack() {
//                override fun onPrepared(url: String?, vararg objects: Any?) {
//                    super.onPrepared(url, *objects)
//                    GSYVideoManager.instance().isNeedMute = true
//                }
//
//                override fun onClickResume(url: String?, vararg objects: Any?) {
//                    super.onClickResume(url, *objects)
//                }
//
//                override fun onClickBlank(url: String?, vararg objects: Any?) {
//                    super.onClickBlank(url, *objects)
//                    //TODO 视频详情页
//                }
//            })
    }

    fun getVideoCompleteUrl(text: String): String {
        var p =
            Pattern.compile("playAddr: \\\"((http|ftp|https)://)(([a-zA-Z0-9._-]+\\\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\\\.[0-9]{1,3}\\\\.[0-9]{1,3}\\\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9&%_./-~-]*)?\", Pattern.CASE_INSENSITIVE")
        var matcher: Matcher = p.matcher(text)
        val find = matcher.find()
        if (find) {
            return matcher.group().replace("playAddr: \"", "")
        }
        return ""
    }

    fun parseHtml(html: String) {
        var document = Jsoup.parse(html)
        if (document == null) {
            Toast.makeText(this, "网页内容获取失败", Toast.LENGTH_LONG).show()
            return
        }

        Log.e("xujj","doc:"+ document.body())
//        var theVideo = document.getElementById("theVideo")
        var theVideo = document.getElementsByTag("video")
        Log.e("xujj","theVideo:"+ theVideo)
        if (theVideo == null) {
            Toast.makeText(this, "视频标签获取失败", Toast.LENGTH_LONG).show()
            return
        }

        var videoUrl = theVideo.attr("src")
        if (TextUtils.isEmpty(videoUrl)) {
            Toast.makeText(this, "视频标签获取失败", Toast.LENGTH_LONG).show()
            return
        }
        videoUrl = videoUrl.replace("playwm", "play")
        //获取重定向的URL
        var finalVideoUrl = getRealUrl(videoUrl)
        if (TextUtils.isEmpty(finalVideoUrl)) {
            Toast.makeText(this, "获取重定向地址失败", Toast.LENGTH_LONG).show()
            return
        }
        runOnUiThread {
            button.text = "开始解析"
            button.isEnabled = true
            tvResult.text = finalVideoUrl

            val urlVideo = "http://v29-dy.ixigua.com/8a48e6c2d81fdc27afcfd92cf854ac0e/5f356fbd/video/tos/cn/tos-cn-ve-15/f8b6de113fcd456eb81e66a9ff4b3624/?a=1128&br=4080&bt=1360&cr=0&cs=0&dr=0&ds=3&er=&l=20200813235206010198066215042E7185&lr=aweme&mime_type=video_mp4&qs=0&rc=MzU8ZjR2aXlzcjMzZmkzM0ApODs6OzozaWVnN2g0aGQ8aGc0Ml9jcS5pZmBfLS1iLS9zc2MvMGBfNWJgL14tMmA1NTI6Yw%3D%3D&vl=&vr="
            startAutoPlay(this, videoPlayer, urlVideo, "", "DYVIDEO",
                object : GSYSampleCallBack() {
                    override fun onPrepared(url: String?, vararg objects: Any?) {
                        super.onPrepared(url, *objects)
                        GSYVideoManager.instance().isNeedMute = true
                    }

                    override fun onClickResume(url: String?, vararg objects: Any?) {
                        super.onClickResume(url, *objects)
                    }

                    override fun onClickBlank(url: String?, vararg objects: Any?) {
                        super.onClickBlank(url, *objects)
                        //TODO 视频详情页
                    }
                })
            Log.e("xujj", "finalVideoUrl+" + finalVideoUrl)
        }
    }

    private fun getRealUrl(urlStr: String): String {
        var realUrl = urlStr;
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "user-agent", "Mozilla/5.0.html (iPhone; U; CPU iPhone OS 4_3_3 like Mac " +
                        "OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) " +
                        "Version/5.0.html.2 Mobile/8J2 Safari/6533.18.5 "
            )
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            var redirectUrl = ""
            if (302 == code) {
                redirectUrl = conn.getHeaderField("Location");
            }
            if (redirectUrl != null && !redirectUrl.equals("")) {
                realUrl = redirectUrl;
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        return realUrl
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            return
        }
        val shareText: String = getShareText()
        if (!TextUtils.isEmpty(shareText) && shareText.contains(" https://v.douyin.com/")) {
            editText.setText(shareText)
        }
    }

    fun getShareText(): String {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var data: ClipData? = null
        if (cm != null) {
            data = cm.primaryClip
        }
        var item: ClipData.Item? = null
        if (data != null) {
            item = data.getItemAt(0)
        }
        var content: String = ""
        if (item != null) {
            content = item.text.toString()
        }
        return content
    }

    open fun getCompleteUrl(text: String): String {
        val p = Pattern.compile(
            "((http|ftp|https)://)(([a-zA-Z0-9._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9&%_./-~-]*)?",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = p.matcher(text)
        if (matcher.find()) {
            return matcher.group()
        }
        return ""
    }

    fun button(view: View) {
        val url: String = getCompleteUrl(editText.text.toString())
        if (TextUtils.isDigitsOnly(url)) {
            ToastUtils.showLong("未找到抖音分享链接")
        }
        button.text = "解析中..."
        button.isEnabled = false;
        webView.loadUrl(url);
    }

    fun onResultClick(view: View) {
        val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        var text: String
        text = tvResult.text.toString()

        val myClip: ClipData = ClipData.newPlainText("text", text)
        myClipboard.setPrimaryClip(myClip)
        ToastUtils.showLong("已复制")
    }


    fun startAutoPlay(
        activity: Activity,
        player: GSYVideoPlayer,
        playUrl: String,
        coverUrl: String,
        playTag: String,
        callBack: GSYSampleCallBack? = null
    ) {
        player.run {
            //防止错位设置
            setPlayTag(playTag)
            //设置播放位置防止错位
//            setPlayPosition(position)
            //音频焦点冲突时是否释放
            setReleaseWhenLossAudio(false)
            //设置循环播放
            setLooping(true)
            //增加封面
//            val cover = ImageView(activity)
//            cover.scaleType = ImageView.ScaleType.CENTER_CROP
//            cover.load(coverUrl, 4f)
//            cover.parent?.run { removeView(cover) }
//            setThumbImageView(cover)
            //设置播放过程中的回调
            setVideoAllCallBack(callBack)
            //设置播放URL
            setUp(playUrl, false, null)
        }
    }
}
