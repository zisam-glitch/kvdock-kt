package io.github.kvdock

import java.time.Duration

data class StorageCapabilities(
    val batch: Boolean = false,
    val streaming: Boolean = false,
    val ttl: Boolean = false
)

data class SetOptions(
    val ttl: Duration? = null
)

data class CacheStats(
    val hits: Long = 0,
    val misses: Long = 0,
    val sets: Long = 0,
    val deletes: Long = 0
)

interface StorageAdapter {
    val capabilities: StorageCapabilities
    fun get(key: String): ByteArray?
    fun set(key: String, value: ByteArray, options: SetOptions)
    fun delete(key: String): Boolean
    fun has(key: String): Boolean
    fun clear()
    fun close()
}

sealed class KVDockException(message: String) : RuntimeException(message) {
    class InvalidTTL(val ttl: Duration) : KVDockException("kvdock: invalid ttl $ttl, must be positive")
    class EmptyKey : KVDockException("kvdock: key cannot be empty")
    class AdapterError(message: String) : KVDockException("kvdock: adapter error: $message")
}
