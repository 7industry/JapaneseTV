package com.kindustry.iptv.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class BackgroundPlayService : Service() {

    private val CHANNEL_ID = "BackgroundPlaybackChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
//        player = ExoPlayer.Builder(this).build()
        val title = "后台视频播放"
        val content = "正在后台播放视频"

        // 在 Service 创建时就创建并显示通知，并将其置于前台
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // 替换为你的应用图标
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 设置较低的优先级
            .setOngoing(true) // 设置为持久通知
            .setColor(ContextCompat.getColor(this, android.R.color.darker_gray)) // 可选：设置通知颜色
            .build()

        //  create Notification Channel  Build.VERSION_CODES.O 是 Android 8.0 Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "后台播放服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)

            startForeground(NOTIFICATION_ID, notification)
        }  else{
            // 在 Android 7 及更低版本中，不需要创建 NotificationChannel
            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val videoUrl = intent?.getStringExtra("videoUrl")
//        if (!videoUrl.isNullOrEmpty() && player?.currentMediaItem == null) {
//            // ... (播放器初始化代码)
//        } else if (player?.playWhenReady == false) {
//            player?.playWhenReady = true
//        }
        return START_STICKY // 系统尝试重新启动 Service
    }

    override fun onDestroy() {
        // 停止前台服务并移除通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}