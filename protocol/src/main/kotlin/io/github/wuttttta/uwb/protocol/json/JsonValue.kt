package io.github.wuttttta.uwb.protocol.json

/**
 * The JSON subset used by protocol v0: objects, arrays, strings, booleans, null and
 * 64-bit integers. Floats are deliberately unsupported (distances are integer mm,
 * times integer ms), which keeps the canonical form unambiguous.
 */
sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue {
        operator fun get(key: String): JsonValue? = fields[key]
    }
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Long) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

class JsonException(message: String) : Exception(message)

/** Small builders so protocol code reads like the spec. */
fun jsonObj(vararg pairs: Pair<String, JsonValue>): JsonValue.Obj {
    val map = LinkedHashMap<String, JsonValue>()
    for ((k, v) in pairs) {
        require(map.put(k, v) == null) { "duplicate key $k" }
    }
    return JsonValue.Obj(map)
}
fun jsonArr(items: List<JsonValue>) = JsonValue.Arr(items)
val String.js get() = JsonValue.Str(this)
val Long.js get() = JsonValue.Num(this)
val Int.js get() = JsonValue.Num(this.toLong())
val Boolean.js get() = JsonValue.Bool(this)
