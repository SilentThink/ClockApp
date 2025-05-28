package com.example.clockapp

import android.app.Application
import android.util.Log
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility

/**
 * 应用程序类，用于初始化科大讯飞SDK
 */
class ClockApplication : Application() {
    
    companion object {
        private const val TAG = "ClockApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化科大讯飞SDK
        initXunfeiSdk()
    }
    
    /**
     * 初始化科大讯飞SDK
     */
    private fun initXunfeiSdk() {
        try {
            // 使用与xunfeiTest项目相同的APPID
            val appId = "f5b3c3fc"
            
            // 初始化科大讯飞SDK
            SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + appId)
            
            Log.i(TAG, "科大讯飞SDK初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "科大讯飞SDK初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }
} 