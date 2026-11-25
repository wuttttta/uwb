package io.github.wuttttta.uwb.protocol.json

/**
 * Canonical encoding for everything that is hashed or signed.
 *
 * Rules (a subset of RFC 8785, JCS, restricted to integers):
 * - no whitespace;
 * - object keys sorted by UTF-16 code units (Kotlin's String.compareTo);
 * - integers in plain decimal, no leading zeros, "-0" never produced;
 * - strings: escape `"` and `\`, use \b \f \n \r \t for those controls, \u00xx
 *   (lowercase hex) for other controls below 0x20; everything else as UTF-8;
 * - output is UTF-8 bytes.
 *
 * The same value always yields the same bytes, whatever order fields were built in.
 */
object CanonicalJson {
    fun encode(value: JsonValue): ByteArray = encodeToString(value).toByteArray(Charsets.UTF_8)

    fun encodeToString(value: JsonValue): String = buildString { write(value) }

    private fun StringBuilder.write(v: JsonValue) {
        when (v) {
            is JsonValue.Obj -> {
                append('{')
                v.fields.keys.sorted().forEachIndexed { i, k ->
                    if (i > 0) append(',')
                    writeString(k)
                    append(':')
                    write(v.fields.getValue(k))
                }
                append('}')
            }
            is JsonValue.Arr -> {
                append('[')
                v.items.forEachIndexed { i, item ->
                    if (i > 0) append(',')
                    write(item)
                }
                append(']')
            }
            is JsonValue.Str -> writeString(v.value)
            is JsonValue.Num -> append(v.value)
            is JsonValue.Bool -> append(if (v.value) "true" else "false")
            JsonValue.Null -> append("null")
        }
    }

    private fun StringBuilder.writeString(s: String) {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') {
                    append("\\u00")
                    append(HEX[c.code shr 4])
                    append(HEX[c.code and 0xF])
                } else {
                    append(c)
                }
            }
        }
        append('"')
    }

    private const val HEX = "0123456789abcdef"
}
