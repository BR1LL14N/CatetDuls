package com.example.catetduls.utils

import java.text.NumberFormat
import java.util.Locale
import java.math.BigDecimal

object Formatters {

    /**
     * Mengubah angka (Double/Int/Long) menjadi format Rupiah yang rapi.
     * Contoh: 1000000 -> "Rp 1.000.000"
     * Menangani notasi ilmiah (E9) secara otomatis.
     */
    fun toRupiah(amount: Double): String {
        // 1. Gunakan BigDecimal untuk mencegah notasi ilmiah (1.0E9)
        // Kita konversi dulu ke BigDecimal, lalu ambil doubleValue-nya untuk formatter
        // atau biarkan formatter menangani double, tapi setting maximumFractionDigits = 0

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0 // Hilangkan ,00 di belakang

        // replace("Rp", "Rp ") memberi spasi setelah Rp agar tidak terlalu mepet
        return format.format(amount).replace("Rp", "Rp ")
    }

    /**
     * Versi overload untuk menerima BigDecimal langsung (jika diperlukan nanti)
     */
    fun toRupiah(amount: BigDecimal): String {
        return toRupiah(amount.toDouble())
    }
}
