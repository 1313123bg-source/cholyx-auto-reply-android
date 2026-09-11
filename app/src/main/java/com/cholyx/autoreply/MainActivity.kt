package com.cholyx.autoreply

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("CholyxPrefs", 0) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply { text = "Cholyx Auto Reply"; textSize = 24f }
        val msg = EditText(this).apply {
            hint = "Автоматично съобщение"
            setText(prefs.getString("msg", "Нека продължим"))
        }
        val delay = EditText(this).apply {
            hint = "Забавяне в секунди"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay", 5).toString())
        }
        val limit = EditText(this).apply {
            hint = "Максимален брой повторения"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("limit", 10).toString())
        }
        val chats = EditText(this).apply {
            hint = "Разрешени чатове — по един ключов текст на ред"
            setText(prefs.getString("chats", ""))
            minLines = 3
        }

        val enabled = CheckBox(this).apply {
            text = "Автоматичният отговор е ВКЛЮЧЕН"
            isChecked = prefs.getBoolean("enabled", false)
        }
        val opera = CheckBox(this).apply {
            text = "Работи в Opera"
            isChecked = prefs.getBoolean("opera", true)
        }
        val chatgpt = CheckBox(this).apply {
            text = "Работи в приложението ChatGPT"
            isChecked = prefs.getBoolean("chatgpt", false)
        }

        val save = Button(this).apply {
            text = "ЗАПАЗИ НАСТРОЙКИТЕ"
            setOnClickListener {
                prefs.edit()
                    .putString("msg", msg.text.toString())
                    .putInt("delay", delay.text.toString().toIntOrNull() ?: 5)
                    .putInt("limit", limit.text.toString().toIntOrNull() ?: 10)
                    .putString("chats", chats.text.toString())
                    .putBoolean("enabled", enabled.isChecked)
                    .putBoolean("opera", opera.isChecked)
                    .putBoolean("chatgpt", chatgpt.isChecked)
                    .apply()
                Toast.makeText(this, "Настройките са запазени", Toast.LENGTH_SHORT).show()
            }
        }
        val open = Button(this).apply {
            text = "ОТВОРИ ACCESSIBILITY НАСТРОЙКИ"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val info = TextView(this).apply {
            text = "1. Избери къде да работи приложението.\n2. Включи/изключи автоматичния отговор от горния превключвател.\n3. Запази настройките.\n4. Включи Cholyx Auto Reply в Accessibility.\n\nЗа пълно спиране изключи превключвателя или Accessibility услугата.\n\nАко полето за чатове е празно, работи във всички разрешени ChatGPT чатове."
        }

        layout.addView(title)
        layout.addView(enabled)
        layout.addView(opera)
        layout.addView(chatgpt)
        layout.addView(msg)
        layout.addView(delay)
        layout.addView(limit)
        layout.addView(chats)
        layout.addView(save)
        layout.addView(open)
        layout.addView(info)
        setContentView(layout)
    }
}
