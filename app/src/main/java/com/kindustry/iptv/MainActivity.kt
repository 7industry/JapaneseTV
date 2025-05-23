package com.kindustry.iptv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Util
import com.kindustry.iptv.utils.ContextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity(), AnalyticsListener {

    private lateinit var playerView: PlayerView
    private lateinit var rootLayout: CoordinatorLayout
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private lateinit var wifiLock: WifiManager.WifiLock

    private var iptvNames: List<String> = emptyList()
    private var videoUrls: List<String> = emptyList()
    private var currentVideoIndex = 0


    // 初始化播放器
    private val player by lazy {
        SimpleExoPlayer.Builder(this).setLoadControl(asapLoadControl).build()
    }

    private val asapLoadControl by lazy {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000,
                60000,
                1000,
                1000,
            ).build()
    }

    private val dataSourceFactory by lazy {
        val userAgent = Util.getUserAgent(this, "Mozilla/4.0 (compatible; MSIE 6.0; ztebw V1.0)")
        val unsafeOkHttpClient = getUnsafeOkHttpClient(this)

        DataSource.Factory {
            val defaultHttpDataSource = OkHttpDataSource.Factory(unsafeOkHttpClient).setUserAgent(userAgent).createDataSource()
            defaultHttpDataSource
        }
    }

    fun getUnsafeOkHttpClient(context: Context): OkHttpClient {
        return try {
            // 创建一个信任所有证书的 TrustManager
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> { return arrayOf() }
            })

            // 获取 SSLContext 实例
            val sslContext = SSLContext.getInstance("TLS")  // 使用 "TLS" 或者 "TLSv1.2"
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true } // 信任所有主机名
            builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val m3uUrl = ContextHelper.decryptBinaryToString(this)

        // 确保你的根视图在 XML 中有 id="rootLayout"
        rootLayout = findViewById(R.id.rootLayout)
        playerView = findViewById(R.id.playerView)

        // 阻止屏幕变暗和休眠
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        // 创建 TextureView
        val textureView = TextureView(this)

        // 将 TextureView 设置为 ExoPlayer 的渲染视图
        player.setVideoTextureView(textureView)

        playerView.keepScreenOn = true
        playerView.player = player
        // 添加播放器监听器
        player.addAnalyticsListener(this)

        //inject initial data
        lifecycleScope.launchWhenCreated {
//            if (BuildConfig.DEBUG)
            withContext(Dispatchers.IO) {
                // 在 IO 线程中执行你的网络操作
                videoUrls = parseM3U(m3uUrl)
                if (videoUrls.isNotEmpty()) {
                    // 切换到主线程来操作 ExoPlayer
                    withContext(Dispatchers.Main) {
                        switchPlay()
                    }
                } else {
                    // 处理 M3U 文件解析失败或没有视频 URL 的情况
                    Toast.makeText(this@MainActivity, "无法加载视频列表", Toast.LENGTH_SHORT).show()
                }
            }
            // 在主线程中处理网络操作的结果，例如更新 UI
//            updateUI(result)
        }

        // 创建 GestureDetectorCompat 实例
        gestureDetectorCompat = GestureDetectorCompat(this, MyGestureListener())

        // 为根视图设置 OnTouchListener，将触摸事件传递给 GestureDetectorCompat
        rootLayout.setOnTouchListener { _, event ->
            gestureDetectorCompat.onTouchEvent(event)
            true // 返回 true 表示你已经处理了触摸事件
        }
    }

    /*
    override fun onStart() {
        super.onStart()
        player.prepare()
        lifecycleScope.launch {
        }
    }*/


    // 当 Activity 的窗口获得或失去焦点时调用
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }


    // 隐藏状态栏和导航栏
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用 WindowInsetsController (API 30+)
            window.insetsController?.let {
                // 隐藏状态栏和导航栏
                it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())

                // 设置行为，使系统栏在用户交互时短暂显示
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // 兼容旧版本 (API < 30) - 你提供的代码仍然适用
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }



    // 在进入前台时恢复播放
    override fun onResume() {
        super.onResume()
        // 标志告诉播放器在切换到Player.STATE_READY时自动播放媒体
        player.playWhenReady = true
        playerView.onResume()
    }

/*    override fun onPause() {
        super.onPause()
        player.playWhenReady = false
        playerView.onPause()
    }

    override fun onRestart() {
        super.onRestart()
        player.playWhenReady = true
        player.playbackState
    }*/

    //  当你的应用进入后台时，你需要保存当前的播放状态，
    //  包括 playWhenReady 的值和当前的播放位置
    //  你可以使用 SharedPreferences、ViewModel 或其他持久化机制来保存这些数据。
    override fun onStop() {
        super.onStop()
        player.playWhenReady = true
        player.playbackState
        player.currentPosition
        player.currentWindowIndex
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        // 在服务销毁或播放停止时释放锁
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }


    fun parseM3U(m3uUrl: String?): List<String> {
        val playlist = mutableListOf<String>()
        val tvnames = mutableListOf<String>()
        try {
            val url = URL(m3uUrl)
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            var lastName = ""
            while (line != null) {
                line = line.trim()

                if (line.startsWith("#EXTINF:-1")) {
                    lastName = line.split(",").last().trim() // 获取列表中的最后一个元素并去除首尾空格
                }
                if (line.isNotBlank() && !line.startsWith("#")) {
                    playlist.add(line)
                    tvnames.add(lastName)
                }
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理网络或文件读取错误
        }

        iptvNames = tvnames
        return playlist
    }


    // 自定义手势监听器
    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY) &&
                Math.abs(diffX) > SWIPE_THRESHOLD &&
                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                if (diffX > 0) {
                    // 向右滑动
                    currentVideoIndex--
                } else {
                    // 向左滑动
                    currentVideoIndex++
                }
                switchPlay()
                return true // 表示我们处理了 fling 手势
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    // 在这里执行滑动时的操作，例如切换到上一个屏幕或执行特定功能
    private fun switchPlay() {
        // 循环回列表的开头或结尾
        currentVideoIndex = (currentVideoIndex % videoUrls.size + videoUrls.size) % videoUrls.size
        Toast.makeText(this, iptvNames[currentVideoIndex], Toast.LENGTH_SHORT).show()
        // 播放列表中的第一个 URL
        val uri = Uri.parse(videoUrls[currentVideoIndex])
        val type = Util.inferContentType(uri)
        val ms = when (type) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
            else -> {
//                toast(str(R.string.fmt_unsupported_content_type, type.toString()))
                return
            }
        }.createMediaSource(MediaItem.fromUri(uri))

        player.setMediaSource(ms)
        player.prepare() // 通常 prepare 也需要在主线程调用
        player.playWhenReady = true // 如果需要自动播放
//        playerView.onResume() // Ensure PlayerView is resumed
    }



}