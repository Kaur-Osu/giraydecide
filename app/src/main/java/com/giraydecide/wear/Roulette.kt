package com.giraydecide.wear

import java.security.SecureRandom
import java.util.UUID

object RouletteRules {
    const val MAX_OPTIONS = 16
    const val MAX_LABEL = 48
    fun normalized(label: String) = label.trim().lowercase(java.util.Locale.ROOT)
    fun canAdd(current: List<String>, label: String): Boolean =
        label.trim().length in 1..MAX_LABEL && current.size < MAX_OPTIONS &&
            current.none { normalized(it) == normalized(label) }
    fun pick(size: Int, random: SecureRandom = SecureRandom()): Int {
        require(size >= 2)
        return random.nextInt(size)
    }
    // Segment 0 starts at 12 o'clock. Pointer stays at 12 o'clock.
    fun targetRotation(index: Int, size: Int): Float {
        require(size >= 2 && index in 0 until size)
        return (360f - (index + 0.5f) * 360f / size) % 360f
    }
}
data class Roulette(val id: String = UUID.randomUUID().toString(), val name: String, val options: List<String>, val createdAt: Long = System.currentTimeMillis())
data class SpinRecord(val id: String = UUID.randomUUID().toString(), val name: String, val result: String, val time: Long = System.currentTimeMillis())