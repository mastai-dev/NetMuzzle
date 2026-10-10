package com.netmuzzle.firewall.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.data.FirewallPreferences
import com.netmuzzle.firewall.service.dns.FloatingWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Usługa zarządzająca pływającym przyciskiem (overlay) wyświetlanym na wierzchu gier.
 * Pozwala graczowi na 1-kliknięcie odblokować reklamy wideo z nagrodą (np. monety/diamenty),
 * po czym automatycznie wznawia blokowanie po upływie zadanego czasu.
 */
class FloatingWidgetService : Service() {

    companion object {
        private const val TAG = "FloatingWidgetService"
        private const val PREFS_NAME = "netmuzzle_floating_widget"
        private const val KEY_POS_X = "widget_x"
        private const val KEY_POS_Y = "widget_y"

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Brak uprawnienia SYSTEM_ALERT_WINDOW")
                return
            }
            val intent = Intent(context, FloatingWidgetService::class.java)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Błąd startu FloatingWidgetService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var widgetView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var iconView: ImageView? = null
    private var textView: TextView? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    private var timerDurationSeconds = 60

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWidget()
        observeWidgetState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWidget() {
        if (widgetView != null) return

        val dpToPx = { dp: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
        }

        // Kontener główny (pigułka)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            elevation = dpToPx(6f).toFloat()
        }

        // Ikona tarczy
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(20f), dpToPx(20f)).apply {
                marginEnd = dpToPx(4f)
            }
            setImageResource(R.drawable.ic_shield_adblock)
        }
        iconView = icon
        container.addView(icon)

        // Etykieta tekstowa (status lub odliczanie)
        val text = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            text = "ADS"
        }
        textView = text
        container.addView(text)

        // Ładowanie zapisanych współrzędnych
        val sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedX = sp.getInt(KEY_POS_X, dpToPx(16f))
        val savedY = sp.getInt(KEY_POS_Y, dpToPx(160f))

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        layoutParams = params

        // Obsługa przeciągania oraz kliknięć
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = layoutParams ?: return false
                val wm = windowManager ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x
                        initialY = p.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > dpToPx(6f) || abs(dy) > dpToPx(6f)) {
                            isDragging = true
                            p.x = initialX + dx.toInt()
                            p.y = initialY + dy.toInt()
                            wm.updateViewLayout(v, p)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // Zapisz pozycję po zakończeniu przeciągania
                            sp.edit().putInt(KEY_POS_X, p.x).putInt(KEY_POS_Y, p.y).apply()
                        } else {
                            // Kliknięcie - wykonaj wibrację i przełącz stan blokady
                            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onWidgetClicked()
                        }
                        return true
                    }
                }
                return false
            }
        })

        widgetView = container
        updateWidgetAppearance(isPaused = false, remainingSeconds = 0)

        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd dodawania widoku do WindowManager", e)
        }
    }

    private fun onWidgetClicked() {
        serviceScope.launch {
            val prefs = FirewallPreferences(applicationContext)
            val isTimerEnabled = prefs.isFloatingTimerEnabled.first()
            val duration = if (isTimerEnabled) prefs.floatingTimerSeconds.first() else 0
            timerDurationSeconds = duration

            FloatingWidgetManager.toggle(
                durationSeconds = if (isTimerEnabled) duration else 0,
                targetPackage = null
            )
        }
    }

    private fun observeWidgetState() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            launch {
                FloatingWidgetManager.isPaused.collect { isPaused ->
                    val remaining = FloatingWidgetManager.remainingSeconds.value
                    updateWidgetAppearance(isPaused, remaining)
                }
            }
            launch {
                FloatingWidgetManager.remainingSeconds.collect { remaining ->
                    val isPaused = FloatingWidgetManager.isPaused.value
                    updateWidgetAppearance(isPaused, remaining)
                }
            }
        }
    }

    private fun updateWidgetAppearance(isPaused: Boolean, remainingSeconds: Int) {
        val dpToPx = { dp: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
        }

        val backgroundDrawable = GradientDrawable().apply {
            cornerRadius = dpToPx(24f).toFloat()
            if (isPaused) {
                setColor(Color.parseColor("#E62A1B08")) // Ciepły ciemny bursztyn
                setStroke(dpToPx(1.5f), Color.parseColor("#F59E0B")) // Złoty/bursztynowy obrys
            } else {
                setColor(Color.parseColor("#E60B1220")) // Ciemny cyjanowy grafit
                setStroke(dpToPx(1.5f), Color.parseColor("#00E5FF")) // Neonowy cyjan
            }
        }
        widgetView?.background = backgroundDrawable

        if (isPaused) {
            iconView?.setColorFilter(Color.parseColor("#F59E0B"))
            textView?.apply {
                setTextColor(Color.parseColor("#FCD34D"))
                text = if (remainingSeconds > 0) "${remainingSeconds}s" else getString(R.string.floating_widget_badge_paused)
            }
        } else {
            iconView?.setColorFilter(Color.parseColor("#00E5FF"))
            textView?.apply {
                setTextColor(Color.parseColor("#00E5FF"))
                text = getString(R.string.floating_widget_badge_active)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Zabezpieczenie przed ucieknięciem przycisku poza ekran po obrocie ekranu
        val p = layoutParams ?: return
        val wm = windowManager ?: return
        val v = widgetView ?: return

        val displayMetrics = resources.displayMetrics
        val maxX = displayMetrics.widthPixels - v.width
        val maxY = displayMetrics.heightPixels - v.height

        var changed = false
        if (p.x > maxX && maxX > 0) {
            p.x = maxX - 20
            changed = true
        }
        if (p.y > maxY && maxY > 0) {
            p.y = maxY - 20
            changed = true
        }
        if (changed) {
            wm.updateViewLayout(v, p)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observerJob?.cancel()
        widgetView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Błąd usuwania widoku widgetu", e)
            }
        }
        widgetView = null
    }
}
