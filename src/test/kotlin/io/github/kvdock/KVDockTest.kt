package io.github.kvdock

import java.time.Duration

fun main() {
    testBasicSetAndGet()
    testCacheMiss()
    testGetOrSet()
    testTTLExpiration()
    testLRUEviction()
    testValidationErrors()
    println("All Kotlin KVDock tests passed successfully!")
}

fun testBasicSetAndGet() {
    val dock = KVDock(prefix = "test")
    dock.set("user:1", "alice")

    val cached = dock.getString("user:1")
    check(cached == "alice") { "expected 'alice', got '$cached'" }

    val stats = dock.stats
    check(stats.sets == 1L) { "expected 1 set" }
    check(stats.hits == 1L) { "expected 1 hit" }
}

fun testCacheMiss() {
    val dock = KVDock()
    val missing = dock.getString("unknown")
    check(missing == null) { "expected null" }

    val stats = dock.stats
    check(stats.misses == 1L) { "expected 1 miss" }
}

fun testGetOrSet() {
    val dock = KVDock()
    var calls = 0

    val v1 = dock.getOrSetString("calc") {
        calls++
        "42"
    }
    check(v1 == "42" && calls == 1) { "v1=$v1 calls=$calls" }

    val v2 = dock.getOrSetString("calc") {
        calls++
        "99"
    }
    check(v2 == "42" && calls == 1) { "v2=$v2 calls=$calls" }
}

fun testTTLExpiration() {
    val dock = KVDock()
    dock.set("temp", "val", SetOptions(ttl = Duration.ofMillis(20)))

    val immediate = dock.getString("temp")
    check(immediate == "val") { "expected 'val', got '$immediate'" }

    Thread.sleep(50)
    val expired = dock.getString("temp")
    check(expired == null) { "expected expired key to be null" }
}

fun testLRUEviction() {
    val adapter = MemoryAdapter(maxEntries = 2)
    val dock = KVDock(adapter = adapter)

    dock.set("k1", "v1")
    dock.set("k2", "v2")
    dock.getString("k1") // access k1 so k2 is LRU
    dock.set("k3", "v3")

    check(dock.getString("k1") == "v1") { "k1 should exist" }
    check(dock.getString("k2") == null) { "k2 should be evicted" }
    check(dock.getString("k3") == "v3") { "k3 should exist" }
}

fun testValidationErrors() {
    val dock = KVDock()
    try {
        dock.set("", "val")
        error("expected EmptyKey exception")
    } catch (_: KVDockException.EmptyKey) {}

    try {
        dock.set("k", "val", SetOptions(ttl = Duration.ofSeconds(-1)))
        error("expected InvalidTTL exception")
    } catch (_: KVDockException.InvalidTTL) {}
}
