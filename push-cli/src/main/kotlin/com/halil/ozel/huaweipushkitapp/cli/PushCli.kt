package com.halil.ozel.huaweipushkitapp.cli

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

data class HttpResult(val status: Int, val body: String)

fun interface HttpPoster {
    fun post(
        url: String,
        contentType: String,
        body: String,
        headers: Map<String, String>,
    ): HttpResult
}

class JavaHttpPoster : HttpPoster {
    private val client: HttpClient = HttpClient.newHttpClient()

    override fun post(
        url: String,
        contentType: String,
        body: String,
        headers: Map<String, String>,
    ): HttpResult {
        val requestBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        val response = client.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        return HttpResult(response.statusCode(), response.body())
    }
}

class PushCli(
    private val http: HttpPoster = JavaHttpPoster(),
    private val stdout: Appendable = System.out,
    private val stderr: Appendable = System.err,
    private val env: Map<String, String> = System.getenv(),
) {
    fun run(args: Array<String>): Int {
        if (args.isEmpty() || args[0] == "--help" || args[0] == "help") {
            stdout.append(HELP)
            return 0
        }
        val command = args[0]
        val options = try {
            parseOptions(args.drop(1))
        } catch (error: IllegalArgumentException) {
            stderr.append(error.message).append('\n')
            return USAGE
        }
        if (options.help) {
            stdout.append(HELP)
            return 0
        }
        return when (command) {
            "token" -> accessToken(options)
            "notify" -> sendNotification(options)
            "data" -> sendData(options)
            "topic" -> sendTopic(options)
            else -> {
                stderr.append("Unknown command: $command\n")
                stderr.append(HELP)
                USAGE
            }
        }
    }

    private fun accessToken(options: Options): Int {
        val appId = options.require("app-id", "HUAWEI_APP_ID") ?: return USAGE
        val appSecret = options.require("app-secret", "HUAWEI_APP_SECRET") ?: return USAGE
        if (!APP_ID.matches(appId)) {
            stderr.append("app-id must be the numeric AppGallery Connect app id.\n")
            return USAGE
        }
        return try {
            val token = requestAccessToken(appId, appSecret, options.region)
            stdout.append(token).append('\n')
            0
        } catch (error: PushRequestException) {
            stderr.append(error.message).append('\n')
            API
        }
    }

    private fun sendNotification(options: Options): Int {
        val title = options.values["title"]
        val body = options.values["body"]
        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            stderr.append("notify requires --title and --body.\n")
            return USAGE
        }
        val payload = { token: String ->
            notificationBody(title, body, token, options.validateOnly)
        }
        return send(options, payload)
    }

    private fun sendData(options: Options): Int {
        val title = options.values["title"]
        val text = options.values["text"]
        if (title.isNullOrBlank() || text.isNullOrBlank()) {
            stderr.append("data requires --title and --text.\n")
            return USAGE
        }
        val channelId = options.values["channel"] ?: DEFAULT_CHANNEL
        val payload = { token: String ->
            dataMessageBody(title, text, channelId, token, options.validateOnly)
        }
        return send(options, payload)
    }

    private fun sendTopic(options: Options): Int {
        val topic = options.values["topic"].orEmpty()
        if (!TOPIC.matches(topic)) {
            stderr.append("topic requires --topic using letters, digits, and -_.~%.\n")
            return USAGE
        }
        val title = options.values["title"]
        if (title.isNullOrBlank()) {
            stderr.append("topic requires --title.\n")
            return USAGE
        }
        val text = options.values["text"]
        val payload = if (!text.isNullOrBlank()) {
            val channelId = options.values["channel"] ?: DEFAULT_CHANNEL
            topicDataBody(title, text, channelId, topic, options.validateOnly)
        } else {
            val body = options.values["body"]
            if (body.isNullOrBlank()) {
                stderr.append("topic requires --body, or --text for a data message.\n")
                return USAGE
            }
            topicNotificationBody(title, body, topic, options.validateOnly)
        }
        return sendPayload(options, payload)
    }

    private fun send(options: Options, payload: (String) -> String): Int {
        val pushToken = options.require("push-token", "HUAWEI_PUSH_TOKEN") ?: return USAGE
        return sendPayload(options, payload(pushToken))
    }

    private fun sendPayload(options: Options, payload: String): Int {
        val appId = options.require("app-id", "HUAWEI_APP_ID") ?: return USAGE
        val appSecret = options.require("app-secret", "HUAWEI_APP_SECRET") ?: return USAGE
        if (!APP_ID.matches(appId)) {
            stderr.append("app-id must be the numeric AppGallery Connect app id.\n")
            return USAGE
        }
        return try {
            val accessToken = requestAccessToken(appId, appSecret, options.region)
            val result = http.post(
                url = options.region.sendUrl(appId),
                contentType = "application/json; charset=UTF-8",
                body = payload,
                headers = mapOf("Authorization" to "Bearer $accessToken"),
            )
            stdout.append(result.body).append('\n')
            if (result.status !in 200..299) {
                stderr.append("Push Kit returned HTTP ${result.status}.\n")
                return API
            }
            val code = extractJsonString(result.body, "code")
            if (code != null && code != SUCCESS_CODE) {
                stderr.append("Push Kit error code $code.\n")
                return API
            }
            0
        } catch (error: PushRequestException) {
            stderr.append(error.message).append('\n')
            API
        }
    }

    private fun requestAccessToken(appId: String, appSecret: String, region: PushRegion): String {
        val form = listOf(
            "grant_type" to "client_credentials",
            "client_id" to appId,
            "client_secret" to appSecret,
        ).joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        val result = http.post(
            url = region.tokenUrl,
            contentType = "application/x-www-form-urlencoded",
            body = form,
            headers = emptyMap(),
        )
        if (result.status !in 200..299) {
            throw PushRequestException("OAuth returned HTTP ${result.status}.")
        }
        return extractJsonString(result.body, "access_token")
            ?: throw PushRequestException("OAuth response did not include an access token.")
    }

    private fun Options.require(flag: String, envName: String): String? {
        val value = values[flag] ?: env[envName]
        if (value.isNullOrBlank()) {
            stderr.append("Missing --$flag or $envName.\n")
            return null
        }
        return value
    }

    private class PushRequestException(message: String) : RuntimeException(message)

    private data class Options(
        val values: Map<String, String>,
        val region: PushRegion,
        val validateOnly: Boolean,
        val help: Boolean,
    )

    private fun parseOptions(args: List<String>): Options {
        val values = linkedMapOf<String, String>()
        var region = PushRegion.GERMANY
        var validateOnly = false
        var help = false
        var index = 0
        while (index < args.size) {
            val arg = args[index]
            when {
                arg == "--help" -> help = true
                arg == "--validate-only" -> validateOnly = true
                arg.startsWith("--") -> {
                    val name = arg.removePrefix("--")
                    val value = args.getOrNull(index + 1)
                        ?: throw IllegalArgumentException("Missing value for --$name.")
                    if (value.startsWith("--")) {
                        throw IllegalArgumentException("Missing value for --$name.")
                    }
                    index += 1
                    if (name == "region") {
                        region = when (value.lowercase()) {
                            "germany" -> PushRegion.GERMANY
                            "china" -> PushRegion.CHINA
                            else -> throw IllegalArgumentException(
                                "Unknown region '$value'. Use germany or china.",
                            )
                        }
                    } else if (name !in FLAGS) {
                        throw IllegalArgumentException("Unknown option --$name.")
                    } else {
                        values[name] = value
                    }
                }
                else -> throw IllegalArgumentException("Unexpected argument: $arg")
            }
            index += 1
        }
        return Options(values, region, validateOnly, help)
    }

    private companion object {
        const val USAGE = 2
        const val API = 1
        const val SUCCESS_CODE = "80000000"
        const val DEFAULT_CHANNEL = "channel_1"
        val APP_ID = Regex("\\d+")
        val TOPIC = Regex("[a-zA-Z0-9\\-_.~%]{1,900}")
        val FLAGS = setOf(
            "app-id",
            "app-secret",
            "push-token",
            "title",
            "body",
            "text",
            "channel",
            "topic",
        )
        val HELP = """
            Push Kit CLI for this sample. Credentials are flags or environment variables.
            Do not commit an app secret or push token.

            Commands:
              token    Request an OAuth access token
              notify   Send a notification message to one device token
              data     Send a data message the app displays itself
              topic    Send a notification, or a data message when --text is set, to a topic

            Options:
              --app-id        AppGallery Connect app id (or HUAWEI_APP_ID)
              --app-secret    App secret (or HUAWEI_APP_SECRET)
              --push-token    Device push token (or HUAWEI_PUSH_TOKEN)
              --topic         Topic name for the topic command
              --title         Notification or data title
              --body          Notification body
              --text          Data message text
              --channel       Data message channel id (default channel_1)
              --region        germany (default) or china
              --validate-only Ask Push Kit to validate the message without delivering it

            Examples:
              ./gradlew :push-cli:run --args="token --app-id 123 --app-secret SECRET"
              ./gradlew :push-cli:run --args="notify --app-id 123 --app-secret SECRET --push-token TOKEN --title Hi --body Hello"
              ./gradlew :push-cli:run --args="data --app-id 123 --app-secret SECRET --push-token TOKEN --title Hi --text Hello"
              ./gradlew :push-cli:run --args="topic --app-id 123 --app-secret SECRET --topic weather --title Hi --text Hello"

        """.trimIndent()
    }
}

fun main(args: Array<String>) {
    exitProcess(PushCli().run(args))
}
