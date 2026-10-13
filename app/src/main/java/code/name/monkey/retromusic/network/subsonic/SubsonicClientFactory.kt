package code.name.monkey.retromusic.network.subsonic

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SubsonicClientFactory {
    private const val API_VERSION = "1.16.1"
    private const val CLIENT_NAME = "RetroDrome"

    @Volatile
    private var cachedApi: SubsonicApi? = null
    @Volatile
    private var lastUrl: String? = null
    @Volatile
    private var lastUser: String? = null
    @Volatile
    private var lastPass: String? = null

    private val random = SecureRandom()

    fun generateSalt(length: Int = 8): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun buildStreamUrl(songId: String): String {
        val baseUrl = SubsonicPreferences.serverUrl
        if (baseUrl.isBlank()) return ""
        val salt = generateSalt()
        val token = md5Hex(SubsonicPreferences.password + salt)
        val user = SubsonicPreferences.username
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/rest/stream.view?id=$songId&u=$user&t=$token&s=$salt&v=$API_VERSION&c=$CLIENT_NAME"
    }

    fun buildCoverArtUrl(id: String, size: Int = 300): String {
        val baseUrl = SubsonicPreferences.serverUrl
        if (baseUrl.isBlank()) return ""
        val salt = generateSalt()
        val token = md5Hex(SubsonicPreferences.password + salt)
        val user = SubsonicPreferences.username
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/rest/getCoverArt.view?id=$id&size=$size&u=$user&t=$token&s=$salt&v=$API_VERSION&c=$CLIENT_NAME"
    }

    private class SubsonicAuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val password = SubsonicPreferences.password
            val salt = generateSalt()
            val token = md5Hex(password + salt)

            val urlWithParams = originalUrl.newBuilder()
                .addQueryParameter("u", SubsonicPreferences.username)
                .addQueryParameter("t", token)
                .addQueryParameter("s", salt)
                .addQueryParameter("v", API_VERSION)
                .addQueryParameter("c", CLIENT_NAME)
                .addQueryParameter("f", "json")
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(urlWithParams)
                .build()

            return try {
                chain.proceed(newRequest)
            } catch (e: SecurityException) {
                // Workaround for Android 16 EPERM on dns resolution or socket creation
                throw IOException("Network security error: ${e.message}", e)
            }
        }
    }

    @Synchronized
    fun getApi(): SubsonicApi? {
        val currentUrl = SubsonicPreferences.serverUrl
        val currentUser = SubsonicPreferences.username
        val currentPass = SubsonicPreferences.password

        if (currentUrl.isBlank()) return null

        if (cachedApi != null && currentUrl == lastUrl && currentUser == lastUser && currentPass == lastPass) {
            return cachedApi
        }

        val formattedBaseUrl = if (currentUrl.endsWith("/")) currentUrl else "$currentUrl/"

        val okHttpClient = createOkHttpClient()

        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl(formattedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(SubsonicApi::class.java)
            cachedApi = api
            lastUrl = currentUrl
            lastUser = currentUser
            lastPass = currentPass
            api
        } catch (e: Exception) {
            null
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(SubsonicAuthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            // Fall back to standard SSL verification if custom trust manager fails
        }

        return builder.build()
    }
}
