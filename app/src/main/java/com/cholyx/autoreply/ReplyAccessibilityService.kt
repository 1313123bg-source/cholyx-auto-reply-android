package com.cholyx.autoreply

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReplyAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var count = 0
    private var lastScreenSignature = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (busy) return

        val prefs = getSharedPreferences("CholyxPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return

        val operaAllowed = prefs.getBoolean("opera", true)
        val chatgptAllowed = prefs.getBoolean("chatgpt", false)
        val packageAllowed = when (packageName) {
            "com.opera.browser" -> operaAllowed
            "com.openai.chatgpt" -> chatgptAllowed
            else -> false
        }
        if (!packageAllowed) return

        val message = prefs.getString("msg", "Нека продължим") ?: "Нека продължим"
        val delaySeconds = prefs.getInt("delay", 5).coerceAtLeast(1)
        val limit = prefs.getInt("limit", 10).coerceAtLeast(1)
        if (message.isBlank() || count >= limit) return

        val root = rootInActiveWindow ?: return
        val screenText = collectText(root).lowercase()
        if (packageName == "com.opera.browser" && !isChatGPTScreen(screenText)) return

        val allowedChats = prefs.getString("chats", "").orEmpty()
            .lines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (allowedChats.isNotEmpty() && allowedChats.none { screenText.contains(it) }) return

        val signature = screenText.takeLast(1200)
        if (signature == lastScreenSignature) return
        lastScreenSignature = signature

        if (findEditable(root) == null || findSendButton(root) == null) return

        busy = true
        handler.postDelayed({
            try {
                val currentRoot = rootInActiveWindow
                val currentEdit = currentRoot?.let { findEditable(it) }
                val currentSend = currentRoot?.let { findSendButton(it) }
                if (currentEdit != null && currentSend != null) {
                    val args = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            message
                        )
                    }
                    currentEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    handler.postDelayed({
                        currentSend.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        count++
                        busy = false
                    }, 300)
                } else busy = false
            } catch (_: Exception) { busy = false }
        }, delaySeconds * 1000L)
    }

    private fun isChatGPTScreen(text: String): Boolean =
        text.contains("chatgpt") || text.contains("openai") ||
        text.contains("what can i help") || text.contains("какво мога")

    private fun collectText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        fun walk(current: AccessibilityNodeInfo?) {
            if (current == null) return
            current.text?.toString()?.let { if (it.isNotBlank()) parts.add(it) }
            current.contentDescription?.toString()?.let { if (it.isNotBlank()) parts.add(it) }
            for (i in 0 until current.childCount) walk(current.getChild(i))
        }
        walk(node)
        return parts.joinToString(" ")
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findEditable(it) }
            if (result != null) return result
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.contentDescription?.toString().orEmpty() + " " +
            node.text?.toString().orEmpty()).lowercase()
        val isButton = node.className?.toString()?.contains("button", true) == true || node.isClickable
        val matches = label.contains("send") || label.contains("изпрати") ||
            label.contains("submit") || label.contains("arrow up")
        if (isButton && matches && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findSendButton(it) }
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        busy = false
    }
}
