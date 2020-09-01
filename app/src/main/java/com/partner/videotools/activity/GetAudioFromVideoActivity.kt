package com.partner.videotools.activity

import android.Manifest
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.partner.videotools.R
import com.partner.videotools.constants.LOCAL_PATH
import com.permissionx.guolindev.PermissionX
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_get_audio_from_video.*
import kotlinx.android.synthetic.main.activity_video_play.*
import kotlinx.android.synthetic.main.activity_video_play.btnDownload
import kotlinx.android.synthetic.main.activity_video_play.videoPlayer
import java.util.*

class GetAudioFromVideoActivity : AppCompatActivity() {
    private var videoPath: String = ""
    private var videoTitle: String = ""

    private var cSubscribe: Disposable? = null
    lateinit var orientationUtils: OrientationUtils
    var outPutPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_audio_from_video)
        setStatusBarTransParent()
        videoPath = intent.getStringExtra("video_Path")
        videoTitle = intent.getStringExtra("video_title")

        init();
    }

    private fun init() {

        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        videoPlayer.setUp(videoPath, true, if (videoTitle == null) "" else videoTitle)

        videoPlayer.titleTextView.visibility = View.VISIBLE
        videoPlayer.backButton.visibility = View.VISIBLE

        orientationUtils = OrientationUtils(this, videoPlayer)
        videoPlayer.backButton.setOnClickListener {
            onBackPressed()
        }
        videoPlayer.startPlayLogic()

        btnDownload.setOnClickListener {
            PermissionX.init(this).permissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
                .onExplainRequestReason { scope, deniedList ->
                    val message = "没有存储权限，无法保存音频"
                    scope.showRequestReasonDialog(
                        deniedList,
                        message,
                        "确定",
                        "取消"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    val message = "没有存储权限，无法保存音频"
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        message,
                        "设置",
                        "取消"
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    getAudio(videoPath)
                }
        }
    }

    /**
     * 音频格式.aac或者.mp3
     */
    fun getAudio(videoPath: String) {
        outPutPath = "$LOCAL_PATH${TimeUtils.date2Millis(Date())}.mp3"
        extractAudio(videoPath, outPutPath)
    }

    /**
     * 使用ffmpeg命令行进行抽取音频
     * @param srcFile 原文件
     * @param targetFile 目标文件
     * @return 抽取后的音频文件
     */
    fun extractAudio(
        srcFile: String?,
        targetFile: String?
    ) {
        //-vn:video not
        var mixAudioCmd = "ffmpeg -i %s -vn %s"
        mixAudioCmd = String.format(mixAudioCmd, srcFile, targetFile)
        var commandsArray = mixAudioCmd.split(" ".toRegex()) //以空格分割为字符串数组

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
                    ToastUtils.showLong("提取成功,路径为:$outPutPath")
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


    override fun onBackPressed() {

        // 先返回正常状态
        if (orientationUtils.screenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            videoPlayer.fullscreenButton.performClick()
            return
        }
        // 释放所有
        videoPlayer.setVideoAllCallBack(null)
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
        if (orientationUtils != null) {
            orientationUtils.releaseListener()
        }
    }

    private fun setStatusBarTransParent() {
        //沉浸式导航栏
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView: View = getWindow().getDecorView()
            val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            decorView.systemUiVisibility = option
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
    }


}