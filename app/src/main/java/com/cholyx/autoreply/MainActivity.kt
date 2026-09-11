package com.cholyx.autoreply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.graphics.Color
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("CholyxPrefs", 0) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Cholyx Auto Reply"
            textSize = 24f
        }

        val status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 12, 0, 12)
        }

        val enabled = CheckBox(this).apply {
            text = "Автоматичният отговор е ВКЛЮЧЕН"
            isChecked = prefs.getBoolean("enabled", false)
        }
        val opera = CheckBox(this).apply {
            text = "Работи в Opera с ChatGPT"
            isChecked = prefs.getBoolean("opera", true)
        }
        val chatgpt = CheckBox(this).apply {
            text = "Работи в приложението ChatGPT"
            isChecked = prefs.getBoolean("chatgpt", false)
        }

        val msg = EditText(this).apply {
            hint = "Автоматично съобщение"
            setText(prefs.getString("msg", "Нека продължим"))
        }
        val delay = EditText(this).apply {
            hint = "Забавяне след завършен отговор, секунди"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay", 5).toString())
        }
        val limit = EditText(this).apply {
            hint = "Максимален брой автоматични изпращания"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("limit", 10).toString())
        }
        val chats = EditText(this).apply {
            hint = "Разрешени чатове — по един ключов текст на ред"
            setText(prefs.getString("chats", ""))
            minLines = 3
        }

        fun refreshStatus() {
            status.text = if (enabled.isChecked) {
                "Статус: ВКЛЮЧЕНО"
            } else {
                "Статус: СПРЯНО"
            }
        }
        refreshStatus()

        val save = Button(this).apply {
            text = "ЗАПАЗИ НАСТРОЙКИТЕ"
            setOnClickListener {
                prefs.edit()
                    .putString("msg", msg.text.toString())
                    .putInt("delay", delay.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 5)
                    .putInt("limit", limit.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 10)
                    .putString("chats", chats.text.toString())
                    .putBoolean("enabled", enabled.isChecked)
                    .putBoolean("opera", opera.isChecked)
                    .putBoolean("chatgpt", chatgpt.isChecked)
                    .apply()
                refreshStatus()
                Toast.makeText(this@MainActivity, "Настройките са запазени", Toast.LENGTH_SHORT).show()
            }
        }

        val stop = Button(this).apply {
            text = "СПРИ АВТОМАТИЧНИТЕ ОТГОВОРИ"
            setOnClickListener {
                enabled.isChecked = false
                prefs.edit().putBoolean("enabled", false).apply()
                refreshStatus()
                Toast.makeText(this@MainActivity, "Автоматичните отговори са спрени", Toast.LENGTH_SHORT).show()
            }
        }

        val open = Button(this).apply {
            text = "ОТВОРИ ACCESSIBILITY НАСТРОЙКИ"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val info = TextView(this).apply {
            text = "1. Избери Opera или приложението ChatGPT.\n2. Въведи съобщението.\n3. Задай забавяне и максимален брой изпращания.\n4. Натисни ЗАПАЗИ НАСТРОЙКИТЕ.\n5. Включи Cholyx Auto Reply в Android Accessibility.\n\nЗа спиране използвай СПРИ АВТОМАТИЧНИТЕ ОТГОВОРИ или изключи Accessibility услугата.\n\nОстави „Разрешени чатове“ празно, за да работи във всички ChatGPT чатове в избраното приложение."
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(enabled)
        layout.addView(opera)
        layout.addView(chatgpt)
        layout.addView(msg)
        layout.addView(delay)
        layout.addView(limit)
        layout.addView(chats)
        layout.addView(save)
        layout.addView(stop)
        layout.addView(open)
        layout.addView(info)
        setContentView(layout)
    }
}
