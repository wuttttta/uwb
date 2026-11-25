package io.github.wuttttta.uwb.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArgsTest {
    private val d1 = "ab".repeat(32)
    private val d2 = "CD:".repeat(31) + "CD"

    @Test
    fun `parses a full command line`() {
        val a = parseArgs(
            listOf("verify", "b.json", "--package", "x.y", "--cert-sha256", d1, d2, "--roots", "r.json", "--check-revocation"),
        ).getOrThrow()
        assertEquals(VerifyArgs("b.json", "x.y", listOf(d1, "cd".repeat(32)), "r.json", true), a)
    }

    @Test
    fun `reports missing pieces`() {
        assertTrue(parseArgs(listOf("verify", "--package", "x", "--cert-sha256", d1)).isFailure)
        assertTrue(parseArgs(listOf("verify", "b.json", "--cert-sha256", d1)).isFailure)
        assertTrue(parseArgs(listOf("verify", "b.json", "--package", "x")).isFailure)
        assertTrue(parseArgs(listOf("verify", "b.json", "--package", "x", "--cert-sha256", "abc")).isFailure)
        assertTrue(parseArgs(listOf("check", "b.json")).isFailure)
        assertTrue(parseArgs(listOf("verify", "b.json", "--package", "x", "--cert-sha256", d1, "--bogus")).isFailure)
    }
}
