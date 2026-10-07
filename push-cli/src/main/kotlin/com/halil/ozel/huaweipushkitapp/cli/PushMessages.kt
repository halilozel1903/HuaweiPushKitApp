package com.halil.ozel.huaweipushkitapp.cli

/**
 * Hosts from the Push Kit server API. Germany matches the sample's data storage
 * location. China matches the Postman collection in the README.
 */
enum class PushRegion(
    val tokenUrl: String,
    val pushHost: String,
) {
    GERMANY(
        tokenUrl = "https://oauth-login.cloud.huawei.eu/oauth2/v3/token",
        pushHost = "https://push-api.cloud.huawei.eu",
    ),
    CHINA(
        tokenUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/token",
        pushHost = "https://push-api.cloud.huawei.com",
    ),
    ;

    fun sendUrl(appId: String): String = "$pushHost/v1/$appId/messages:send"
}

fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char.code < 0x20) {
                append("\\u")
                append(char.code.toString(16).padStart(4, '0'))
            } else {
                append(char)
            }
        }
    }
    append('"')
}

fun notificationBody(
    title: String,
    body: String,
    pushToken: String,
    validateOnly: Boolean,
): String {
    return """
        {
          "validate_only": $validateOnly,
          "message": {
            "notification": {
              "title": ${jsonString(title)},
              "body": ${jsonString(body)}
            },
            "android": {
              "notification": {
                "click_action": {
                  "type": 3
                }
              }
            },
            "token": [${jsonString(pushToken)}]
          }
        }
    """.trimIndent()
}

/** Data keys match [com.halil.ozel.huaweipushkitapp.HuaweiPushService]. */
fun dataMessageBody(
    title: String,
    text: String,
    channelId: String,
    pushToken: String,
    validateOnly: Boolean,
): String {
    val data = """{"title":${jsonString(title)},"text":${jsonString(text)},"channel_id":${jsonString(channelId)}}"""
    return """
        {
          "validate_only": $validateOnly,
          "message": {
            "data": ${jsonString(data)},
            "token": [${jsonString(pushToken)}]
          }
        }
    """.trimIndent()
}

fun topicNotificationBody(
    title: String,
    body: String,
    topic: String,
    validateOnly: Boolean,
): String {
    return """
        {
          "validate_only": $validateOnly,
          "message": {
            "notification": {
              "title": ${jsonString(title)},
              "body": ${jsonString(body)}
            },
            "android": {
              "notification": {
                "click_action": {
                  "type": 3
                }
              }
            },
            "topic": ${jsonString(topic)}
          }
        }
    """.trimIndent()
}

fun topicDataBody(
    title: String,
    text: String,
    channelId: String,
    topic: String,
    validateOnly: Boolean,
): String {
    val data = """{"title":${jsonString(title)},"text":${jsonString(text)},"channel_id":${jsonString(channelId)}}"""
    return """
        {
          "validate_only": $validateOnly,
          "message": {
            "data": ${jsonString(data)},
            "topic": ${jsonString(topic)}
          }
        }
    """.trimIndent()
}

fun extractJsonString(json: String, field: String): String? {
    val pattern = Regex(""""${Regex.escape(field)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
    val raw = pattern.find(json)?.groupValues?.get(1) ?: return null
    return raw.replace("\\\"", "\"").replace("\\\\", "\\")
}
