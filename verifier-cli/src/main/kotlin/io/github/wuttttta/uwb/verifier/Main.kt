package io.github.wuttttta.uwb.verifier

import kotlin.system.exitProcess

/** Parsed `verify` command line. */
data class VerifyArgs(
    val bundlePath: String,
    val packageName: String,
    val certSha256: List<String>,
    val rootsPath: String?,
    val checkRevocation: Boolean,
)

const val USAGE =
    "usage: poe-verify verify <bundle> --package <name> --cert-sha256 <hex>... [--roots <file>] [--check-revocation]"

/** Returns the parsed arguments, or an error message. */
fun parseArgs(args: List<String>): Result<VerifyArgs> = runCatching {
    require(args.firstOrNull() == "verify") { USAGE }
    var bundle: String? = null
    var pkg: String? = null
    val certs = mutableListOf<String>()
    var roots: String? = null
    var revocation = false
    var i = 1
    fun value(flag: String): String = args.getOrNull(++i)?.takeUnless { it.startsWith("--") }
        ?: throw IllegalArgumentException("$flag needs a value")
    while (i < args.size) {
        when (val a = args[i]) {
            "--package" -> pkg = value(a)
            "--cert-sha256" -> {
                certs += value(a)
                // Accept several digests after one flag: --cert-sha256 <a> <b>
                while (args.getOrNull(i + 1)?.startsWith("--") == false) certs += args[++i]
            }
            "--roots" -> roots = value(a)
            "--check-revocation" -> revocation = true
            else -> {
                require(!a.startsWith("--")) { "unknown option $a" }
                require(bundle == null) { "only one bundle per run" }
                bundle = a
            }
        }
        i++
    }
    requireNotNull(bundle) { "missing <bundle>" }
    requireNotNull(pkg) { "missing --package" }
    require(certs.isNotEmpty()) { "missing --cert-sha256" }
    val hex = Regex("^[0-9a-fA-F]{64}$")
    certs.forEach { require(hex.matches(it.replace(":", ""))) { "not a SHA-256 hex digest: $it" } }
    VerifyArgs(bundle, pkg, certs.map { it.replace(":", "").lowercase() }, roots, revocation)
}

fun main(args: Array<String>) {
    val parsed = parseArgs(args.toList()).getOrElse {
        System.err.println(it.message)
        exitProcess(64)
    }
    // Bundle verification arrives in M4; until then never print PASS.
    System.err.println("FAIL: verification not implemented yet (milestone M4). Args: $parsed")
    exitProcess(2)
}
