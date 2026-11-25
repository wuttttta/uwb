package io.github.wuttttta.uwb.protocol.ranging

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scripted radio for tests and radio-less runs.
 *
 * [script] lists one entry per ranging round: a distance in mm, or null for a
 * round with no valid reading (nothing is emitted, time still passes). After the
 * script, the flow either ends with a [RangingException] ("peer lost") or, with
 * [repeat], starts over. Timestamps come from [clock], so tests on virtual time get
 * deterministic values.
 */
class FakeRangingProvider(
    private val script: List<Int?>,
    private val intervalMs: Long = 200,
    private val repeat: Boolean = false,
    private val capabilities: RangingCapabilitiesInfo = RangingCapabilitiesInfo(
        supportedConfigIds = setOf(
            RangingCapabilitiesInfo.CONFIG_UNICAST_DS_TWR,
            RangingCapabilitiesInfo.CONFIG_PROVISIONED_UNICAST_DS_TWR,
        ),
        supportedUpdateRates = setOf(1, 2, 3),
        minRangingIntervalMs = 96,
    ),
    private val address: ByteArray = byteArrayOf(0x0A, 0x0B),
    private val clock: () -> Long = System::currentTimeMillis,
) : RangingProvider {
    var lastParams: RangingSessionParams? = null
        private set
    var closed = false
        private set

    override suspend fun capabilities() = capabilities

    override suspend fun open(role: UwbRole): LocalUwbInfo = when (role) {
        UwbRole.CONTROLLER -> LocalUwbInfo(address.copyOf(), ComplexChannel(channel = 9, preambleIndex = 10), sessionId = 0x1234)
        UwbRole.CONTROLEE -> LocalUwbInfo(address.copyOf(), complexChannel = null, sessionId = null)
    }

    override fun ranging(params: RangingSessionParams): Flow<RangingSample> = flow {
        if (params.configId !in capabilities.supportedConfigIds) {
            throw RangingException("config ${params.configId} not supported")
        }
        lastParams = params
        do {
            for (mm in script) {
                delay(intervalMs)
                if (mm != null) emit(RangingSample(mm, clock()))
            }
        } while (repeat)
        throw RangingException("peer lost (end of script)")
    }

    override fun close() {
        closed = true
    }
}
