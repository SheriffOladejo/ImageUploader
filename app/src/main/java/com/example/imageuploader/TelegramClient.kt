import kotlin.math.min
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class TelegramClient(private val chatId: String) {
    private val botToken = "6484085142:AAFGeLB2kDAaH8_LP2g_o1WM6NP-mO31RM8" // Replace with your actual bot token
    private val client = OkHttpClient()

    // Text of the message to be sent, 1-4096 characters after entities parsing
    private fun limitMessage(message: String): String {
        return message.substring(0, min(4096, message.length))
    }

    fun sendMessage(message: String) {
        val queryParams = mapOf(
            "chat_id" to chatId,
            "text" to limitMessage(message),
            "parse_mode" to "html"
        )

        val httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("api.telegram.org")
            .addPathSegment("bot$botToken/sendMessage")

        for ((key, value) in queryParams) {
            httpUrl.addQueryParameter(key, value)
        }

        val request = Request.Builder()
            .url(httpUrl.build())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Telegram client onFailure: ${e.toString()}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Telegram client onResponse: ${responseBody.toString()}")
                } else {
                    println("Telegram client onResponse error: ${response.code} ${response.body?.string()} ${response.message}")
                }
            }
        })
    }

    fun sendPhoto(photo: String) {
        val url = "https://api.telegram.org/bot$botToken/sendPhoto"

        val requestBody = MultipartBody.Builder("boundary")
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("parse_mode", "html")
            .addFormDataPart("photo", photo)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Telegram client onFailure: ${e.toString()}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Telegram client onResponse: ${responseBody.toString()}")
                } else {
                    println("Telegram client onResponse error: ${response.code} ${response.body?.string()} ${response.message}")
                }
            }
        })
    }
}
