package io.github.wuttttta.uwb.protocol

import io.github.wuttttta.uwb.protocol.ranging.ComplexChannel
import io.github.wuttttta.uwb.protocol.ranging.FakeRangingProvider
import io.github.wuttttta.uwb.protocol.ranging.RangingCapabilitiesInfo
import io.github.wuttttta.uwb.protocol.ranging.RangingException
import io.github.wuttttta.uwb.protocol.ranging.RangingSample
import io.github.wuttttta.uwb.protocol.ranging.RangingSessionParams
import io.github.wuttttta.uwb.protocol.ranging.UwbRole
import io.github.wuttttta.uwb.protocol.transport.InMemoryChannelPair
import io.github.wuttttta.uwb.protocol.transport.MessageTooLargeException
import io.github.wuttttta.uwb.protocol.transport.PeerChannel
import io.github.wuttttta.uwb.protocol.transport.PeerChannelClosedException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakesTest {
    private fun params(configId: Int = RangingCapabilitiesInfo.CONFIG_PROVISIONED_UNICAST_DS_TWR) = RangingSessionParams(
        configId = configId,
        sessionId = 1,
        complexChannel = ComplexChannel(9, 10),
        peerAddress = byteArrayOf(1, 2),
        stsKey = ByteArray(16) { 0x42 },
        updateRate = 3,
    )

    @Test
    fun `channel pair delivers in order both ways`() = runTest {
        val pair = InMemoryChannelPair()
        pair.a.send(byteArrayOf(1))
        pair.a.send(byteArrayOf(2))
        pair.b.send(byteArrayOf(9))
        assertContentEquals(byteArrayOf(1), pair.b.receive())
        assertContentEquals(byteArrayOf(2), pair.b.receive())
        assertContentEquals(byteArrayOf(9), pair.a.receive())
    }

    @Test
    fun `sender mutating its buffer after send does not change the delivered message`() = runTest {
        val pair = InMemoryChannelPair()
        val buf = byteArrayOf(1, 2, 3)
        pair.a.send(buf)
        buf[0] = 99
        assertContentEquals(byteArrayOf(1, 2, 3), pair.b.receive())
    }

    @Test
    fun `wire hooks can tamper with and drop messages`() = runTest {
        val pair = InMemoryChannelPair(aToB = { m -> if (m[0] == 0.toByte()) null else m.also { it[0] = 7 } })
        pair.a.send(byteArrayOf(0)) // dropped
        pair.a.send(byteArrayOf(1)) // tampered
        assertContentEquals(byteArrayOf(7), pair.b.receive())
    }

    @Test
    fun `closing one end fails the other`() = runTest {
        val pair = InMemoryChannelPair()
        pair.a.close()
        assertFailsWith<PeerChannelClosedException> { pair.b.receive() }
        assertFailsWith<PeerChannelClosedException> { pair.b.send(byteArrayOf(1)) }
    }

    @Test
    fun `oversized messages are refused`() = runTest {
        val pair = InMemoryChannelPair()
        assertFailsWith<MessageTooLargeException> { pair.a.send(ByteArray(PeerChannel.MAX_MESSAGE_BYTES + 1)) }
    }

    @Test
    fun `fake ranging emits the script on virtual time and skips missing rounds`() = runTest {
        val fake = FakeRangingProvider(listOf(300, null, 250), intervalMs = 100, clock = { testScheduler.currentTime })
        val samples = mutableListOf<RangingSample>()
        val error = runCatching { fake.ranging(params()).toList(samples) }.exceptionOrNull()
        assertEquals(listOf(RangingSample(300, 100), RangingSample(250, 300)), samples)
        assertTrue(error is RangingException, "script end reports peer loss")
    }

    @Test
    fun `fake ranging can repeat until cancelled`() = runTest {
        val fake = FakeRangingProvider(listOf(100, 200), repeat = true, clock = { testScheduler.currentTime })
        val samples = fake.ranging(params()).take(5).toList()
        assertEquals(listOf(100, 200, 100, 200, 100), samples.map { it.distanceMm })
    }

    @Test
    fun `fake ranging rejects unsupported configs`() = runTest {
        val staticOnly = RangingCapabilitiesInfo(setOf(RangingCapabilitiesInfo.CONFIG_UNICAST_DS_TWR), setOf(1), 96)
        val fake = FakeRangingProvider(listOf(1), capabilities = staticOnly)
        assertFalse(fake.capabilities().supportsProvisionedSts)
        var failed = false
        fake.ranging(params()).catch { failed = it is RangingException }.toList()
        assertTrue(failed)
    }

    @Test
    fun `controller gets channel and session id, controlee does not`() = runTest {
        val fake = FakeRangingProvider(emptyList())
        val ctl = fake.open(UwbRole.CONTROLLER)
        val cte = fake.open(UwbRole.CONTROLEE)
        assertTrue(ctl.complexChannel != null && ctl.sessionId != null)
        assertTrue(cte.complexChannel == null && cte.sessionId == null)
    }

    @Test
    fun `session params never print the STS key`() {
        val text = params().toString()
        assertFalse(text.contains("42"), text)
        assertTrue(text.contains("redacted"))
    }
}
