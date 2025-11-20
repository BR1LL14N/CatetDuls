package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.DailySummary
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

// Kelas pembantu untuk menampung data yang akan ditampilkan di grid
data class CalendarDayCell(
    val dayOfMonth: Int,
    val summary: DailySummary?,
    val isCurrentMonth: Boolean = true,
    val isToday: Boolean = false
)

// Adapter yang menerima List<CalendarDayCell?>
class CalendarAdapter(initialDays: List<CalendarDayCell?>) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    // PROPERTI DIPERBAIKI: Mendeklarasikan data sebagai Mutable List dari awal
    private val calendarDays: MutableList<CalendarDayCell?> = initialDays.toMutableList()

    /**
     * Memperbarui daftar data kalender dengan List baru.
     * Mengganti seluruh konten list internal.
     */
    fun submitList(newDays: List<CalendarDayCell?>) {
        // --- Perbaikan: Tidak lagi menggunakan 'as MutableList' yang menyebabkan ClassCastException ---
        calendarDays.clear()
        calendarDays.addAll(newDays)
        notifyDataSetChanged()
    }

    override fun getItemCount() = calendarDays.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        // Akses item langsung dari list internal
        val item = calendarDays[position]
        holder.bind(item)
    }

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val tvDayIncome: TextView = itemView.findViewById(R.id.tv_day_income)
        private val tvDayExpense: TextView = itemView.findViewById(R.id.tv_day_expense)
        private val tvDayTotal: TextView = itemView.findViewById(R.id.tv_day_total)
        private val viewDayMarker: View = itemView.findViewById(R.id.view_day_marker)

        private val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        fun bind(cell: CalendarDayCell?) {
            val context = itemView.context

            // Reset visibility
            tvDayIncome.visibility = View.GONE
            tvDayExpense.visibility = View.GONE
            tvDayTotal.visibility = View.GONE
            viewDayMarker.visibility = View.GONE
            tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))

            // Logika untuk Cell Kosong (Padding)
            if (cell == null) {
                tvDayNumber.text = ""
                return
            }

            // Atur Nomor Hari
            tvDayNumber.text = cell.dayOfMonth.toString()

            // Tandai Hari Ini
            if (cell.isToday) {
                viewDayMarker.visibility = View.VISIBLE
                tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
            }

            val summary = cell.summary
            if (summary != null) {
                val income = summary.totalIncome
                val expense = summary.totalExpense
                val total = income - expense

                // Pemasukan
                if (income > 0) {
                    tvDayIncome.text = currencyFormat.format(income)
                    tvDayIncome.visibility = View.VISIBLE
                }

                // Pengeluaran
                if (expense > 0) {
                    tvDayExpense.text = currencyFormat.format(expense)
                    tvDayExpense.visibility = View.VISIBLE
                }

                // Total/Saldo
                if (total != 0.0) {
                    val totalSign = if (total >= 0) "" else "-"
                    tvDayTotal.text = "$totalSign${currencyFormat.format(abs(total))}"
                    tvDayTotal.visibility = View.VISIBLE

                    val totalColor = if (total >= 0) R.color.success else R.color.danger
                    tvDayTotal.setTextColor(ContextCompat.getColor(context, totalColor))
                }
            }
        }
    }
}