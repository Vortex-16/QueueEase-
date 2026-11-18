package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun callGeminiGrounded(
        userQuery: String,
        groundedContext: String,
        role: com.example.data.model.UserRole = com.example.data.model.UserRole.CUSTOMER
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        val roleInstruction = when (role) {
            com.example.data.model.UserRole.CUSTOMER -> """
                You are QEase Service Intelligence Assistant for public citizens.
                Help citizens understand required documents, official fees, eligibility, wait times, and counter steps.
            """.trimIndent()
            com.example.data.model.UserRole.STAFF -> """
                You are QEase Counter Desk AI Copilot for a counter staff officer.
                Help the officer draft compliant service or required-document change proposals for Admin review, verify queue/crowd pressure at their locked counter, and look up counter SOP rules.
                Keep suggestions actionable so the officer can submit a clean proposal or verify field crowd reports immediately.
            """.trimIndent()
            com.example.data.model.UserRole.ADMIN -> """
                You are QEase Governance & Audit AI Assistant for a system administrator.
                Audit pending Staff proposals for policy/risk consistency, identify bottleneck centres or high queue wait loads from live metrics, and recommend governance actions (approvals, counter staffing, live notices, or maintenance controls).
            """.trimIndent()
        }

        try {
            val systemPrompt = """
                $roleInstruction
                CRITICAL RULES:
                1. Answer ONLY based on the verified grounded context and live operational snapshot provided below.
                2. If the exact fact is missing from the context, state clearly what is verified and note any gap instead of inventing rules or fees.
                3. Format output cleanly using short section labels (e.g. "Summary:", "Recommended Action:", "Required Documents:", "Audit Findings:") and "- " hyphen bullets. Never output raw markdown asterisks (* or **).
                
                GROUNDED VERIFIED CONTEXT:
                $groundedContext
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userQuery))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                )
            )

            val response = service.generateContent(apiKey, request)
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!replyText.isNullOrBlank()) {
                Result.success(replyText)
            } else {
                Result.failure(IllegalStateException("Empty response from AI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
