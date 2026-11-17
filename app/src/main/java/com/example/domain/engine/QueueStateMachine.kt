package com.example.domain.engine

import com.example.data.model.TokenStatus
import com.example.data.model.UserRole

object QueueStateMachine {

    fun canTransition(current: TokenStatus, next: TokenStatus, actorRole: UserRole): Boolean {
        return when (current) {
            TokenStatus.WAITING -> when (next) {
                TokenStatus.CALLED -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN
                TokenStatus.CANCELLED -> true // Customer or Staff can cancel
                else -> false
            }
            TokenStatus.CALLED -> when (next) {
                TokenStatus.SERVING -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN
                TokenStatus.NO_SHOW -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN
                TokenStatus.WAITING -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN // Re-queue
                TokenStatus.CANCELLED -> true
                else -> false
            }
            TokenStatus.SERVING -> when (next) {
                TokenStatus.COMPLETED -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN
                TokenStatus.CANCELLED -> actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN
                else -> false
            }
            TokenStatus.COMPLETED -> false // Terminal state
            TokenStatus.CANCELLED -> false // Terminal state
            TokenStatus.NO_SHOW -> next == TokenStatus.WAITING && (actorRole == UserRole.STAFF || actorRole == UserRole.ADMIN)
        }
    }
}
