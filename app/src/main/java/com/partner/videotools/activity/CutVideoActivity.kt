package com.partner.videotools.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.partner.videotools.R
import com.partner.videotools.constants.LOCAL_PATH
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.android.synthetic.main.activity_cut_video.*
import kotlinx.android.synthetic.main.activity_cut_video.progress100
import kotlinx.android.synthetic.main.activity_get_audio_from_video.*
import kotlinx.android.synthetic.main.activity_get_audio_from_video.videoPlayer
import kotlinx.android.synthetic.main.activity_video_play.*
import java.util.*

/**
 * 截取视频
 */
class CutVideoActivity : AppCompatActivity() {
    private var videoPath = ""
    private var startTime = ""
    private var duration = ""
    var outPutPath: String = ""
    lateinit var orientationUtils: OrientationUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cut_video)

        videoPath = intent.getStringExtra("video_Path")

        btnStart.setOnClickListener {
            startTime = etStart.text.toString()
            duration = etDuration.text.toString()

            etStart.clearFocus()
            etDuration.clearFocus()
            if (!TextUtils.isEmpty(startTime) && !TextUtils.isEmpty(duration)) {
                outPutPath = "${TimeUtils.date2Millis(Date())}.mp4"
                extractVideo(
                    startTime,
                    duration,
                    videoPath,
                    outPutPath
                )
            } else {
                ToastUtils.showLong("请输入视频开始时间和时长")
            }
        }

        initPalyer()
    }

    private fun initPalyer() {
        Log.e("xujj", "path:$videoPath")
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        cutVideoPlayer.setUp(videoPath, true, null)

        cutVideoPlayer.titleTextView.visibility = View.GONE
        cutVideoPlayer.backButton.visibility = View.GONE

        orientationUtils = OrientationUtils(this, cutVideoPlayer)

        cutVideoPlayer.startPlayLogic()
    }

    /**
     * 截取视频片段时间：startTime 开始时间
     * duration:视频时间
     * path：视频原始地址
     * outPath:视频输出时间
     */
    private fun extractVideo(startTime: String, duration: String, path: String, outPath: String) {
        var mixVideoCmd = "ffmpeg -i ".plus(path).plus(" -ss ").plus(startTime).plus(" -t ")
            .plus("$duration ").plus(LOCAL_PATH).plus(outPath)

        var commandsArray = mixVideoCmd.split(" ".toRegex()) //以空格分割为字符串数组

        Log.e("xujj", "mixVideoCmd:$mixVideoCmd")

        Log.e("xujj", "commandsArray:$commandsArray")

        runFFmpegRxJava(commandsArray)
    }

    private fun runFFmpegRxJava(commandsArray: List<String>) {
        progress100.visibility = View.VISIBLE
        Log.e("xujj", "commandsArray:${commandsArray.toTypedArray()}")
        RxFFmpegInvoke.getInstance().runCommandRxJava(commandsArray.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onFinish() {
                    Log.e("xujj", "onFinish()")
                    progress100.visibility = View.GONE
                    ToastUtils.showLong("提取成功,路径为:$LOCAL_PATH$outPutPath")
                }

                override fun onCancel() {
                    Log.e("xujj", "onCancel()")
                    progress100.visibility = View.GONE
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    Log.e("xujj", "progress():$progress")
                    progress100.progress = progress
//                    ToastUtils.showShort("提取中：$progress")
                    //Logger.d("onCancel")
                }

                override fun onError(message: String?) {
                    progress100.visibility = View.GONE
                    ToastUtils.showShort("提取失败：$message")
                }
            })
    }

    override fun onPause() {
        super.onPause()
        cutVideoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        cutVideoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
    }
}