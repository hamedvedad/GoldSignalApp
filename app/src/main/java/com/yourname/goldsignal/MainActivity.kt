package com.yourname.goldsignal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSettings: Button
    private lateinit var tvPrice: TextView
    private lateinit var tvSignal: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvWorkingHours: TextView
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private var monitoringJob: Job? = null
    private val goldAnalyzer = GoldAnalyzer()
    private val workingHoursManager = WorkingHoursManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupNotificationChannel()
        setupSharedPreferences()
        loadSettings()
        setupClickListeners()
        
        addLog("برنامه راه‌اندازی شد")
        updateWorkingHoursDisplay()
    }
    
    private fun initializeViews() {
        btnStart = findViewById(R.id.btnStartMonitoring)
        btnStop = findViewById(R.id.btnStopMonitoring)
        btnSettings = findViewById(R.id.btnSettings)
        tvPrice = findViewById(R.id.tvPrice)
        tvSignal = findViewById(R.id.tvSignal)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        tvLog = findViewById(R.id.tvLog)
        tvWorkingHours = findViewById(R.id.tvWorkingHours)
    }
    
    private fun setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("GoldSignalPrefs", Context.MODE_PRIVATE)
    }
    
    private fun setupNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "GOLD_SIGNAL_CHANNEL",
                "سیگنال‌های طلا",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اطلاع‌رسانی سیگنال‌های خرید و فروش طلا"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun setupClickListeners() {
        btnStart.setOnClickListener {
            startMonitoring()
        }
        
        btnStop.setOnClickListener {
            stopMonitoring()
        }
        
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun startMonitoring() {
        if (!workingHoursManager.isWithinWorkingHours()) {
            addLog("❌ خارج از ساعت کاری! مانیتورینگ شروع نشد")
            return
        }
        
        addLog("✅ مانیتورینگ شروع شد...")
        
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    if (workingHoursManager.isWithinWorkingHours()) {
                        val signal = goldAnalyzer.checkGoldSignal()
                        
                        withContext(Dispatchers.Main) {
                            updateUI(signal)
                        }
                        
                        if (signal.hasSignal) {
                            sendNotification(signal)
                            addLog("💰 سیگنال ${getFarsiSignalType(signal.signalType)} شناسایی شد!")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            addLog("⏸ خارج از ساعت کاری - منتظر ساعت کاری...")
                        }
                    }
                    
                    delay(30000) // هر 30 ثانیه چک کن
                    
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        addLog("خطا: ${e.message}")
                    }
                    delay(10000)
                }
            }
        }
    }
    
    private fun stopMonitoring() {
        monitoringJob?.cancel()
        addLog("⏹ مانیتورینگ متوقف شد")
        
        tvSignal.text = "سیگنال: متوقف شده"
        tvSignal.setTextColor(Color.GRAY)
    }
    
    private fun updateUI(signal: GoldSignal) {
        tvPrice.text = "قیمت: ${signal.currentPrice}"
        tvLastUpdate.text = "آخرین بروزرسانی: ${signal.timestamp}"
        
        when {
            signal.signalType == "BUY" -> {
                tvSignal.text = "سیگنال: خرید 🟢"
                tvSignal.setTextColor(Color.GREEN)
            }
            signal.signalType == "SELL" -> {
                tvSignal.text = "سیگنال: فروش 🔴" 
                tvSignal.setTextColor(Color.RED)
            }
            else -> {
                tvSignal.text = "سیگنال: عدم سیگنال ⚪"
                tvSignal.setTextColor(Color.GRAY)
            }
        }
    }
    
    private fun sendNotification(signal: GoldSignal) {
        val notificationId = System.currentTimeMillis().toInt()
        
        val notification = NotificationCompat.Builder(this, "GOLD_SIGNAL_CHANNEL")
            .setSmallIcon(R.drawable.ic_gold)
            .setContentTitle("سیگنال طلا - ${getFarsiSignalType(signal.signalType)}")
            .setContentText("قیمت: ${signal.currentPrice}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("""
                    💰 سیگنال ${getFarsiSignalType(signal.signalType)}
                    💵 قیمت: ${signal.currentPrice}
                    🎯 سود هدف: ${signal.takeProfit}
                    🛑 حد ضرر: ${signal.stopLoss}
                    ⏰ زمان: ${signal.timestamp}
                """.trimIndent()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    private fun getFarsiSignalType(signalType: String): String {
        return when (signalType) {
            "BUY" -> "خرید"
            "SELL" -> "فروش"
            else -> "نامشخص"
        }
    }
    
    private fun showSettingsDialog() {
        addLog("📝 باز کردن تنظیمات ساعت کاری")
        // در نسخه کامل پیاده‌سازی میشه
    }
    
    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        editor.putInt("startHour", 8)
        editor.putInt("startMinute", 0)
        editor.putInt("endHour", 18)
        editor.putInt("endMinute", 0)
        editor.apply()
        
        workingHoursManager.loadSettings(sharedPreferences)
        updateWorkingHoursDisplay()
        addLog("✅ تنظیمات ساعت کاری ذخیره شد")
    }
    
    private fun loadSettings() {
        workingHoursManager.loadSettings(sharedPreferences)
    }
    
    private fun updateWorkingHoursDisplay() {
        val startTime = "${sharedPreferences.getInt("startHour", 8)}:${sharedPreferences.getInt("startMinute", 0).toString().padStart(2, '0')}"
        val endTime = "${sharedPreferences.getInt("endHour", 18)}:${sharedPreferences.getInt("endMinute", 0).toString().padStart(2, '0')}"
        tvWorkingHours.text = "ساعت کاری: $startTime - $endTime"
    }
    
    private fun addLog(message: String) {
        runOnUiThread {
            val currentText = tvLog.text.toString()
            val newText = "• $message\n$currentText"
            tvLog.text = newText
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
    }
}
