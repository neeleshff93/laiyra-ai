 package com.laiyra.ai.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.laiyra.ai.MainActivity

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var logoView: View? = null
    private var menuView: View? = null
    private var logoParams: WindowManager.LayoutParams? = null
    private var isMenuOpen = false
    private var colorIndex = 0

    private val colours = intArrayOf(
        0xFF00F5FF.toInt(),
        0xFF6D28FF.toInt(),
        0xFF10B981.toInt(),
        0xFFFF2D9B.toInt(),
        0xFFFFA500.toInt(),
        0xFFFFD700.toInt(),
        0xFFFF3B30.toInt()
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
    super.onCreate()
    android.util.Log.d("Laiyra", "=== FloatingService STARTED ===")
    startForegroundNotification()

    wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val can = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(this)
    } else true

    android.util.Log.d("Laiyra", "Overlay permission: $can")

    if (can) {
        showLogo()
        android.util.Log.d("Laiyra", "Logo shown")
    } else {
        android.util.Log.e("Laiyra", "No overlay permission — closing")
        stopSelf()
    }
}


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlay()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (logoView == null) showLogo()
        return START_STICKY
    }

    private fun canDrawOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun startForegroundNotification() {
        val channelId = "laiyra_overlay"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Laiyra Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notif: Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notif = Notification.Builder(this, channelId)
                .setContentTitle("Laiyra Active")
                .setContentText("Floating logo active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        } else {
            @Suppress("DEPRECATION")
            notif = Notification.Builder(this)
                .setContentTitle("Laiyra Active")
                .setContentText("Floating logo active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }

        startForeground(1001, notif)
    }

    private fun showLogo() {
        val size = dp(58)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER

        val logo = TextView(this)
        logo.text = "LA"
        logo.setTextColor(Color.WHITE)
        logo.textSize = 18f
        logo.gravity = Gravity.CENTER
        logo.isClickable = true
        logo.isFocusable = true
        logo.background = makeLogoBg()

        val logoLp = LinearLayout.LayoutParams(size, size)
        container.addView(logo, logoLp)

        logoView = container

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 300

        logoParams = params

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        logo.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = ev.rawX
                    touchY = ev.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - touchX).toInt()
                    val dy = (ev.rawY - touchY).toInt()
                    if (dx * dx + dy * dy > 100) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    wm.updateViewLayout(logoView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMenu()
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(logoView, params)
            Log.d("Laiyra", "Logo added")
        } catch (ex: Exception) {
            Log.e("Laiyra", "Logo add failed: ${ex.message}")
        }
    }

    private fun makeLogoBg(): GradientDrawable {
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(colours[colorIndex])
        bg.setStroke(dp(2), 0xFF00A9BC.toInt())
        return bg
    }

    private fun toggleMenu() {
        if (isMenuOpen) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        if (isMenuOpen) return
        isMenuOpen = true

        val menu = LinearLayout(this)
        menu.orientation = LinearLayout.VERTICAL
        menu.setPadding(dp(6), dp(6), dp(6), dp(6))

        val menuBg = GradientDrawable()
        menuBg.shape = GradientDrawable.RECTANGLE
        menuBg.cornerRadius = dp(18).toFloat()
        menuBg.setColor(0xE600131D.toInt())
        menuBg.setStroke(dp(1), 0xFF00F5FF.toInt())
        menu.background = menuBg

        addMenuRow(menu, "Laiyra", 0)
        addMenuRow(menu, "Colour", 1)
        addMenuRow(menu, "Hide 30s", 2)
        addMenuRow(menu, "Remove", 3)
        addMenuRow(menu, "Close", 4)

        menuView = menu

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val p = logoParams
        params.x = (p?.x ?: 40) - dp(60)
        params.y = (p?.y ?: 300) - dp(260)

        try {
            wm.addView(menuView, params)
        } catch (ex: Exception) {
            Log.e("Laiyra", "Menu add failed: ${ex.message}")
        }
    }

    private fun addMenuRow(menu: LinearLayout, label: String, actionId: Int) {
        val row = TextView(this)
        row.text = label
        row.setTextColor(0xFFDDEBFF.toInt())
        row.textSize = 14f
        row.setPadding(dp(20), dp(12), dp(20), dp(12))
        row.isClickable = true
        row.isFocusable = true

        row.setOnClickListener {
            when (actionId) {
                0 -> doOpenLaiyra()
                1 -> doColour()
                2 -> doHide30s()
                3 -> doRemove()
                4 -> doClose()
            }
        }

        val lp = LinearLayout.LayoutParams(
            dp(160),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        menu.addView(row, lp)
    }

    private fun doOpenLaiyra() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        hideMenu()
    }

    private fun doColour() {
        colorIndex = (colorIndex + 1) % colours.size
        updateLogoColor()
    }

    private fun doHide30s() {
        hideLogo()
        val handler = Handler(mainLooper)
        handler.postDelayed({
            if (logoView == null) showLogo()
        }, 30000L)
        hideMenu()
    }

    private fun doRemove() {
        hideLogo()
        hideMenu()
    }

    private fun doClose() {
        stopSelf()
    }

    private fun hideMenu() {
        isMenuOpen = false
        val mv = menuView
        if (mv != null) {
            try {
                wm.removeView(mv)
            } catch (ex: Exception) {
                // ignore
            }
        }
        menuView = null
    }

    private fun hideLogo() {
        hideMenu()
        val lv = logoView
        if (lv != null) {
            try {
                wm.removeView(lv)
            } catch (ex: Exception) {
                // ignore
            }
        }
        logoView = null
    }

    private fun updateLogoColor() {
        val container = logoView as? LinearLayout
        val logo = container?.getChildAt(0) as? TextView
        if (logo != null) {
            logo.background = makeLogoBg()
        }
    }

    override fun onDestroy() {
        hideMenu()
        hideLogo()
        super.onDestroy()
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }
}