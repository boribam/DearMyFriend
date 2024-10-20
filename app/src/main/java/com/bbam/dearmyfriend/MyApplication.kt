package com.bbam.dearmyfriend

import android.app.Application
import com.naver.maps.map.NaverMapSdk

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient("xnxdkgomdx")
    }
}