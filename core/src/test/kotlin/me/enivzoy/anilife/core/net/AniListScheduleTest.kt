package me.enivzoy.anilife.core.net

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.http.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AniListScheduleTest {
    @Test
    fun futureScheduleFetchesEveryPageIncludingPagesBeyondEight() = runBlocking {
        val from = 1_811_548_800L
        val until = from + 9 * 86_400
        val requests = mutableListOf<Triple<Int, Long, Long>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val variables = Json.parseToJsonElement(exchange.requestBody.bufferedReader().readText())
                .jsonObject.getValue("variables").jsonObject
            val page = variables.getValue("page").jsonPrimitive.content.toInt()
            requests += Triple(page, variables.getValue("from").jsonPrimitive.content.toLong(), variables.getValue("to").jsonPrimitive.content.toLong())
            val response = """
                {"data":{"Page":{"pageInfo":{"hasNextPage":${page < 11}},"airingSchedules":[
                  {"airingAt":${from + page * 3600},"episode":$page,"media":{"id":$page,"idMal":${page + 100},"title":{"english":"Future title $page"}}}
                ]}}}
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val client = AniListClient(HttpClient.newHttpClient(), "http://127.0.0.1:${server.address.port}/")
            val entries = client.scheduleRange(from, until)
            assertEquals(11, entries.size)
            assertEquals((1..11).toList(), requests.map { it.first })
            assertTrue(requests.all { it.second == from - 1 && it.third == until })
            assertEquals(111L, entries.last().anime.malId)
            assertTrue(entries.all { it.airingAt >= from && it.airingAt < until })
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun invalidRangesAreRejectedBeforeRequestingTheApi() = runBlocking {
        val client = AniListClient()
        assertFailsWith<IllegalArgumentException> { client.scheduleRange(100, 100) }
        assertFailsWith<IllegalArgumentException> { client.scheduleRange(-1, 100) }
        assertFailsWith<IllegalArgumentException> { client.scheduleRange(100, Int.MAX_VALUE.toLong() + 1) }
    }
}
