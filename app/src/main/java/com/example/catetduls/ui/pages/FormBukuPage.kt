package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.Book
import com.example.catetduls.ui.viewmodel.FormBukuViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FormBukuPage : Fragment() {

    private val viewModel: FormBukuViewModel by viewModels()

    private var existingBook: Book? = null

    private lateinit var etName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etIcon: TextInputEditText
    private lateinit var etCurrencySelection: android.widget.AutoCompleteTextView
    private lateinit var btnSimpan: MaterialButton

    private var selectedCurrencyCode: String = "IDR"
    private var selectedCurrencySymbol: String = "Rp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { existingBook = it.getParcelable(ARG_BOOK) }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_form_buku, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupCurrencyDropdown()
        populateData()
        setupListeners()
        observeEvents()
    }

    private fun initViews(view: View) {
        etName = view.findViewById(R.id.et_name)
        etDescription = view.findViewById(R.id.et_description)
        etIcon = view.findViewById(R.id.et_icon)
        etCurrencySelection = view.findViewById(R.id.et_currency_selection)
        btnSimpan = view.findViewById(R.id.btn_simpan)
    }

    private fun setupCurrencyDropdown() {
        val currencies = com.example.catetduls.utils.CurrencyHelper.getAvailableCurrencies()
        val adapter =
                android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        currencies
                )
        etCurrencySelection.setAdapter(adapter)

        etCurrencySelection.setOnItemClickListener { _, _, position, _ ->
            val selected = currencies[position]
            selectedCurrencyCode = selected.code
            selectedCurrencySymbol = selected.symbol
        }
    }

    private fun populateData() {
        existingBook?.let { book ->
            etName.setText(book.name)
            etDescription.setText(book.description)
            etIcon.setText(book.icon)

            // Set Selection based on book currency
            val currencies = com.example.catetduls.utils.CurrencyHelper.getAvailableCurrencies()
            val savedCurrency = currencies.find { it.code == book.currencyCode }

            if (savedCurrency != null) {
                etCurrencySelection.setText(savedCurrency.toString(), false)
                selectedCurrencyCode = savedCurrency.code
                selectedCurrencySymbol = savedCurrency.symbol
            } else {
                // Fallback if custom currency (or not in list)

                // ✅ PERBAIKAN: Gunakan nilai default jika null
                val code = book.currencyCode ?: "IDR"
                val symbol = book.currencySymbol ?: "Rp"

                etCurrencySelection.setText("$code - $symbol", false)
                selectedCurrencyCode = code
                selectedCurrencySymbol = symbol
            }

            btnSimpan.text = "Update Buku"
        }
    }

    private fun setupListeners() {
        btnSimpan.setOnClickListener {
            val name = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val icon = etIcon.text.toString().trim()

            viewModel.saveBook(
                    id = existingBook?.id ?: 0,
                    name = name,
                    description = description,
                    icon = icon.ifBlank { "📖" },
                    currencyCode = selectedCurrencyCode,
                    currencySymbol = selectedCurrencySymbol,
                    existingBook = existingBook
            )
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collectLatest { event ->
                when (event) {
                    is FormBukuViewModel.UiEvent.Success -> {
                        // 1. Beritahu user
                        Toast.makeText(requireContext(), "Berhasil disimpan", Toast.LENGTH_SHORT).show()

                        // 2. [PENTING] Panggil SyncManager untuk kirim data ke server SEKARANG JUGA
                        // Pastikan import com.example.catetduls.data.sync.SyncManager
                        try {
                            com.example.catetduls.data.sync.SyncManager.forceOneTimeSync(requireContext())
                        } catch (e: Exception) {
                            android.util.Log.e("FormBukuPage", "Gagal trigger sync: ${e.message}")
                        }

                        // 3. Tutup Halaman
                        parentFragmentManager.popBackStack()
                    }
                    is FormBukuViewModel.UiEvent.Error -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_BOOK = "arg_book"

        fun newInstance(book: Book?): FormBukuPage {
            val fragment = FormBukuPage()
            val args = Bundle()
            args.putParcelable(ARG_BOOK, book)
            fragment.arguments = args
            return fragment
        }
    }
}
