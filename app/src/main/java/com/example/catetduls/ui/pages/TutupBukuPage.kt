package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.BookClosing
import com.example.catetduls.utils.BookClosingReportGenerator
import com.example.catetduls.utils.CurrencyHelper
import com.example.catetduls.viewmodel.TutupBukuViewModel
import com.example.catetduls.viewmodel.TutupBukuViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TutupBukuPage : Fragment() {

        private lateinit var viewModel: TutupBukuViewModel

        private lateinit var tvLockIcon: TextView
        private lateinit var tvStatusText: TextView
        private lateinit var layoutVerification: LinearLayout
        private lateinit var cbTransactionsVerified: CheckBox
        private lateinit var cbBalanceMatch: CheckBox
        private lateinit var btnAction: MaterialButton
        private lateinit var btnExportPdf: MaterialButton
        private lateinit var btnExportCsv: MaterialButton
        private lateinit var cardInfo: MaterialCardView
        private lateinit var tvInfoText: TextView

        private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
        private var currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
        private var currentPeriodClosing: BookClosing? = null

        private val createPdfLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
                        ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                                result.data?.data?.let { uri ->
                                        exportReportToUri(uri, isPdf = true)
                                }
                        }
                }

        private val createCsvLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
                        ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                                result.data?.data?.let { uri ->
                                        exportReportToUri(uri, isPdf = false)
                                }
                        }
                }

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View? {
                return inflater.inflate(R.layout.fragment_tutup_buku, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                val factory = TutupBukuViewModelFactory(requireActivity().application)
                viewModel = ViewModelProvider(this, factory)[TutupBukuViewModel::class.java]

                initViews(view)
                setupListeners()
                observeData()

                // Initial load
                checkPeriodStatus()
                calculateFinalBalance()
        }

        private fun initViews(view: View) {
                tvLockIcon = view.findViewById(R.id.tv_lock_icon)
                tvStatusText = view.findViewById(R.id.tv_status_text)
                layoutVerification = view.findViewById(R.id.layout_verification)
                cbTransactionsVerified = view.findViewById(R.id.cb_transactions_verified)
                cbBalanceMatch = view.findViewById(R.id.cb_balance_match)
                btnAction = view.findViewById(R.id.btn_action)
                btnExportPdf = view.findViewById(R.id.btn_export_pdf)
                btnExportCsv = view.findViewById(R.id.btn_export_csv)
                cardInfo = view.findViewById(R.id.card_info)
                tvInfoText = view.findViewById(R.id.tv_info_text)
        }

        private fun setupListeners() {
                btnAction.setOnClickListener {
                        if (currentPeriodClosing == null) {
                                showCloseBookDialog()
                        } else {
                                showReopenBookDialog()
                        }
                }

                btnExportPdf.setOnClickListener {
                        val monthName =
                                SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                        .format(
                                                Calendar.getInstance()
                                                        .apply {
                                                                set(Calendar.YEAR, currentYear)
                                                                set(
                                                                        Calendar.MONTH,
                                                                        currentMonth - 1
                                                                )
                                                        }
                                                        .time
                                        )
                        val fileName = "Laporan_Keuangan_$monthName.pdf"

                        val intent =
                                android.content.Intent(
                                                android.content.Intent.ACTION_CREATE_DOCUMENT
                                        )
                                        .apply {
                                                addCategory(
                                                        android.content.Intent.CATEGORY_OPENABLE
                                                )
                                                type = "application/pdf"
                                                putExtra(
                                                        android.content.Intent.EXTRA_TITLE,
                                                        fileName
                                                )
                                        }
                        createPdfLauncher.launch(intent)
                }

                btnExportCsv.setOnClickListener {
                        val monthName =
                                SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                        .format(
                                                Calendar.getInstance()
                                                        .apply {
                                                                set(Calendar.YEAR, currentYear)
                                                                set(
                                                                        Calendar.MONTH,
                                                                        currentMonth - 1
                                                                )
                                                        }
                                                        .time
                                        )
                        val fileName = "Laporan_Keuangan_$monthName.csv"

                        val intent =
                                android.content.Intent(
                                                android.content.Intent.ACTION_CREATE_DOCUMENT
                                        )
                                        .apply {
                                                addCategory(
                                                        android.content.Intent.CATEGORY_OPENABLE
                                                )
                                                type = "text/csv"
                                                putExtra(
                                                        android.content.Intent.EXTRA_TITLE,
                                                        fileName
                                                )
                                        }
                        createCsvLauncher.launch(intent)
                }
        }

        private fun exportReportToUri(uri: android.net.Uri, isPdf: Boolean) {
                viewModel.getTransactionsForPeriod(currentYear, currentMonth) {
                        transactions,
                        categoryMap ->
                        lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                        requireContext()
                                                .contentResolver
                                                .openOutputStream(uri)
                                                ?.use { outputStream ->
                                                        val monthName =
                                                                SimpleDateFormat(
                                                                                "MMMM yyyy",
                                                                                Locale("id", "ID")
                                                                        )
                                                                        .format(
                                                                                Calendar.getInstance()
                                                                                        .apply {
                                                                                                set(
                                                                                                        Calendar.YEAR,
                                                                                                        currentYear
                                                                                                )
                                                                                                set(
                                                                                                        Calendar.MONTH,
                                                                                                        currentMonth -
                                                                                                                1
                                                                                                )
                                                                                        }
                                                                                        .time
                                                                        )

                                                        if (isPdf) {
                                                                BookClosingReportGenerator
                                                                        .generatePdf(
                                                                                requireContext(),
                                                                                transactions,
                                                                                categoryMap,
                                                                                monthName,
                                                                                outputStream
                                                                        )
                                                        } else {
                                                                val csvContent =
                                                                        BookClosingReportGenerator
                                                                                .generateCsv(
                                                                                        transactions,
                                                                                        categoryMap,
                                                                                        monthName
                                                                                )
                                                                outputStream.write(
                                                                        csvContent.toByteArray()
                                                                )
                                                        }
                                                }
                                        withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                                requireContext(),
                                                                "Berhasil mengekspor Laporan!",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                        }
                                } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                                requireContext(),
                                                                "Gagal mengekspor: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                        }
                                }
                        }
                }
        }

        private fun observeData() {
                viewModel.currentPeriodClosed.observe(viewLifecycleOwner) { closing ->
                        currentPeriodClosing = closing
                        updateUIBasedOnStatus(closing)
                }

                viewModel.error.observe(viewLifecycleOwner) { error ->
                        error?.let {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                                viewModel.clearError()
                        }
                }

                viewModel.success.observe(viewLifecycleOwner) { success ->
                        if (success) {
                                Toast.makeText(requireContext(), "Berhasil!", Toast.LENGTH_SHORT)
                                        .show()
                                viewModel.clearSuccess()
                                checkPeriodStatus()
                        }
                }
        }

        // Exposed method for parent to call
        fun updateDate(year: Int, month: Int) {
                currentYear = year
                currentMonth = month
                checkPeriodStatus()
                calculateFinalBalance()
        }

        private fun checkPeriodStatus() {
                viewModel.checkPeriodStatus(currentYear, currentMonth)
        }

        private fun calculateFinalBalance() {
                val calendar = Calendar.getInstance()
                calendar.set(currentYear, currentMonth - 1, 1, 0, 0, 0)
                val periodStart = calendar.timeInMillis

                calendar.set(
                        Calendar.DAY_OF_MONTH,
                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val periodEnd = calendar.timeInMillis

                viewModel.calculateFinalBalance(periodStart, periodEnd)
        }

        private fun updateUIBasedOnStatus(closing: BookClosing?) {
                val monthName =
                        SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                .format(
                                        Calendar.getInstance()
                                                .apply {
                                                        set(Calendar.YEAR, currentYear)
                                                        set(Calendar.MONTH, currentMonth - 1)
                                                }
                                                .time
                                )

                if (closing == null) {
                        // Period is OPEN
                        tvLockIcon.text = "🔓"
                        tvStatusText.text =
                                "Status periode $monthName masih terbuka. Transaksi masih dapat ditambah atau diubah."
                        layoutVerification.visibility = View.VISIBLE
                        btnAction.text = "🔒 Tutup Buku Sekarang"
                        btnAction.backgroundTintList =
                                resources.getColorStateList(R.color.primary, null)
                        tvInfoText.text =
                                "Menutup buku akan mengunci seluruh transaksi pada periode ini. Anda tidak dapat menambah atau mengubah transaksi setelah periode ditutup."
                        tvInfoText.setTextColor(resources.getColor(android.R.color.white, null))
                        cardInfo.setCardBackgroundColor(resources.getColor(R.color.primary, null))
                } else {
                        // Period is CLOSED
                        tvLockIcon.text = "🔒"
                        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
                        tvStatusText.text =
                                "Periode tertutup pada ${dateFormat.format(Date(closing.closedAt))}. Transaksi tidak dapat diubah."
                        layoutVerification.visibility = View.GONE
                        btnAction.text = "🔓 Buka Kembali Periode"
                        btnAction.backgroundTintList =
                                resources.getColorStateList(R.color.warning, null)
                        tvInfoText.text =
                                "⚠️ Periode yang sudah ditutup sebaiknya tidak dibuka kembali kecuali untuk koreksi penting. Pastikan untuk menutup kembali setelah selesai."
                        tvInfoText.setTextColor(resources.getColor(android.R.color.white, null))
                        cardInfo.setCardBackgroundColor(resources.getColor(R.color.warning, null))
                }
        }

        private fun showCloseBookDialog() {
                val dialogView =
                        LayoutInflater.from(requireContext())
                                .inflate(R.layout.dialog_close_book_confirm, null)

                val tvMessage = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
                val tvFinalBalanceValue =
                        dialogView.findViewById<TextView>(R.id.tv_final_balance_value)

                val monthName =
                        SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                .format(
                                        Calendar.getInstance()
                                                .apply {
                                                        set(Calendar.YEAR, currentYear)
                                                        set(Calendar.MONTH, currentMonth - 1)
                                                }
                                                .time
                                )

                tvMessage.text =
                        "Apakah Anda yakin ingin menutup buku untuk periode $monthName?\n\nSetelah ditutup, semua transaksi pada periode ini tidak dapat diubah atau dihapus."

                val finalBalance = viewModel.finalBalance.value ?: 0.0
                tvFinalBalanceValue.text = CurrencyHelper.format(finalBalance, "Rp")

                val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

                dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                        dialog.dismiss()
                }

                dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                        // Calculate period timestamps
                        val calendar = Calendar.getInstance()
                        calendar.set(currentYear, currentMonth - 1, 1, 0, 0, 0)
                        val periodStart = calendar.timeInMillis

                        calendar.set(
                                Calendar.DAY_OF_MONTH,
                                calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        )
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        val periodEnd = calendar.timeInMillis

                        val isVerified =
                                cbTransactionsVerified.isChecked && cbBalanceMatch.isChecked

                        viewModel.closeBook(
                                periodStart = periodStart,
                                periodEnd = periodEnd,
                                periodLabel = monthName,
                                finalBalance = finalBalance,
                                isVerified = isVerified
                        )

                        dialog.dismiss()
                }

                dialog.show()
        }

        private fun showReopenBookDialog() {
                val dialogView =
                        LayoutInflater.from(requireContext())
                                .inflate(R.layout.dialog_reopen_book_confirm, null)

                val tvMessage = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
                val monthName =
                        SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                .format(
                                        Calendar.getInstance()
                                                .apply {
                                                        set(Calendar.YEAR, currentYear)
                                                        set(Calendar.MONTH, currentMonth - 1)
                                                }
                                                .time
                                )

                tvMessage.text =
                        "Apakah Anda yakin ingin membuka kembali periode $monthName?\n\nSetelah dibuka, transaksi dapat diedit dan dihapus kembali.\n\nPastikan untuk menutup kembali periode setelah selesai melakukan koreksi."

                val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

                dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                        dialog.dismiss()
                }

                dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                        currentPeriodClosing?.let { closing -> viewModel.reopenBook(closing.id) }
                        dialog.dismiss()
                }

                dialog.show()
        }
}
