package com.ultron.ai.domain

enum class UltronState {
    STANDBY,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    ERROR
}

sealed class CommandResult {
    data class Success(val responseMessage: String, val executedAction: String? = null) : CommandResult()
    data class Failure(val errorMessage: String, val missingPermission: String? = null) : CommandResult()
    data class AmbiguousContact(val name: String, val matches: List<ContactItem>) : CommandResult()
}

data class ContactItem(
    val id: String,
    val displayName: String,
    val phoneNumber: String
)

data class HudMessage(
    val title: String,
    val detail: String,
    val isUser: Boolean = false
)
