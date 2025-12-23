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

        // Log warning if we suspect malformation might be present (optional, but good for debug)
        // We will just apply fixes aggressively or only if needed.
        // Given the randomness, better to just apply regex fixes on the string.

        var fixedJson = rawJson

        // Fix 1: Missing Colon between key and numeric/boolean/null value
        // Pattern: "key"123 -> "key":123
        // We look for "key" followed by whitespace (optional) then a digit, true, false, or null
        // We explicitly exclude cases where there is already a colon
        // Regex: "(\w+)"\s*(?=[0-9]|true|false|null) -> This is hard to do with lookahead for
        // replacement
        // Better: Group 1: "key", Group 2: value. Match strict lack of colon.
        // Regex: ("\w+")\s+((?:-?\d+(?:\.\d+)?|true|false|null)\b)
        // Wait, \s+ requires space. "key"123 has no space.
        // Regex: ("\w+")((?:-?\d+(?:\.\d+)?|true|false|null)\b)
        // This effectively matches "key"123.

        // Let's use a simpler regex loop or ReplaceAll
        // Fix Missing Colon: "some_key"0 -> "some_key":0
        // We match: Quote, Word, Quote, Optional Space, Digit
        val missingColonRegex = Regex("\"(\\w+)\"\\s*(-?\\d+(\\.\\d+)?|true|false|null)")
        fixedJson =
                missingColonRegex.replace(fixedJson) { matchResult ->
                    // Use the groups. If original was "key"123, we want "key":123
                    // Group 1: key (without quotes? No, regex includes quotes? No, parens are
                    // inside quotes in my regex above? No, parens are around word)
                    // My Regex: "\"(\\w+)\"..." -> Group 1 is just the word.
                    // Match text: "key"123
                    // We want: "key":123
                    val key = matchResult.groupValues[1]
                    val value = matchResult.groupValues[2]
                    "\"$key\":$value"
                }

        // Fix 2: Missing Comma between values and next key
        // Pattern: 123"next_key": -> 123,"next_key":
        // Pattern: "value""next_key": -> "value","next_key":
        // Matches: (Value)(Optional Space)("NextKey":)
        // Value can be: Number, boolean, null, or String (with quotes)
        // Regex for Value: (-?\d+(\.\d+)?|true|false|null|"[^"]*")
        // Regex for Next Key: ("\w+":)
        // Regex Combined: (-?\d+(\.\d+)?|true|false|null|"[^"]*")\s*("\w+":)
        val missingCommaRegex =
                Regex("(-?\\d+(\\.\\d+)?|true|false|null|\"[^\"]*\")\\s*(\"\\w+\":)")
        fixedJson =
                missingCommaRegex.replace(fixedJson) { matchResult ->
                    val valuePart = matchResult.groupValues[1]
                    val nextKeyPart =
                            matchResult.groupValues[
                                    3] // Group 2 is nested in Group 1 (decimal part) if strictly
                    // following parens.
                    // Actually Regex group index depends on open parens.
                    // 1: Value
                    // 2: (.Decimal) (optional)
                    // 3: NextKey
                    "$valuePart,$nextKeyPart"
                }

        // Fix 3: Missing Opening Quote for Key
        // Pattern: ,key":value or {key":value
        // This causes Gson to parse "key" as unquoted key, then see '"' and expect ':' -> Error!
        // Regex: ([{,])\s*(\w+)\":
        // Group 1: separator ({ or ,)
        // Group 2: key name
        // We replace with $1"$2":
        val missingQuoteRegex = Regex("([{},])\\s*(\\w+)\":")
        fixedJson =
                missingQuoteRegex.replace(fixedJson) { matchResult ->
                    val separator = matchResult.groupValues[1]
                    val key = matchResult.groupValues[2]
                    "$separator\"$key\":"
                }

        // Fix 4: Truncated JSON (Missing closing brace)
        // The server response seems to occasionally drop the final '}'
        // Example: {"success":true,"data":[...]} -> {"success":true,"data":[...]
        val trimmed = fixedJson.trim()
        if (trimmed.startsWith("{") && !trimmed.endsWith("}")) {
            // If it ends with ']', it likely just missed the root '}'
            fixedJson = trimmed + "}"
            Log.w("JsonCorrection", "⚠️ Appended missing root closing brace '}'")
        }

        if (fixedJson != rawJson) {
            Log.w("JsonCorrection", "⚠️ JSON Malformation detected and fixed!")
            // Log.d("JsonCorrection", "Original: $rawJson")
            // Log.d("JsonCorrection", "Fixed: $fixedJson")
        }

        return response.newBuilder().body(fixedJson.toResponseBody(body.contentType())).build()
    }
}
