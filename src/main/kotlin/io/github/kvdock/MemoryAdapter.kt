package io.github.kvdock

import java.time.Duration
import java.util.LinkedHashMap

class MemoryAdapter(
    private val maxEntries: Int = 10_000
) : StorageAdapter {

    private data class Entry(
        val value: ByteArray,
        val expiresAt: Long?
    ) {
        fun isExpired(now: Long): Boolean = expiresAt != null && now >= expiresAt
    }

    override val capabilities = StorageCapabilities(batch = false, streaming = false, ttl = true)

    private val lock = Any()
    private val cache = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Entry>?): Boolean {
            return size > maxEntries
        }
    }

    override fun get(key: String): ByteArray? = synchronized(lock) {
        val entry = cache[key] ?: return null
        val now = System.currentTimeMillis()
        if (entry.isExpired(now)) {
            cache.remove(key)
            return null
        }
        return entry.value.copyOf()
    }

    override fun set(key: String, value: ByteArray, options: SetOptions): Unit = synchronized(lock) {
        val now = System.currentTimeMillis()
        val expiresAt = options.ttl?.let { now + it.toMillis() }
        cache[key] = Entry(value.copyOf(), expiresAt)
    }

    override fun delete(key: String): Boolean = synchronized(lock) {
        return cache.remove(key) != null
    }

    override fun has(key: String): Boolean = synchronized(lock) {
        return get(key) != null
    }

    override fun clear(): Unit = synchronized(lock) {
        cache.clear()
    }

    override fun close() {
        clear()
    }
}
