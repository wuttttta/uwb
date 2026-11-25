package io.github.wuttttta.uwb

import io.github.wuttttta.uwb.ui.KNOWN_LIMITS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KnownLimitsTest {
    @Test
    fun `about screen lists the four known limits`() {
        assertEquals(4, KNOWN_LIMITS.size)
    }

    @Test
    fun `shipped builds keep the insecure and secret-logging flags off`() {
        assertFalse(BuildConfig.INSECURE_STATIC_STS)
        assertFalse(BuildConfig.DEBUG_LOG_SECRETS)
    }
}
