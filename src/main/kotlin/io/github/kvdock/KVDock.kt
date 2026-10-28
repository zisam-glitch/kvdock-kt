package io.github.kvdock

import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class KVDock(
    private val adapter: StorageAdapter = MemoryAdapter(),
    private val prefix: String = "",
    private val defaultTTL: Duration? = null
) {
    init {
        if (defaultTTL != null && (defaultTTL.isNegative || defaultTTL.isZero)) {
            throw KVDockException.InvalidTTL(defaultTTL)
        }
    }

    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val sets = AtomicLong(0)
    private val deletes = AtomicLong(0)

    val stats: CacheStats
        get() = CacheStats(
            hits = hits.get(),
            misses = misses.get(),
            sets = sets.get(),
            deletes = deletes.get()
        )

    private fun resolveKey(key: String): String {
        if (key.isEmpty()) {
            throw KVDockException.EmptyKey()
        }
        return if (prefix.isEmpty()) key else "$prefix:$key"
    }

    fun get(key: String): ByteArray? {
        val resolved = resolveKey(key)
        val value = adapter.get(resolved)
        if (value != null) {
            hits.incrementAndGet()
        } else {
            misses.incrementAndGet()
        }
        return value
    }

    fun getString(key: String): String? {
        return get(key)?.toString(Charsets.UTF_8)
    }

    fun set(key: String, value: ByteArray, options: SetOptions = SetOptions(ttl = defaultTTL)) {
        val resolved = resolveKey(key)
        val ttl = options.ttl ?: defaultTTL
        if (ttl != null && (ttl.isNegative || ttl.isZero)) {
            throw KVDockException.InvalidTTL(ttl)
        }
        adapter.set(resolved, value, SetOptions(ttl = ttl))
        sets.incrementAndGet()
    }

    fun set(key: String, value: String, options: SetOptions = SetOptions(ttl = defaultTTL)) {
        set(key, value.toByteArray(Charsets.UTF_8), options)
    }

    fun getOrSet(key: String, options: SetOptions = SetOptions(ttl = defaultTTL), fetcher: () -> ByteArray): ByteArray {
        val cached = get(key)
        if (cached != null) {
            return cached
        }
        val fresh = fetcher()
        set(key, fresh, options)
        return fresh
    }

    fun getOrSetString(key: String, options: SetOptions = SetOptions(ttl = defaultTTL), fetcher: () -> String): String {
        val cached = getString(key)
        if (cached != null) {
            return cached
        }
        val fresh = fetcher()
        set(key, fresh, options)
        return fresh
    }

    fun delete(key: String): Boolean {
        val resolved = resolveKey(key)
        val deleted = adapter.delete(resolved)
        if (deleted) {
            deletes.incrementAndGet()
        }
        return deleted
    }

    fun has(key: String): Boolean {
        val resolved = resolveKey(key)
        return adapter.has(resolved)
    }

    fun clear() {
        adapter.clear()
    }

    fun close() {
        adapter.close()
    }
}
