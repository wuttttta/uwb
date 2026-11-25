package io.github.wuttttta.uwb.protocol.transport

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException

/**
 * Two connected [PeerChannel] ends for tests and for running the whole flow
 * without radios.
 *
 * [aToB] and [bToA] sit "on the wire": they see every message in flight and may
 * return it unchanged, return modified bytes (a tampering attacker), or return null
 * to drop it. That is how tests play man in the middle.
 */
class InMemoryChannelPair(
    aToB: (ByteArray) -> ByteArray? = { it },
    bToA: (ByteArray) -> ByteArray? = { it },
) {
    private val toB = Channel<ByteArray>(Channel.UNLIMITED)
    private val toA = Channel<ByteArray>(Channel.UNLIMITED)

    val a: PeerChannel = End(outbox = toB, inbox = toA, wire = aToB)
    val b: PeerChannel = End(outbox = toA, inbox = toB, wire = bToA)

    private inner class End(
        private val outbox: Channel<ByteArray>,
        private val inbox: Channel<ByteArray>,
        private val wire: (ByteArray) -> ByteArray?,
    ) : PeerChannel {
        override suspend fun send(message: ByteArray) {
            if (message.size > PeerChannel.MAX_MESSAGE_BYTES) throw MessageTooLargeException(message.size)
            // Copy so neither side can mutate the other's buffer after the fact.
            val onWire = wire(message.copyOf()) ?: return
            try {
                outbox.send(onWire.copyOf())
            } catch (e: ClosedSendChannelException) {
                throw PeerChannelClosedException()
            }
        }

        override suspend fun receive(): ByteArray = try {
            inbox.receive()
        } catch (e: ClosedReceiveChannelException) {
            throw PeerChannelClosedException()
        }

        override fun close() {
            toA.close()
            toB.close()
        }
    }
}
