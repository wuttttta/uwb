package io.github.wuttttta.uwb.protocol.transport

/**
 * An ordered, reliable byte-message pipe to the other phone (Nearby Connections on
 * the device, in-memory in tests). It is **untrusted**: anything received may have
 * been written by an attacker, and the protocol authenticates on top of it.
 */
interface PeerChannel {
    /** Sends one framed message. Throws [PeerChannelClosedException] once closed. */
    suspend fun send(message: ByteArray)

    /**
     * Suspends until the next message arrives. Throws [PeerChannelClosedException]
     * when either side has closed the channel.
     */
    suspend fun receive(): ByteArray

    /** Closes both directions. Idempotent. */
    fun close()

    companion object {
        /** Upper bound for one message; certificate chains are the largest payload (a few KiB). */
        const val MAX_MESSAGE_BYTES = 64 * 1024
    }
}

class PeerChannelClosedException(message: String = "peer channel closed") : Exception(message)

class MessageTooLargeException(size: Int) :
    Exception("message of $size bytes exceeds ${PeerChannel.MAX_MESSAGE_BYTES}")
