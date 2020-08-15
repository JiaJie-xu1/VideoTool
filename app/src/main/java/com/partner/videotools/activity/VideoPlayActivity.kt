package com.partner.videotools.activity

import android.Manifest
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import com.partner.videotools.R
import com.permissionx.guolindev.PermissionX
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_video_play.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Created by Jay.Xu
 * @since 2020/8/15
 */
class VideoPlayActivity : AppCompatActivity() {
    var videoUrl: String = ""
    var videoTitle: String = ""

    var cSubscribe: Disposable? = null
    lateinit var orientationUtils: OrientationUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)
        setStatusBarTransParent()
        videoUrl = intent.getStringExtra("video_url")
        videoTitle = intent.getStringExtra("video_title")

        init();
    }

    private fun init() {

        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        videoPlayer.setUp(videoUrl, true, if (videoTitle == null) "" else videoTitle)

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
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .onExplainRequestReason { scope, deniedList ->
                    val message = "没有存储权限，无法下载视频"
                    scope.showRequestReasonDialog(
                        deniedList,
                        message,
                        "确定",
                        "取消"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    val message = "没有存储权限，无法下载视频"
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        message,
                        "设置",
                        "取消"
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    download(videoUrl)
                }
        }
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

    fun setStatusBarTransParent() {
        //沉浸式导航栏
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView: View = getWindow().getDecorView()
            val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            decorView.systemUiVisibility = option
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
    }

    fun download(url: String) {
        val internalMoviesPath = PathUtils.getExternalDownloadsPath() +
                File.separator + EncryptUtils.encryptMD5ToString(videoUrl) + ".mp4"

        val uri = createFile(EncryptUtils.encryptMD5ToString(videoUrl) + ".mp4")

        if(File(internalMoviesPath).exists()){
            Toast.makeText(
                this@VideoPlayActivity,
                "该视频已存在",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        FileDownloader.setup(this)

        FileDownloader.getImpl().create(url)
            .setPath(internalMoviesPath)
            .setListener(object : FileDownloadListener() {
                override fun started(task: BaseDownloadTask?) {
                    super.started(task)
                    Toast.makeText(this@VideoPlayActivity, "视频开始下载", Toast.LENGTH_SHORT).show()
                }

                override fun warn(task: BaseDownloadTask?) {}

                override fun completed(task: BaseDownloadTask) {
//                    copyFile(task.path, uri!!)
                    Log.e("xujj","path："+task.path)
                    Toast.makeText(
                        this@VideoPlayActivity,
                        "视频下载完成，请去相册查看",
                        Toast.LENGTH_SHORT
                    ).show()
                    FileUtils.notifySystemToScan(task.path)
                }

                override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {}

                override fun error(task: BaseDownloadTask?, e: Throwable?) {
                    Toast.makeText(this@VideoPlayActivity, "视频下载出错", Toast.LENGTH_SHORT).show()
                }

                override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {}

                override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {}

            }).start()
    }

    /**
     * 新增文件
     */
    private fun createFile(fileName: String): Uri? {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.Video.VideoColumns.MIME_TYPE, "video/mp4")
        return contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    /**
     * 文件复制到外部
     */
    private fun copyFile(path: String, uri: Uri) {
        if (cSubscribe != null && !cSubscribe!!.isDisposed()) {
            cSubscribe!!.dispose()
        }
        cSubscribe =
            Flowable.create(FlowableOnSubscribe { emitter: FlowableEmitter<String?> ->
                // 文件存储适配Android Q
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    FileUtils.copy(
                        path,
                        PathUtils.getExternalMoviesPath()
                    )
                } else {
                    writeFile(path, uri)
                }
                emitter.onNext("成功")
            }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { s: String? ->
                        Toast.makeText(
                            this@VideoPlayActivity,
                            "视频下载完成",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { obj: Throwable -> obj.printStackTrace() }
    }

    /**
     * 对文件进行写操作
     */
    private fun writeFile(path: String, uri: Uri) {
        try {
            val assetFileDescriptor =
                contentResolver.openAssetFileDescriptor(uri, "rw")
            val os = assetFileDescriptor!!.createOutputStream()
            val isf = FileInputStream(path)
            val data = ByteArray(8192)
            var len: Int
            while (isf.read(data, 0, 8192).also { len = it } != -1) {
                os.write(data, 0, len)
            }
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}