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
    private var lastCompletedSignature = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || pending) return

        val packageName = event.packageName?.toString() ?: return
        val prefs = getSharedPreferences("CholyxPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return

        val allowed = when (packageName) {
            "com.opera.browser" -> prefs.getBoolean("opera", true)
            "com.openai.chatgpt" -> prefs.getBoolean("chatgpt", false)
            else -> false
        }
        if (!allowed) return

        val root = rootInActiveWindow ?: return
        val screenText = collectText(root).lowercase()
        if (packageName == "com.opera.browser" && !looksLikeChatGpt(screenText)) return

        val message = prefs.getString("msg", "Нека продължим").orEmpty().trim()
        if (message.isEmpty()) return

        val limit = prefs.getInt("limit", 10).coerceIn(1, 100)
        if (sentCount >= limit) return

        val allowedChats = prefs.getString("chats", "").orEmpty()
            .lines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (allowedChats.isNotEmpty() && allowedChats.none { screenText.contains(it) }) return

        val edit = findEditable(root) ?: return
        val send = findSendButton(root) ?: return

        // Do not react to a screen that is still changing or to the same completed state.
        val signature = screenText.takeLast(1600)
        if (signature == lastCompletedSignature) return
        if (hasWorkingIndicator(screenText)) return

        pending = true
        val delayMs = prefs.getInt("delay", 5).coerceIn(1, 60) * 1000L
        handler.postDelayed({
            try {
                val currentRoot = rootInActiveWindow
                val currentText = currentRoot?.let { collectText(it).lowercase() }.orEmpty()
                if (currentRoot == null || hasWorkingIndicator(currentText)) {
                    pending = false
                    return@postDelayed
                }

                val currentEdit = findEditable(currentRoot)
                val currentSend = findSendButton(currentRoot)
                if (currentEdit == null || currentSend == null) {
                    pending = false
                    return@postDelayed
                }

                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        message
                    )
                }
                if (!currentEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    pending = false
                    return@postDelayed
                }

                handler.postDelayed({
                    try {
                        val finalRoot = rootInActiveWindow
                        val finalSend = finalRoot?.let { findSendButton(it) }
                        if (finalSend != null && finalSend.isEnabled) {
                            finalSend.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            sentCount++
                            lastCompletedSignature = currentText.takeLast(1600)
                        }
                    } finally {
                        pending = false
                    }
                }, 450)
            } catch (_: Exception) {
                pending = false
            }
        }, delayMs)
    }

    private fun looksLikeChatGpt(text: String): Boolean =
        text.contains("chatgpt") || text.contains("openai") ||
            text.contains("what can i help") || text.contains("какво мога") ||
            text.contains("как мога да помогна")

    private fun hasWorkingIndicator(text: String): Boolean =
        text.contains("stop generating") || text.contains("спри генерирането") ||
            text.contains("generating") || text.contains("генериране")

    private fun collectText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        fun walk(current: AccessibilityNodeInfo?) {
            if (current == null) return
            current.text?.toString()?.takeIf { it.isNotBlank() }?.let(parts::add)
            current.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(parts::add)
            for (i in 0 until current.childCount) walk(current.getChild(i))
        }
        walk(node)
        return parts.joinToString(" ")
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let(::findEditable)
            if (result != null) return result
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.contentDescription?.toString().orEmpty() + " " +
            node.text?.toString().orEmpty()).lowercase()
        val buttonLike = node.isClickable || node.className?.toString()?.contains("button", true) == true
        val matches = label.contains("send") || label.contains("изпрати") ||
            label.contains("submit") || label.contains("arrow up") ||
            label.contains("изпращане")
        if (buttonLike && matches && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let(::findSendButton)
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        pending = false
    }
}
