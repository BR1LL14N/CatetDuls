package com.example.catetduls.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyHelper {

    data class CurrencyModel(val code: String, val symbol: String, val name: String) {
        override fun toString(): String {
            return "$code - $name"
        }
    }

    fun getAvailableCurrencies(): List<CurrencyModel> {
        val currencies = mutableListOf<CurrencyModel>()

        // Prioritize IDR
        currencies.add(CurrencyModel("IDR", "Rp", "Indonesian Rupiah"))

        // Add other common currencies
        val commonCodes =
                listOf("USD", "EUR", "GBP", "JPY", "AUD", "SGD", "MYR", "CNY", "KRW", "INR", "THB")

        for (code in commonCodes) {
            try {
                val currency = java.util.Currency.getInstance(code)
                val symbol = currency.symbol
                val name = currency.displayName
                currencies.add(CurrencyModel(code, symbol, name))
            } catch (e: Exception) {
                // Ignore if currency not found
            }
        }

        // You can uncomment this to load ALL available currencies (might be too long)
        /*
        val allCurrencies = java.util.Currency.getAvailableCurrencies()
        val sortedList = allCurrencies.sortedBy { it.currencyCode }
        for (currency in sortedList) {
             if (!commonCodes.contains(currency.currencyCode) && currency.currencyCode != "IDR") {
                 currencies.add(CurrencyModel(currency.currencyCode, currency.symbol, currency.displayName))
             }
        }
        */

        return currencies
    }

    fun format(amount: Double, symbol: String = "Rp"): String {
        // Gunakan Locale Indonesia untuk format angka (titik sebagai ribuan)
        val localeID = Locale("id", "ID")
        val numberFormat = NumberFormat.getInstance(localeID)

        return "$symbol ${numberFormat.format(amount)}"
    }
}
