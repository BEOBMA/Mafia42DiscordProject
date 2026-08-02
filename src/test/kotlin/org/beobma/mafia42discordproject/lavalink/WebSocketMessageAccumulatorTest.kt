package org.beobma.mafia42discordproject.lavalink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebSocketMessageAccumulatorTest {
    @Test
    fun `combines fragmented websocket messages`() {
        val accumulator = WebSocketMessageAccumulator()

        assertNull(accumulator.append("""{"op":"event","type":""", last = false))
        assertEquals(
            """{"op":"event","type":"TrackEndEvent"}""",
            accumulator.append(""""TrackEndEvent"}""", last = true)
        )
    }

    @Test
    fun `clears completed messages before receiving the next one`() {
        val accumulator = WebSocketMessageAccumulator()

        assertEquals("first", accumulator.append("first", last = true))
        assertEquals("second", accumulator.append("second", last = true))
    }
}
