package com.halil.ozel.huaweipushkitapp.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushCliTest {
    @Test
    fun notificationBodyMatchesTheSampleMessage() {
        val json = notificationBody(
            title = "Huawei \"Push\"",
            body = "Hello",
            pushToken = "token-1",
            validateOnly = false,
        )
        assertTrue(json.contains("\"validate_only\": false"))
        assertTrue(json.contains("\"title\": \"Huawei \\\"Push\\\"\""))
        assertTrue(json.contains("\"type\": 3"))
        assertTrue(json.contains("\"token\": [\"token-1\"]"))
    }

    @Test
    fun dataBodyUsesTheAppChannelKeys() {
        val json = dataMessageBody(
            title = "Hi",
            text = "there",
            channelId = "channel_1",
            pushToken = "token-1",
            validateOnly = true,
        )
        assertTrue(json.contains("\"validate_only\": true"))
        val data = extractJsonString(json, "data")
        assertEquals(
            """{"title":"Hi","text":"there","channel_id":"channel_1"}""",
            data,
        )
    }

    @Test
    fun germanyUsesTheEuropeanPushHost() {
        assertEquals(
            "https://push-api.cloud.huawei.eu/v1/123456789/messages:send",
            PushRegion.GERMANY.sendUrl("123456789"),
        )
        assertEquals(
            "https://push-api.cloud.huawei.com/v1/123456789/messages:send",
            PushRegion.CHINA.sendUrl("123456789"),
        )
    }

    @Test
    fun tokenCommandPrintsTheAccessToken() {
        val http = FakeHttp(
            HttpResult(200, """{"access_token":"abc","token_type":"Bearer"}"""),
        )
        val out = StringBuilder()
        val code = PushCli(http = http, stdout = out, stderr = StringBuilder()).run(
            arrayOf("token", "--app-id", "123456789", "--app-secret", "secret"),
        )
        assertEquals(0, code)
        assertEquals("abc\n", out.toString())
        assertEquals("https://oauth-login.cloud.huawei.eu/oauth2/v3/token", http.calls.single().url)
        assertTrue(http.calls.single().body.contains("grant_type=client_credentials"))
        assertTrue(http.calls.single().body.contains("client_id=123456789"))
        assertTrue(http.calls.single().body.contains("client_secret=secret"))
    }

    @Test
    fun notifySendsTheTokenAndReportsPushErrors() {
        val http = FakeHttp(
            HttpResult(200, """{"access_token":"abc"}"""),
            HttpResult(200, """{"code":"80300007","msg":"token invalid"}"""),
        )
        val err = StringBuilder()
        val code = PushCli(http = http, stdout = StringBuilder(), stderr = err).run(
            arrayOf(
                "notify",
                "--region", "china",
                "--app-id", "123456789",
                "--app-secret", "secret",
                "--push-token", "device",
                "--title", "Hi",
                "--body", "Hello",
            ),
        )
        assertEquals(1, code)
        assertTrue(err.toString().contains("80300007"))
        assertEquals(
            "https://push-api.cloud.huawei.com/v1/123456789/messages:send",
            http.calls[1].url,
        )
        assertEquals("Bearer abc", http.calls[1].headers["Authorization"])
    }

    @Test
    fun topicDataMessageOmitsTheDeviceToken() {
        val json = topicDataBody(
            title = "Hi",
            text = "there",
            channelId = "channel_1",
            topic = "weather",
            validateOnly = false,
        )
        assertTrue(json.contains("\"topic\": \"weather\""))
        assertTrue(!json.contains("\"token\""))
        assertEquals(
            """{"title":"Hi","text":"there","channel_id":"channel_1"}""",
            extractJsonString(json, "data"),
        )
    }

    @Test
    fun topicCommandSendsWithoutADeviceToken() {
        val http = FakeHttp(
            HttpResult(200, """{"access_token":"abc"}"""),
            HttpResult(200, """{"code":"80000000","msg":"Success"}"""),
        )
        val code = PushCli(http = http, stdout = StringBuilder(), stderr = StringBuilder()).run(
            arrayOf(
                "topic",
                "--app-id", "123456789",
                "--app-secret", "secret",
                "--topic", "weather",
                "--title", "Hi",
                "--body", "Hello",
            ),
        )
        assertEquals(0, code)
        assertTrue(http.calls[1].body.contains("\"topic\": \"weather\""))
        assertTrue(!http.calls[1].body.contains("push-token"))
    }

    @Test
    fun missingCredentialsDoesNotCallTheNetwork() {
        val http = FakeHttp()
        val code = PushCli(http = http, stdout = StringBuilder(), stderr = StringBuilder()).run(
            arrayOf("data", "--title", "Hi", "--text", "Hello"),
        )
        assertEquals(2, code)
        assertTrue(http.calls.isEmpty())
    }

    private class FakeHttp(private vararg val results: HttpResult) : HttpPoster {
        val calls = mutableListOf<Call>()

        override fun post(
            url: String,
            contentType: String,
            body: String,
            headers: Map<String, String>,
        ): HttpResult {
            calls += Call(url, body, headers)
            return results[calls.lastIndex]
        }
    }

    private data class Call(val url: String, val body: String, val headers: Map<String, String>)
}
