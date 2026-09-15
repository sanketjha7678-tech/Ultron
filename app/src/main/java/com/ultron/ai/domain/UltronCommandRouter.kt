package com.ultron.ai.domain

import com.ultron.ai.actions.UltronActionExecutor
import java.util.Locale

class UltronCommandRouter(private val executor: UltronActionExecutor) {

    fun processCommand(input: String): CommandResult {
        val query = input.lowercase(Locale.ROOT).trim()

        // 1. YouTube actions & Hinglish support
        if (query.contains("youtube")) {
            return when {
                query.contains("search") || query.contains("par") || query.contains("kholo aur") -> {
                    val searchTerm = extractSearchTerm(query, listOf("search youtube for", "youtube par", "search", "kholo aur"))
                    executor.searchYouTube(searchTerm)
                }
                else -> executor.launchApp("com.google.android.youtube", "YouTube")
            }
        }

        // 2. Maps & Navigation
        if (query.contains("maps") || query.contains("navigate") || query.contains("location")) {
            return when {
                query.contains("navigate to") -> {
                    val dest = query.substringAfter("navigate to").trim()
                    executor.searchMapsOrNavigate(dest, isNavigation = true)
                }
                query.contains("search maps for") || query.contains("maps par") -> {
                    val dest = extractSearchTerm(query, listOf("search maps for", "maps par"))
                    executor.searchMapsOrNavigate(dest, isNavigation = false)
                }
                else -> executor.launchApp("com.google.android.apps.maps", "Google Maps")
            }
        }

        // 3. Voice Calling
        if (query.startsWith("call") || query.contains("phone lagao") || query.contains("ko call karo") || query.contains("ko call kar")) {
            val target = extractContactTarget(query)
            return executor.makePhoneCall(target)
        }

        // 4. Standard App Launches
        if (query.startsWith("open") || query.endsWith("kholo")) {
            val appName = extractAppName(query)
            return when (appName) {
                "instagram" -> executor.launchApp("com.instagram.android", "Instagram")
                "camera" -> executor.openSystemCamera()
                "chrome" -> executor.launchApp("com.android.chrome", "Chrome")
                "photos", "gallery" -> executor.launchApp("com.google.android.apps.photos", "Gallery")
                "settings" -> executor.launchApp("com.android.settings", "Settings")
                else -> executor.launchApp("com.$appName.android", appName)
            }
        }

        // 5. Web Search
        if (query.contains("search the web") || query.contains("search online") || query.startsWith("look up")) {
            val term = extractSearchTerm(query, listOf("search the web for", "search online for", "look up"))
            return executor.searchWeb(term)
        }

        // 6. Direct Knowledge Query (Fallback)
        return CommandResult.Success("Processing query: $input")
    }

    private fun extractSearchTerm(query: String, prefixes: List<String>): String {
        var clean = query
        for (prefix in prefixes) {
            if (clean.contains(prefix)) {
                clean = clean.substringAfter(prefix).replace("search karo", "").replace("kholo", "").trim()
                break
            }
        }
        return clean.ifEmpty { "Android AI" }
    }

    private fun extractContactTarget(query: String): String {
        return query.replace("call", "")
            .replace("mummy ko call karo", "Mom")
            .replace("papa ko phone lagao", "Dad")
            .replace("ko call karo", "")
            .replace("ko phone lagao", "")
            .replace("ko call kar", "")
            .trim()
    }

    private fun extractAppName(query: String): String {
        return query.replace("open", "").replace("kholo", "").trim()
    }
}
