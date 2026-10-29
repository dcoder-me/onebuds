package com.example.budsbattery.bt

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val WAIT_S = 5L

class SocketOpenTest {

    @Test
    fun `a connect that lands after the sync ended is closed, not left holding the channel`() = runBlocking {
        val connecting = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closed = AtomicReference<String?>(null)
        val attempt = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            openUnlessCancelled(
                open = {
                    connecting.countDown()
                    release.await(WAIT_S, TimeUnit.SECONDS)
                    "socket"
                },
                close = { closed.set(it) },
            )
        }
        connecting.await(WAIT_S, TimeUnit.SECONDS)
        // The buds leave mid-connect: the sync is cancelled, then Android finishes the connect anyway
        attempt.cancel()
        release.countDown()
        runCatching { attempt.await() }

        assertEquals("socket", closed.get())
    }

    @Test
    fun `a connect that lands while the sync runs is handed over open`() = runBlocking {
        val closed = AtomicReference<String?>(null)

        val socket = openUnlessCancelled(open = { "socket" }, close = { closed.set(it) })

        assertEquals("socket", socket)
        assertNull(closed.get())
    }
}
