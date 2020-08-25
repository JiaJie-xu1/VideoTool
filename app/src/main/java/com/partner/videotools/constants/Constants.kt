package com.partner.videotools.constants

import android.os.Environment

// 本地文件目录结构
public val ALBUM_PATH = Environment.getExternalStorageDirectory().toString()
public val LOCAL_PATH =  "$ALBUM_PATH/VIDEO_TOOLS/"