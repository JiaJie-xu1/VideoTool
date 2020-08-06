package com.partner.videotools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        webView.setHtmlCallback(this::parseHtml)
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

        var theVideo = document.getElementById("theVideo")
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


    private lateinit var qSubscribe: Disposable
    override fun onResume() {
        super.onResume()
        if (qSubscribe != null && !qSubscribe.isDisposed()) {
            qSubscribe.dispose()
        }
        // 延迟获取，Android Q 以上问题
        // 延迟获取，Android Q 以上问题
        qSubscribe = Flowable.timer(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { aLong: Long? ->
                val shareText: String = getShareText()
                if (!TextUtils.isEmpty(shareText) && shareText.contains(" https://v.douyin.com/")) {
                    editText.setText(shareText)
                }
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

    open fun getCompleteUrl(text: String) :String{
        val p = Pattern.compile("((http|ftp|https)://)(([a-zA-Z0-9._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9&%_./-~-]*)?", Pattern.CASE_INSENSITIVE)
        val matcher = p.matcher(text)
        return ""
    }

}
