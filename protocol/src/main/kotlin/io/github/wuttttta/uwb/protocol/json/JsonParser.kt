package io.github.wuttttta.uwb.protocol.json

/**
 * Strict parser for the protocol's JSON subset. Input comes from an untrusted peer
 * or from a bundle file, so it rejects rather than guesses:
 * floats and exponents, leading zeros, integers outside Long, duplicate keys,
 * lone surrogates, trailing data, and nesting deeper than [maxDepth].
 *
 * Whitespace between tokens is accepted (bundles may be pretty-printed); signatures
 * are always checked over the re-encoded canonical form, never the input bytes.
 */
class JsonParser private constructor(private val s: String, private val maxDepth: Int) {
    private var i = 0

    companion object {
        const val DEFAULT_MAX_DEPTH = 32

        fun parse(bytes: ByteArray, maxDepth: Int = DEFAULT_MAX_DEPTH): JsonValue {
            val text = Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .let {
                    try {
                        it.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
                    } catch (e: java.nio.charset.CharacterCodingException) {
                        throw JsonException("invalid UTF-8")
                    }
                }
            return parse(text, maxDepth)
        }

        fun parse(text: String, maxDepth: Int = DEFAULT_MAX_DEPTH): JsonValue =
            JsonParser(text, maxDepth).parseDocument()
    }

    private fun parseDocument(): JsonValue {
        skipWs()
        val v = value(0)
        skipWs()
        if (i != s.length) fail("trailing data")
        return v
    }

    private fun value(depth: Int): JsonValue {
        if (depth > maxDepth) fail("nesting too deep")
        if (i >= s.length) fail("unexpected end")
        return when (val c = s[i]) {
            '{' -> obj(depth)
            '[' -> arr(depth)
            '"' -> JsonValue.Str(string())
            't' -> literal("true", JsonValue.Bool(true))
            'f' -> literal("false", JsonValue.Bool(false))
            'n' -> literal("null", JsonValue.Null)
            else -> if (c == '-' || c in '0'..'9') number() else fail("unexpected '$c'")
        }
    }

    private fun obj(depth: Int): JsonValue.Obj {
        i++ // {
        val map = LinkedHashMap<String, JsonValue>()
        skipWs()
        if (peek() == '}') { i++; return JsonValue.Obj(map) }
        while (true) {
            skipWs()
            if (peek() != '"') fail("expected key")
            val key = string()
            skipWs()
            expect(':')
            skipWs()
            val v = value(depth + 1)
            if (map.put(key, v) != null) fail("duplicate key \"$key\"")
            skipWs()
            when (next()) {
                ',' -> continue
                '}' -> return JsonValue.Obj(map)
                else -> fail("expected ',' or '}'")
            }
        }
    }

    private fun arr(depth: Int): JsonValue.Arr {
        i++ // [
        val items = ArrayList<JsonValue>()
        skipWs()
        if (peek() == ']') { i++; return JsonValue.Arr(items) }
        while (true) {
            skipWs()
            items += value(depth + 1)
            skipWs()
            when (next()) {
                ',' -> continue
                ']' -> return JsonValue.Arr(items)
                else -> fail("expected ',' or ']'")
            }
        }
    }

    private fun string(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            if (i >= s.length) fail("unterminated string")
            val c = s[i++]
            when {
                c == '"' -> break
                c == '\\' -> {
                    when (val e = next()) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 4 > s.length) fail("bad \\u escape")
                            val code = s.substring(i, i + 4).toIntOrNull(16) ?: fail("bad \\u escape")
                            i += 4
                            sb.append(code.toChar())
                        }
                        else -> fail("bad escape \\$e")
                    }
                }
                c < ' ' -> fail("control character in string")
                else -> sb.append(c)
            }
        }
        val out = sb.toString()
        checkSurrogates(out)
        return out
    }

    private fun checkSurrogates(str: String) {
        var k = 0
        while (k < str.length) {
            val c = str[k]
            if (c.isHighSurrogate()) {
                if (k + 1 >= str.length || !str[k + 1].isLowSurrogate()) fail("lone surrogate")
                k += 2
            } else {
                if (c.isLowSurrogate()) fail("lone surrogate")
                k++
            }
        }
    }

    private fun number(): JsonValue.Num {
        val start = i
        if (peek() == '-') i++
        if (i >= s.length || s[i] !in '0'..'9') fail("bad number")
        if (s[i] == '0' && i + 1 < s.length && s[i + 1] in '0'..'9') fail("leading zero")
        while (i < s.length && s[i] in '0'..'9') i++
        if (i < s.length && (s[i] == '.' || s[i] == 'e' || s[i] == 'E')) fail("floats are not allowed")
        val text = s.substring(start, i)
        if (text == "-0") fail("negative zero")
        return JsonValue.Num(text.toLongOrNull() ?: fail("integer out of range"))
    }

    private fun literal(word: String, v: JsonValue): JsonValue {
        if (!s.startsWith(word, i)) fail("bad literal")
        i += word.length
        return v
    }

    private fun skipWs() {
        while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
    }

    private fun peek(): Char = if (i < s.length) s[i] else fail("unexpected end")
    private fun next(): Char = peek().also { i++ }
    private fun expect(c: Char) { if (next() != c) fail("expected '$c'") }
    private fun fail(msg: String): Nothing = throw JsonException("$msg at offset $i")
}
