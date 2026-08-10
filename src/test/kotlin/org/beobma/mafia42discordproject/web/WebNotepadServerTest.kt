package org.beobma.mafia42discordproject.web

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebNotepadServerTest {
    @AfterTest
    fun tearDown() {
        WebNotepadServer.stop()
    }

    @Test
    fun servesHealthAndWebApplicationOnLoopback() {
        val port = WebNotepadServer.startForTests()
        val baseUrl = "http://127.0.0.1:$port"
        val client = HttpClient.newHttpClient()

        val health = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val page = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val notepad = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/notepad")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val laboratory = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/lab")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val replays = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/replays")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateState = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/state")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateEvents = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/events")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateAbility = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/ability"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateGeneralNote = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/general-note"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""{"content":"메모"}"""))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val privateLaboratory = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/lab/state")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val laboratorySession = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/lab/session"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertEquals(200, health.statusCode())
        assertTrue(health.body().contains("\"status\":\"ok\""))
        assertEquals(200, page.statusCode())
        assertTrue(page.body().contains("게임 리플레이 아카이브"))
        assertEquals(200, notepad.statusCode())
        assertTrue(notepad.body().contains("게임 메모장"))
        assertTrue(notepad.body().contains("자유 메모"))
        assertEquals(200, laboratory.statusCode())
        assertTrue(laboratory.body().contains("게임 실험실"))
        assertEquals(200, replays.statusCode())
        assertTrue(replays.body().contains("\"replays\""))
        assertEquals(401, privateState.statusCode())
        assertTrue(privateState.body().contains("/메모장"))
        assertEquals(401, privateEvents.statusCode())
        assertEquals(401, privateAbility.statusCode())
        assertEquals(401, privateGeneralNote.statusCode())
        assertEquals(401, privateLaboratory.statusCode())
        assertEquals(201, laboratorySession.statusCode())
        assertTrue(laboratorySession.body().contains("\"token\""))
        assertTrue(laboratorySession.body().contains("\"phase\":\"SETUP\""))

        val laboratoryToken = Regex("\"token\":\"([^\"]+)\"")
            .find(laboratorySession.body())
            ?.groupValues
            ?.get(1)
            ?: error("실험실 토큰이 응답에 없습니다.")
        val setup = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/lab/setup"))
                .header("X-Lab-Session", laboratoryToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                    {"players":[
                        {"name":"나","jobName":"시민"},
                        {"name":"봇 1","jobName":"마피아"},
                        {"name":"봇 2","jobName":"의사"},
                        {"name":"봇 3","jobName":"경찰"}
                    ]}
                """.trimIndent()))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val start = client.send(
            HttpRequest.newBuilder(URI("$baseUrl/api/lab/start"))
                .header("X-Lab-Session", laboratoryToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, setup.statusCode())
        assertEquals(200, start.statusCode())
        assertTrue(start.body().contains("\"phase\":\"NIGHT\""))
    }

    @Test
    fun keepsMadScientistDeadInNotepadUntilRevivalIsPublic() {
        assertTrue(
            WebNotepadServer.isPubliclyDeadForNotepad(
                isDead = false,
                isMadScientistRevivalHidden = true
            )
        )
        assertTrue(
            WebNotepadServer.isPubliclyDeadForNotepad(
                isDead = true,
                isMadScientistRevivalHidden = false
            )
        )
        assertFalse(
            WebNotepadServer.isPubliclyDeadForNotepad(
                isDead = false,
                isMadScientistRevivalHidden = false
            )
        )
    }
}
