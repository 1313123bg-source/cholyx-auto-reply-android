package com.cholyx.autoreply

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) { super.onCreate(b)
    val layout=LinearLayout(this); layout.orientation=LinearLayout.VERTICAL; layout.setPadding(32,32,32,32)
    val title=TextView(this); title.text="Cholyx Auto Reply"; title.textSize=24f
    val msg=EditText(this); msg.hint="Автоматично съобщение"; msg.setText(getSharedPreferences("CholyxPrefs",0).getString("msg","Нека продължим"))
    val delay=EditText(this); delay.hint="Забавяне в секунди"; delay.setText("5"); delay.inputType=2
    val limit=EditText(this); limit.hint="Максимален брой повторения"; limit.setText("10"); limit.inputType=2
    val chats=EditText(this); chats.hint="Разрешени чатове — по един ключов текст на ред"; chats.setText(getSharedPreferences("CholyxPrefs",0).getString("chats","")); chats.minLines=3
    val save=Button(this); save.text="Запази настройките"; save.setOnClickListener{getSharedPreferences("CholyxPrefs",0).edit().putString("msg",msg.text.toString()).putInt("delay",delay.text.toString().toIntOrNull()?:5).putInt("limit",limit.text.toString().toIntOrNull()?:10).putString("chats",chats.text.toString()).apply(); Toast.makeText(this,"Запазено",Toast.LENGTH_SHORT).show()}
    val open=Button(this); open.text="Отвори Accessibility настройки"; open.setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
    val info=TextView(this); info.text="1. Запази съобщението.\n2. Включи Cholyx Auto Reply в Accessibility.\n3. Отвори ChatGPT в Opera.\n4. За спиране изключи услугата.\n\nЧатове: въведи ключова дума от заглавието на разрешените чатове, по една на ред. Ако полето е празно, работи във всички ChatGPT чатове."
    layout.addView(title); layout.addView(msg); layout.addView(delay); layout.addView(limit); layout.addView(chats); layout.addView(save); layout.addView(open); layout.addView(info); setContentView(layout)
    if (!getSharedPreferences("CholyxPrefs",0).getBoolean("access_prompt_shown", false)) { getSharedPreferences("CholyxPrefs",0).edit().putBoolean("access_prompt_shown", true).apply(); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
  }
}
