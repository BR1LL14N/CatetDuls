package com.example.catetduls.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyHelper {

    data class CurrencyModel(val code: String, val symbol: String, val name: String) {
        override fun toString(): String {
            return "$code - $name"
        }
    }

    // Static exchange rates relative to IDR (Base = IDR)
    // 1 Unit of Currency = X IDR
    private val exchangeRates =
            mapOf(
                    "IDR" to 1.0,
                    "USD" to 15500.0,
                    "EUR" to 16800.0,
                    "GBP" to 19500.0,
                    "JPY" to 105.0,
                    "AUD" to 10000.0,
                    "SGD" to 11500.0,
                    "MYR" to 3300.0,
                    "CNY" to 2150.0,
                    "KRW" to 12.0,
                    "INR" to 185.0,
                    "THB" to 430.0
            )

    fun getConversionFactor(fromCode: String, toCode: String): Double {
        val fromRate = exchangeRates[fromCode] ?: 1.0 // Default to 1.0 (treat as IDR) if unknown
        val toRate = exchangeRates[toCode] ?: 1.0

        // Formula: Amount * (RateOld / RateNew)
        return fromRate / toRate
    }

    /** Mengkonversi nilai IDR ke mata uang target. Contoh: 15500 IDR -> 1 USD */
    fun convertIdrTo(amountIdr: Double, targetCode: String): Double {
        if (targetCode == "IDR") return amountIdr
        val rate = exchangeRates[targetCode] ?: 1.0
        // IDR to USD = IDR / Rate
        // 15500 / 15500 = 1
        return amountIdr / rate
    }

    /** Mengkonversi nilai mata uang asing ke IDR. Contoh: 1 USD -> 15500 IDR */
    fun convertToIdr(amountTarget: Double, sourceCode: String): Double {
        if (sourceCode == "IDR") return amountTarget
        val rate = exchangeRates[sourceCode] ?: 1.0
        // USD to IDR = USD * Rate
        return amountTarget * rate
    }

    fun getAvailableCurrencies(): List<CurrencyModel> {
        val currencies = mutableListOf<CurrencyModel>()

        // Prioritize IDR
        currencies.add(CurrencyModel("IDR", "Rp", "Indonesian Rupiah"))

        // Add other common currencies from our supported list
        val supportedCodes = exchangeRates.keys.filter { it != "IDR" }

        for (code in supportedCodes) {
            try {
                val currency = java.util.Currency.getInstance(code)
                val symbol = currency.symbol
                val name = currency.displayName
                currencies.add(CurrencyModel(code, symbol, name))
            } catch (e: Exception) {
                // Determine symbol manually if system lookup fails for some reason, or ignore
                // currencies.add(CurrencyModel(code, code, code))
            }
        }

        return currencies
    }

    fun format(amount: Double, symbol: String = "Rp"): String {
        // Gunakan Locale Indonesia untuk format angka (titik sebagai ribuan)
        val localeID = Locale("id", "ID")
        val numberFormat = NumberFormat.getInstance(localeID)
        numberFormat.maximumFractionDigits = 2 // Allow cents for foreign currencies

        return "$symbol ${numberFormat.format(amount)}"
    }
}
