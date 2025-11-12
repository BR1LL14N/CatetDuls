package com.example.catetduls.ui.pages

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.TambahViewModel
import com.example.catetduls.viewmodel.TambahViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.Lifecycle

/**
 * TambahTransaksiPage - Form untuk tambah/edit transaksi
 *
 * Fitur:
 * - Radio button (Pemasukan/Pengeluaran)
 * - Spinner kategori (dinamis berdasarkan tipe)
 * - Input amount dengan format Rupiah
 * - Date picker
 * - Notes (opsional)
 * - Validasi
 */
class TambahTransaksiPage : Fragment() {

    private lateinit var viewModel: TambahViewModel

    // Views
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioPemasukan: RadioButton
    private lateinit var radioPengeluaran: RadioButton
    private lateinit var spinnerKategori: Spinner
    private lateinit var etAmount: TextInputEditText
    private lateinit var tvDate: TextView
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var categoriesList: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val factory = TambahViewModelFactory(transactionRepo, categoryRepo)
        viewModel = ViewModelProvider(this, factory)[TambahViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup
        setupRadioGroup()
        setupDatePicker()
        setupButtons()

        // Observe data
        observeData()
    }

    private fun initViews(view: View) {
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioPemasukan = view.findViewById(R.id.radio_pemasukan)
        radioPengeluaran = view.findViewById(R.id.radio_pengeluaran)
        spinnerKategori = view.findViewById(R.id.spinner_kategori)
        etAmount = view.findViewById(R.id.et_amount)
        tvDate = view.findViewById(R.id.tv_date)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        etNotes = view.findViewById(R.id.et_notes)
        btnSimpan = view.findViewById(R.id.btn_simpan)
        btnReset = view.findViewById(R.id.btn_reset)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun setupRadioGroup() {
        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_pemasukan -> viewModel.setType("Pemasukan")
                R.id.radio_pengeluaran -> viewModel.setType("Pengeluaran")
            }
        }

        // Set default
        radioPengeluaran.isChecked = true
    }

    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    viewModel.setDate(calendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        btnSimpan.setOnClickListener {
            // Get values from inputs
            val amount = etAmount.text.toString()
            val notes = etNotes.text.toString()

            // Set to ViewModel
            viewModel.setAmount(amount)
            viewModel.setNotes(notes)

            // Save
            if (viewModel.isFormValid()) {
                viewModel.saveTransaction()
            } else {
                Toast.makeText(requireContext(), "Form belum lengkap", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            viewModel.resetForm()
            clearInputs()
        }
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK STATEFLOW (di dalam lifecycleScope)
        // ===================================================
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe tanggal
                launch {
                    viewModel.date.collect { timestamp ->
                        tvDate.text = viewModel.formatDate(timestamp)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isSaving.collect { isSaving ->
                        progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
                        btnSimpan.isEnabled = !isSaving
                    }
                }

                // Observe success message
                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessages()
                            clearInputs()
                        }
                    }
                }

                // Observe error message
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessages()
                        }
                    }
                }
            }
        }

        // 2. OBSERVE UNTUK LIVE DATA (di luar lifecycleScope)
        // ==================================================

        // Observe categories berdasarkan tipe
        viewModel.categories.observe(viewLifecycleOwner) { categories: List<Category> ->
            categoriesList = categories

            val categoryNames = categories.map { "${it.icon} ${it.name}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategori.adapter = adapter

            // Listener untuk set category ID
            spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (categoriesList.isNotEmpty() && position < categoriesList.size) {
                        viewModel.setCategory(categoriesList[position].id)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun clearInputs() {
        etAmount.text?.clear()
        etNotes.text?.clear()
        radioPengeluaran.isChecked = true
        spinnerKategori.setSelection(0)
    }
}