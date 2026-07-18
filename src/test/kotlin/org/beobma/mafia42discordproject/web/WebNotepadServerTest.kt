package org.beobma.mafia42discordproject.web

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebNotepadServerTest {
    @AfterTest
    fun tearDown() {
        WebNotepadServer.stop()
    }

    @Test
    fun servesHealthAndWebApplicationOnLoopback() {
        WebNotepadServer.start()
        val client = HttpClient.newHttpClient()

        val health = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:8080/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val page = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:8080/")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val notepad = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:8080/notepad")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val replays = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:8080/api/replays")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateState = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:8080/api/state")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertEquals(200, health.statusCode())
        assertTrue(health.body().contains("\"status\":\"ok\""))
        assertEquals(200, page.statusCode())
        assertTrue(page.body().contains("게임 리플레이 아카이브"))
        assertEquals(200, notepad.statusCode())
        assertTrue(notepad.body().contains("게임 메모장"))
        assertEquals(200, replays.statusCode())
        assertTrue(replays.body().contains("\"replays\""))
        assertEquals(401, privateState.statusCode())
        assertTrue(privateState.body().contains("/메모장"))
    }
}
