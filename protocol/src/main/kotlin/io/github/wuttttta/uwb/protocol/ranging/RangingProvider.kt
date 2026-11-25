package io.github.wuttttta.uwb.protocol.ranging

import kotlinx.coroutines.flow.Flow

/** One distance measurement. Integer millimeters and milliseconds: no floats anywhere. */
data class RangingSample(val distanceMm: Int, val timestampMs: Long)

enum class UwbRole {
    /** Host, phone A: UWB controller, picks the channel and session id. */
    CONTROLLER,

    /** Join, phone B: UWB controlee. */
    CONTROLEE,
}

/** UWB complex channel: channel number plus preamble index, chosen by the controller. */
data class ComplexChannel(val channel: Int, val preambleIndex: Int)

/** What the radio reports about itself. Config ids use androidx.core.uwb's numbering. */
data class RangingCapabilitiesInfo(
    val supportedConfigIds: Set<Int>,
    val supportedUpdateRates: Set<Int>,
    val minRangingIntervalMs: Int,
) {
    val supportsProvisionedSts: Boolean get() = CONFIG_PROVISIONED_UNICAST_DS_TWR in supportedConfigIds

    companion object {
        // Values of androidx.core.uwb.RangingParameters.CONFIG_* (checked against the
        // 1.1.0-alpha03 reference). Duplicated here because protocol has no Android deps.
        const val CONFIG_UNICAST_DS_TWR = 1 // static STS: relay-susceptible, insecure
        const val CONFIG_PROVISIONED_UNICAST_DS_TWR = 4 // provisioned STS: what we require
    }
}

/**
 * Non-secret ranging parameters this phone advertises in its hello message.
 * The controller also fills [complexChannel] and [sessionId] for the controlee.
 */
data class LocalUwbInfo(
    val address: ByteArray,
    val complexChannel: ComplexChannel?,
    val sessionId: Int?,
) {
    override fun equals(other: Any?) = other is LocalUwbInfo &&
        address.contentEquals(other.address) && complexChannel == other.complexChannel && sessionId == other.sessionId
    override fun hashCode() = 31 * (31 * address.contentHashCode() + complexChannel.hashCode()) + (sessionId ?: 0)
}

/** Everything needed to start ranging with the peer. */
class RangingSessionParams(
    val configId: Int,
    val sessionId: Int,
    val complexChannel: ComplexChannel,
    val peerAddress: ByteArray,
    /** STS session key (16 bytes). Secret: never logged, and redacted from toString. */
    val stsKey: ByteArray,
    val updateRate: Int,
) {
    override fun toString() =
        "RangingSessionParams(configId=$configId, sessionId=$sessionId, channel=$complexChannel, " +
            "peerAddress=${peerAddress.toHex()}, stsKey=<redacted ${stsKey.size} bytes>, updateRate=$updateRate)"
}

/**
 * The UWB radio behind an interface, so everything above it runs on the JVM.
 * Implementations: JetpackUwbRangingProvider (app) and [FakeRangingProvider].
 *
 * Lifecycle: [open] a role to learn the local address (and, as controller, the
 * channel), exchange that with the peer, then collect [ranging]. Cancelling the
 * collection stops ranging; [close] releases the session.
 */
interface RangingProvider {
    suspend fun capabilities(): RangingCapabilitiesInfo

    suspend fun open(role: UwbRole): LocalUwbInfo

    /** Emits samples until cancelled; fails with [RangingException] on errors or peer loss. */
    fun ranging(params: RangingSessionParams): Flow<RangingSample>

    fun close()
}

class RangingException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
