package com.cholyx.autoreply

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper

class ReplyAccessibilityService: AccessibilityService() {
 private val h=Handler(Looper.getMainLooper()); private var busy=false; private var count=0
 override fun onAccessibilityEvent(e: AccessibilityEvent?) { if(busy || (e?.packageName!="com.opera.browser" && e?.packageName!="com.openai.chatgpt")) return; val prefs=getSharedPreferences("CholyxPrefs",0); val msg=prefs.getString("msg","Нека продължим")?:"Нека продължим"; val delay=prefs.getInt("delay",5); val limit=prefs.getInt("limit",10); if(msg.isBlank()) return
  val allowed=prefs.getString("chats","")!!.lines().map{it.trim().lowercase()}.filter{it.isNotBlank()}; val root=rootInActiveWindow?:return; val text=root.text?.toString()?.lowercase()? :""; if(!text.contains("chatgpt")) return
  if(allowed.isNotEmpty() && allowed.none{text.contains(it)}) return; if(count>=limit) return; val edit=findEditable(root)?:return; busy=true
  h.postDelayed({ edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,msg)}); findSend(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK); count++; busy=false },delay*1000L)
 }
 private fun findEditable(n:AccessibilityNodeInfo):AccessibilityNodeInfo? { if(n.isEditable)return n; for(i in 0 until n.childCount){n.getChild(i)?.let{findEditable(it)?.also{return it}}};return null }
 private fun findSend(n:AccessibilityNodeInfo):AccessibilityNodeInfo? { val d=(n.contentDescription?.toString()+" "+n.text?.toString()).lowercase(); if(d.contains("send")||d.contains("изпрати"))return n; for(i in 0 until n.childCount){n.getChild(i)?.let{findSend(it)?.also{return it}}};return null }
 override fun onInterrupt() { h.removeCallbacksAndMessages(null) }
}
