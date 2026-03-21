package org.beobma.mafia42discordproject.discord

import dev.kord.rest.request.KtorRequestException

object InteractionErrorHandler {
    private val ignorableTokens = listOf(
        "Unknown interaction",
        "10062",
        "Interaction has already been acknowledged",
        "40060"
    )

    fun isIgnorable(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val message = current.message.orEmpty()
            if (ignorableTokens.any { token -> message.contains(token, ignoreCase = true) }) {
                return true
            }

            if (current is KtorRequestException) {
                val ktorMessage = current.error.message.orEmpty()
                if (ignorableTokens.any { token -> ktorMessage.contains(token, ignoreCase = true) }) {
                    return true
                }
                val code = current.error.code?.toString().orEmpty()
                if (code == "10062" || code == "40060") {
                    return true
                }
            }

            current = current.cause
        }

        return false
    }

    suspend inline fun runSafely(context: String, crossinline block: suspend () -> Unit) {
        runCatching { block() }
            .onFailure { error ->
                if (isIgnorable(error)) {
                    println("ℹ️ 무시 가능한 인터랙션 오류 발생 [$context]: ${error.message}")
                } else {
                    println("⚠️ 인터랙션 처리 실패 [$context]: ${error.message}")
                    throw error
                }
            }
    }
}
