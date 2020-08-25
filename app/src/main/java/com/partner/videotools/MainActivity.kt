package com.partner.videotools

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.partner.videotools.activity.DyVideoActivity
import com.partner.videotools.constants.ALBUM_PATH
import com.partner.videotools.constants.LOCAL_PATH
import com.permissionx.guolindev.PermissionX
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_VIDEO_GET_AUDIO_CODE = 10001
    private val REQUEST_CUT_VIDEO_AUDIO_CODE = 10002

    private var audioName: String = ""
    private var fileName: String = ""
    private var commands: String = ""//要执行的命令
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionX.init(this).permissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
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
                initLocalFiles()
            }

        btnDyin.setOnClickListener {
            val intent = Intent(this, DyVideoActivity::class.java)
            ActivityUtils.startActivity(intent)
        }
        tvMusic.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_VIDEO_GET_AUDIO_CODE)
        }

        tvCut.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CUT_VIDEO_AUDIO_CODE)
        }
    }

    fun initLocalFiles() {
        FileUtils.createOrExistsDir(LOCAL_PATH)
    }

    /**
     * 提取视频中音频的片段
     */

    fun extractAudio(startTime: String, time: String, path: String, name: String, deType: String) {
        var type = ".mp3"
        if (deType != "") {
            type = deType
        }
        audioName = name
        fileName = "${TimeUtils.date2Millis(Date())}audio$type"

        if (startTime == "") {
            commands = "ffmpeg -i ".plus(path).plus(" -vn ")
                .plus(LOCAL_PATH)
                .plus(fileName)
        } else {
            commands =
                "ffmpeg -i ".plus(path).plus(" -vn ").plus("-ss ").plus(startTime).plus(" -t ")
                    .plus(time + " ").plus(LOCAL_PATH).plus(fileName)
        }
        Log.e("xujj", "commands:$commands")
        var commandsArray = commands.split(" ")

        Log.e("xujj", "commandsArray:$commandsArray")

        runFFmpegRxJava(commandsArray)
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
        var mixAudioCmd = "ffmpeg -i %s -acodec copy -vn %s"
        mixAudioCmd = String.format(mixAudioCmd, srcFile, targetFile)
        var commandsArray = mixAudioCmd.split(" ".toRegex()) //以空格分割为字符串数组

        Log.e("xujj", "commandsArray:$commandsArray")

        runFFmpegRxJava(commandsArray)
    }

    private fun runFFmpegRxJava(commandsArray: List<String>) {
        Log.e("xujj", "commandsArray:${commandsArray.toTypedArray()}")
        RxFFmpegInvoke.getInstance().runCommandRxJava(commandsArray.toTypedArray())
            .subscribe(object : RxFFmpegSubscriber() {
                override fun onFinish() {
                    Log.e("xujj", "onFinish()")
                    ToastUtils.showShort("提取成功")
                }

                override fun onCancel() {
                    Log.e("xujj", "onCancel()")
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    Log.e("xujj", "progress():$progress")
                    //Logger.d("onCancel")
                }

                override fun onError(message: String?) {
                    ToastUtils.showShort("提取失败：$message")
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("xujj", "result code:$requestCode")
        if (requestCode == REQUEST_VIDEO_GET_AUDIO_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                val cr = this.contentResolver

                val cursor = cr.query(uri!!, null, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val videoPath =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))

                        Log.e("xujj", "videoPath:$videoPath")

                        extractAudio(videoPath, "$LOCAL_PATH${TimeUtils.date2Millis(Date())}.aac")
                    }
                }
            }
        } else if (requestCode == REQUEST_CUT_VIDEO_AUDIO_CODE) {//截取视频中的音频片段
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                val cr = this.contentResolver

                val cursor = cr.query(uri!!, null, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val videoPath =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))

                        Log.e("xujj", "videoPath:$videoPath")

                        extractAudio("5", "20", videoPath, "测试", ".mp3")
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
