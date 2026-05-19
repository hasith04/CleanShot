package com.cleanshot.app.utils

/**
 * Infers the originating app from MediaStore path / filename heuristics.
 * Used for library sorting and section grouping.
 */
object ScreenshotSourceDetector {

    const val OTHER = "Other"

    private data class SourceRule(val label: String, val tokens: List<String>)

    private val rules = listOf(
        SourceRule("WhatsApp", listOf("whatsapp")),
        SourceRule("Instagram", listOf("instagram")),
        SourceRule("Chrome", listOf("chrome")),
        SourceRule("YouTube", listOf("youtube")),
        SourceRule("X", listOf("twitter", "x_twitter", "/x/")),
        SourceRule("Telegram", listOf("telegram")),
        SourceRule("Snapchat", listOf("snapchat")),
        SourceRule("Facebook", listOf("facebook")),
        SourceRule("Messenger", listOf("messenger")),
        SourceRule("TikTok", listOf("tiktok")),
        SourceRule("Discord", listOf("discord")),
        SourceRule("Signal", listOf("signal")),
        SourceRule("Reddit", listOf("reddit")),
        SourceRule("Slack", listOf("slack")),
        SourceRule("LinkedIn", listOf("linkedin")),
        SourceRule("Pinterest", listOf("pinterest"))
    )

    fun detect(relativePath: String, displayName: String): String {
        val haystack = buildString {
            append(relativePath.lowercase())
            append('|')
            append(displayName.lowercase())
        }
        for (rule in rules) {
            if (rule.tokens.any { token -> haystack.contains(token) }) {
                return rule.label
            }
        }
        return OTHER
    }
}
