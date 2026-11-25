package io.github.wuttttta.uwb.protocol.json

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CanonicalJsonTest {
    @Test
    fun `keys are sorted and there is no whitespace`() {
        val v = jsonObj("b" to 1.js, "a" to jsonObj("z" to true.js, "y" to JsonValue.Null), "c" to "x".js)
        assertEquals("""{"a":{"y":null,"z":true},"b":1,"c":"x"}""", CanonicalJson.encodeToString(v))
    }

    @Test
    fun `field insertion order does not change the bytes`() {
        val rnd = Random(42)
        val fields = (0 until 30).map { "k$it" to rnd.nextLong().js }
        val reference = CanonicalJson.encode(jsonObj(*fields.toTypedArray()))
        repeat(20) {
            val shuffled = fields.shuffled(rnd)
            assertContentEquals(reference, CanonicalJson.encode(jsonObj(*shuffled.toTypedArray())))
        }
    }

    @Test
    fun `keys sort by UTF-16 code units`() {
        // U+FF61 sorts after U+1F600 (surrogate pair D83D DE00) in UTF-16 order, the JCS rule.
        val v = jsonObj("｡" to 1.js, "😀" to 2.js, "a" to 3.js, "B" to 4.js)
        assertEquals("{\"B\":4,\"a\":3,\"😀\":2,\"｡\":1}", CanonicalJson.encodeToString(v))
    }

    @Test
    fun `string escaping`() {
        val v = "q\"b\\s/\b\u000C\n\r\t\u0001\u001Fé€".js
        assertEquals("\"q\\\"b\\\\s/\\b\\f\\n\\r\\t\\u0001\\u001fé€\"", CanonicalJson.encodeToString(v))
    }

    @Test
    fun `integers including extremes`() {
        val v = jsonArr(listOf(0.js, (-1).js, Long.MAX_VALUE.js, Long.MIN_VALUE.js))
        assertEquals("[0,-1,9223372036854775807,-9223372036854775808]", CanonicalJson.encodeToString(v))
    }

    @Test
    fun `parse then encode round-trips and normalizes whitespace and order`() {
        val messy = """ { "b" : [ 1 , 2 ] ,
            "a" : "é\/" } """
        val parsed = JsonParser.parse(messy)
        assertEquals("""{"a":"é/","b":[1,2]}""", CanonicalJson.encodeToString(parsed))
        val canonical = CanonicalJson.encode(parsed)
        assertContentEquals(canonical, CanonicalJson.encode(JsonParser.parse(canonical)))
    }

    @Test
    fun `byte strings are unpadded base64url and strictly decoded`() {
        val bytes = byteArrayOf(-5, -1, 0, 1, 2)
        assertEquals("-_8AAQI", B64.encode(bytes))
        assertContentEquals(bytes, B64.decode("-_8AAQI"))
        assertFailsWith<JsonException> { B64.decode("-_8AAQI=") } // padding
        assertFailsWith<JsonException> { B64.decode("+/8AAQI") } // standard alphabet
        assertFailsWith<JsonException> { B64.decode("-_8AAQJ") } // non-zero trailing bits
    }
}
