package io.github.wuttttta.uwb.protocol.json

import java.util.Base64

/**
 * Byte strings travel as unpadded base64url. Decoding is strict: the text must be
 * exactly what [encode] would produce, so each byte string has one representation.
 */
object B64 {
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun encode(bytes: ByteArray): String = enc.encodeToString(bytes)

    fun decode(text: String): ByteArray {
        val out = try {
            dec.decode(text)
        } catch (e: IllegalArgumentException) {
            throw JsonException("invalid base64url")
        }
        if (encode(out) != text) throw JsonException("non-canonical base64url")
        return out
    }
}

val ByteArray.js get() = JsonValue.Str(B64.encode(this))

/** Typed accessors for reading untrusted objects; each throws JsonException on mismatch. */
fun JsonValue.Obj.str(key: String): String =
    (this[key] as? JsonValue.Str)?.value ?: throw JsonException("missing string \"$key\"")
fun JsonValue.Obj.long(key: String): Long =
    (this[key] as? JsonValue.Num)?.value ?: throw JsonException("missing integer \"$key\"")
fun JsonValue.Obj.int(key: String): Int {
    val v = long(key)
    if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) throw JsonException("\"$key\" out of range")
    return v.toInt()
}
fun JsonValue.Obj.bool(key: String): Boolean =
    (this[key] as? JsonValue.Bool)?.value ?: throw JsonException("missing boolean \"$key\"")
fun JsonValue.Obj.bytes(key: String): ByteArray = B64.decode(str(key))
fun JsonValue.Obj.obj(key: String): JsonValue.Obj =
    this[key] as? JsonValue.Obj ?: throw JsonException("missing object \"$key\"")
fun JsonValue.Obj.arr(key: String): List<JsonValue> =
    (this[key] as? JsonValue.Arr)?.items ?: throw JsonException("missing array \"$key\"")
