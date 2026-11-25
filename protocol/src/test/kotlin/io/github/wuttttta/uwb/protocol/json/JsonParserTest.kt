package io.github.wuttttta.uwb.protocol.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonParserTest {
    private fun rejects(text: String) {
        assertFailsWith<JsonException>(text) { JsonParser.parse(text) }
    }

    @Test
    fun `accepts the protocol subset`() {
        val v = JsonParser.parse("""{"v":0,"n":-12,"s":"x","b":false,"z":null,"a":[{},[]]}""") as JsonValue.Obj
        assertEquals(0L, v.long("v"))
        assertEquals(-12, v.int("n"))
        assertEquals("x", v.str("s"))
        assertEquals(false, v.bool("b"))
        assertEquals(JsonValue.Null, v["z"])
        assertEquals(2, v.arr("a").size)
    }

    @Test
    fun `rejects floats and exponents`() {
        rejects("1.5")
        rejects("1e3")
        rejects("[0.0]")
        rejects("""{"d":12E-1}""")
    }

    @Test
    fun `rejects non-canonical or out of range integers`() {
        rejects("01")
        rejects("-0")
        rejects("-")
        rejects("9223372036854775808")
    }

    @Test
    fun `rejects duplicate keys`() {
        rejects("""{"a":1,"a":2}""")
    }

    @Test
    fun `rejects malformed documents`() {
        rejects("")
        rejects("{")
        rejects("[1,]")
        rejects("""{"a":1,}""")
        rejects("""{"a" 1}""")
        rejects("{} {}")
        rejects("tru")
        rejects("\"unterminated")
        rejects("\"bad \\x escape\"")
        rejects("\"raw\u0001control\"")
        rejects("\"\\ud800\"") // lone surrogate
        rejects("NaN")
    }

    @Test
    fun `rejects invalid UTF-8`() {
        assertFailsWith<JsonException> { JsonParser.parse(byteArrayOf('"'.code.toByte(), 0xC3.toByte(), '"'.code.toByte())) }
    }

    @Test
    fun `limits nesting depth`() {
        JsonParser.parse("[".repeat(32) + "]".repeat(32))
        rejects("[".repeat(34) + "]".repeat(34))
    }

    @Test
    fun `typed accessors reject wrong types`() {
        val v = JsonParser.parse("""{"s":"x","n":1,"big":4294967296}""") as JsonValue.Obj
        assertFailsWith<JsonException> { v.long("s") }
        assertFailsWith<JsonException> { v.str("n") }
        assertFailsWith<JsonException> { v.str("missing") }
        assertFailsWith<JsonException> { v.int("big") }
    }
}
