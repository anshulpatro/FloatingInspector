package com.anshulpatro.floatinginspector

import android.os.Looper
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageDispatcherTest {

    /**
     * Regression test for a field crash: NullPointerException in LinkedList$ListItr.next()
     * when setService/setSink iterated the pending-message queue on the main thread while
     * background threads were still enqueueing messages into the same (non-thread-safe) list.
     */
    @Test
    fun concurrentLoggingWhileServiceConnectsDeliversEveryMessageExactlyOnce() {
        val dispatcher = DebugOverlay.MessageDispatcher()
        val delivered = Collections.synchronizedList(mutableListOf<String>())
        val threadCount = 8
        val messagesPerThread = 500
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        repeat(threadCount) { t ->
            Thread {
                start.await()
                repeat(messagesPerThread) { i ->
                    dispatcher.enqueueMessage("msg-$t-$i")
                }
                done.countDown()
            }.start()
        }

        start.countDown()
        // Attach the sink mid-flight on the main thread — this mirrors onServiceConnected
        // racing against log() calls from background threads.
        dispatcher.setSink { delivered.add(it) }

        assertTrue("workers did not finish in time", done.await(30, TimeUnit.SECONDS))
        shadowOf(Looper.getMainLooper()).idle()

        val expected = threadCount * messagesPerThread
        assertEquals(expected, delivered.size)
        assertEquals("messages must not be dispatched twice", expected, delivered.toSet().size)
    }

    @Test
    fun messagesEnqueuedBeforeSinkAttachesAreDispatchedInOrder() {
        val dispatcher = DebugOverlay.MessageDispatcher()
        val delivered = mutableListOf<String>()

        dispatcher.enqueueMessage("first")
        dispatcher.enqueueMessage("second")
        dispatcher.setSink { delivered.add(it) }
        dispatcher.enqueueMessage("third")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf("first", "second", "third"), delivered)
    }
}
