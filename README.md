# kvdock-kt

Thread-safe, generic key-value caching library for Kotlin and the JVM with zero external dependencies.

## Architecture

- **`KVDock`**: Thread-safe cache engine using atomic counters and key namespacing.
- **`StorageAdapter`**: Interface decoupling storage engines from client callers.
- **`MemoryAdapter`**: In-memory storage using `LinkedHashMap` for $O(1)$ LRU eviction and atomic TTL enforcement.
- **Cache-Aside Pattern**: `getOrSet` and `getOrSetString` evaluate lambdas only on misses.
- **Zero Runtime Dependencies**: Kotlin standard library only.

## Usage

```kotlin
import io.github.kvdock.KVDock
import io.github.kvdock.SetOptions
import java.time.Duration

fun main() {
    val dock = KVDock(prefix = "app", defaultTTL = Duration.ofMinutes(5))

    // cache-aside pattern
    val user = dock.getOrSetString("user:101") {
        database.queryUser(101)
    }

    println("cached user: $user")
}
```

## Testing

```sh
kotlinc src/main/kotlin/io/github/kvdock/*.kt src/test/kotlin/io/github/kvdock/*.kt -include-runtime -d test.jar
kotlin -cp test.jar io.github.kvdock.KVDockTestKt
```

## Ecosystem & Other Languages

KVDock is also available natively across multiple languages with matching design principles and zero external dependencies:

- **TypeScript / Node.js**: [`kvdock` on npm](https://www.npmjs.com/package/kvdock) | [`zisam-glitch/kvdock`](https://github.com/zisam-glitch/kvdock) — Reference implementation with Tiered Caching, SQLite, S3, Redis, and Cloudflare KV adapters.
- **Rust**: [`zisam-glitch/kvdock-rs`](https://github.com/zisam-glitch/kvdock-rs) — Thread-safe caching layer with monotonic `Instant` TTL.
- **Go**: [`zisam-glitch/kvdock-go`](https://github.com/zisam-glitch/kvdock-go) — Generic key-value cache engine with atomic telemetry.
- **Swift**: [`zisam-glitch/kvdock-swift`](https://github.com/zisam-glitch/kvdock-swift) — Actor-isolated concurrent cache with `ContinuousClock`.

## License

MIT
