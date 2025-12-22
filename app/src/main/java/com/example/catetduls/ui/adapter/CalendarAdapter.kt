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
    val isToday: Boolean = false,
    val timestamp: Long = 0L // Store the actual date timestamp
)

// Adapter yang menerima List<CalendarDayCell?>
class CalendarAdapter(
    initialDays: List<CalendarDayCell?>,
    private val onDayClick: (Long) -> Unit,
    private var currencyCode: String = "IDR",
    private var currencySymbol: String = "Rp"
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

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
    
    fun updateCurrency(code: String, symbol: String) {
        this.currencyCode = code
        this.currencySymbol = symbol
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
        holder.bind(item, onDayClick, currencyCode, currencySymbol)
    }

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val tvDayIncome: TextView = itemView.findViewById(R.id.tv_day_income)
        private val tvDayExpense: TextView = itemView.findViewById(R.id.tv_day_expense)
        private val tvDayTotal: TextView = itemView.findViewById(R.id.tv_day_total)
        private val viewDayMarker: View = itemView.findViewById(R.id.view_day_marker)

        fun bind(cell: CalendarDayCell?, onDayClick: (Long) -> Unit, currencyCode: String, currencySymbol: String) {
            val context = itemView.context

            // Reset visibility & Style
            tvDayIncome.visibility = View.GONE
            tvDayExpense.visibility = View.GONE
            tvDayTotal.visibility = View.GONE
            viewDayMarker.visibility = View.GONE
            tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            itemView.alpha = 1.0f // Reset alpha
            itemView.setOnClickListener(null) // Reset listener

            // Logika untuk Cell Kosong (Padding)
            // Note: With the new logic, cell ideally shouldn't be null often if we fill everything,
            // but we keep null safety just in case.
            if (cell == null) {
                tvDayNumber.text = ""
                return
            }

            // Atur Nomor Hari
            tvDayNumber.text = cell.dayOfMonth.toString()

            // Styling untuk Bukan Bulan Saat Ini
            if (!cell.isCurrentMonth) {
                // Dim content only, keep border (background) opaque
                tvDayNumber.alpha = 0.5f
                tvDayIncome.alpha = 0.5f
                tvDayExpense.alpha = 0.5f
                tvDayTotal.alpha = 0.5f
            } else {
                tvDayNumber.alpha = 1.0f
                tvDayIncome.alpha = 1.0f
                tvDayExpense.alpha = 1.0f
                tvDayTotal.alpha = 1.0f
            }

            // Tandai Hari Ini
            if (cell.isToday) {
                viewDayMarker.visibility = View.VISIBLE
                tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
            }

            // Click Listener
            itemView.setOnClickListener {
                onDayClick(cell.timestamp)
            }

            val summary = cell.summary
            if (summary != null) {
                val income = summary.totalIncome
                val expense = summary.totalExpense
                val total = income - expense
                
                // Convert Values
                val convertedIncome = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(income, currencyCode)
                val convertedExpense = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(expense, currencyCode)
                val convertedTotal = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(total, currencyCode)

                // Pemasukan
                if (income > 0) {
                    tvDayIncome.text = com.example.catetduls.utils.CurrencyHelper.format(convertedIncome, currencySymbol)
                    tvDayIncome.visibility = View.VISIBLE
                }

                // Pengeluaran
                if (expense > 0) {
                    tvDayExpense.text = com.example.catetduls.utils.CurrencyHelper.format(convertedExpense, currencySymbol)
                    tvDayExpense.visibility = View.VISIBLE
                }

                // Total/Saldo
                if (total != 0.0) {
                     // Use helper format directly, handles negative sign too
                    tvDayTotal.text = com.example.catetduls.utils.CurrencyHelper.format(abs(convertedTotal), currencySymbol)
                    if (total < 0) {
                        tvDayTotal.text = "-" + tvDayTotal.text
                    }
                    
                    tvDayTotal.visibility = View.VISIBLE

                    val totalColor = if (total >= 0) R.color.success else R.color.danger
                    tvDayTotal.setTextColor(ContextCompat.getColor(context, totalColor))
                }
            }
        }
    }
}