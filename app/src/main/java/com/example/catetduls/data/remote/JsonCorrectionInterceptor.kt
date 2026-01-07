package com.example.catetduls.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class JsonCorrectionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val body = response.body ?: return response
        val rawJson = body.string()

        var fixedJson = rawJson

        // Fix 1: Missing Colon between key and numeric/boolean/null value
        val missingColonRegex = Regex("\"(\\w+)\"\\s*(-?\\d+(\\.\\d+)?|true|false|null)")
        fixedJson =
                missingColonRegex.replace(fixedJson) { matchResult ->
                    val key = matchResult.groupValues[1]
                    val value = matchResult.groupValues[2]
                    "\"$key\":$value"
                }

        // Fix 2: Missing Comma between values and next key
        val missingCommaRegex =
                Regex("(-?\\d+(\\.\\d+)?|true|false|null|\"[^\"]*\")\\s*(\"\\w+\":)")
        fixedJson =
                missingCommaRegex.replace(fixedJson) { matchResult ->
                    val valuePart = matchResult.groupValues[1]
                    val nextKeyPart = matchResult.groupValues[3]
                    "$valuePart,$nextKeyPart"
                }

        // Fix 3: Missing Opening Quote for Key
        val missingQuoteRegex = Regex("([{},])\\s*(\\w+)\":")
        fixedJson =
                missingQuoteRegex.replace(fixedJson) { matchResult ->
                    val separator = matchResult.groupValues[1]
                    val key = matchResult.groupValues[2]
                    "$separator\"$key\":"
                }

        // Fix 4: Truncated JSON (Missing closing brace)
        val trimmed = fixedJson.trim()
        if (trimmed.startsWith("{") && !trimmed.endsWith("}")) {
            fixedJson = trimmed + "}"
            Log.w("JsonCorrection", "⚠️ Appended missing root closing brace '}'")
        }

        if (fixedJson != rawJson) {
            Log.w("JsonCorrection", "⚠️ JSON Malformation detected and fixed!")
        }

        return response.newBuilder().body(fixedJson.toResponseBody(body.contentType())).build()
    }
}
