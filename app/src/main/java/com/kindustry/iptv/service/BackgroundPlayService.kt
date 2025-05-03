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
        //接受传递过来的intent的数据
//        val videoUrl = intent?.getStringExtra("videoUrl")
//        if (!videoUrl.isNullOrEmpty() && player?.currentMediaItem == null) {
//            // ... (播放器初始化代码)
//        } else if (player?.playWhenReady == false) {
//            player?.playWhenReady = true
//        }
        // ... 处理启动命令 ...
     /*   if (intent?.action == "ACTION_STOP_PLAYBACK") {
            // 停止任何正在进行的后台播放操作
            stopForeground(true) // 如果服务在前台运行，需要先停止前台模式
            stopSelf() // 停止服务自身
        }*/
        return START_STICKY // 系统尝试重新启动 Service
    }


    /**
     * onBind 是 Service 的虚方法，因此我们不得不实现它。
     * 返回 null，表示客服端不能建立到此服务的连接。
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 在这里处理应用被上滑关闭的事件
        // 例如：停止后台播放、清理资源、保存状态等

        // 如果你希望服务在被强制停止后不再尝试重启，可以调用 stopSelf()
        stopSelf()
    }

    override fun onDestroy() {
        // 注意：onDestroy() 不保证在强制停止时一定被调用
        // 可以在这里进行一些清理工作，但不要依赖它来处理关键的“应用关闭”逻辑
        // 停止前台服务并移除通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        super.onDestroy()
    }


}