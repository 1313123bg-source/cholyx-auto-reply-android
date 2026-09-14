package com.cholyx.autoreply

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReplyAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pending = false
    private var sentCount = 0
    private var lastSignature = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || pending) return
        if (event.packageName?.toString() != "com.openai.chatgpt") return

        val prefs = getSharedPreferences("CholyxPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false) || !prefs.getBoolean("chatgpt", true)) return

        val root = rootInActiveWindow ?: return
        val text = collectText(root).lowercase()
        val message = prefs.getString("msg", "Нека продължим").orEmpty().trim()
        if (message.isEmpty()) return
        if (sentCount >= prefs.getInt("limit", 10).coerceIn(1, 100)) return
        if (isGenerating(text)) return

        val edit = findEditable(root) ?: return
        if (findSend(root) == null) return
        val signature = text.takeLast(2000)
        if (signature == lastSignature) return

        pending = true
        val delay = prefs.getInt("delay", 5).coerceIn(1, 60) * 1000L
        handler.postDelayed({
            val current = rootInActiveWindow
            if (current == null) {
                pending = false
                return@postDelayed
            }
            val currentText = collectText(current).lowercase()
            if (isGenerating(currentText)) {
                pending = false
                return@postDelayed
            }
            val currentEdit = findEditable(current)
            if (currentEdit == null) {
                pending = false
                return@postDelayed
            }

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            }
            if (!currentEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                pending = false
                return@postDelayed
            }

            handler.postDelayed({
                try {
                    val finalRoot = rootInActiveWindow ?: return@postDelayed
                    val finalSend = findSend(finalRoot) ?: return@postDelayed
                    if (finalSend.isEnabled && finalSend.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        sentCount++
                        lastSignature = signature
                    }
                } finally {
                    pending = false
                }
            }, 700)
        }, delay)
    }

    private fun isGenerating(text: String): Boolean =
        text.contains("stop generating") || text.contains("спри генерирането")

    private fun collectText(node: AccessibilityNodeInfo): String {
        val result = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            n.text?.toString()?.let { result.append(' ').append(it) }
            n.contentDescription?.toString()?.let { result.append(' ').append(it) }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(node)
        return result.toString()
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) findEditable(node.getChild(i) ?: continue)?.let { return it }
        return null
    }

    private fun findSend(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty()).lowercase()
        val id = node.viewIdResourceName.orEmpty().lowercase()
        val matches = label.contains("send") || label.contains("изпрати") ||
            label.contains("submit") || label.contains("arrow up") ||
            id.contains("send") || id.contains("submit")
        if ((node.isClickable || node.className?.toString()?.contains("button", true) == true) && matches && node.isEnabled) return node
        for (i in 0 until node.childCount) findSend(node.getChild(i) ?: continue)?.let { return it }
        return null
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        pending = false
    }
}
