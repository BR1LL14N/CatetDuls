package com.example.catetduls.ui.pages

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.ui.adapter.KategoriAdapter
import com.example.catetduls.ui.viewmodel.FormKategoriViewModel
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModel
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class KelolaKategoriPage : Fragment() {

    private lateinit var viewModel: KelolaKategoriViewModel
    private lateinit var adapter: KategoriAdapter

    // Properti Views untuk Search dan Filter
    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerFilter: Spinner
    private lateinit var rvCategories: RecyclerView
    private lateinit var fabAdd: FloatingActionButton


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kelola_kategori, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. AMBIL ACTIVE BOOK ID & INIT VIEW MODEL ---
        val repo = requireContext().getCategoryRepository()

        val activeBookId: Int = try {
            val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.getInt("active_book_id", 1) // Mengambil ID Buku Aktif
        } catch (e: Exception) {
            1 // Fallback
        }

        val factory = KelolaKategoriViewModelFactory(repo, activeBookId)
        viewModel = ViewModelProvider(this, factory)[KelolaKategoriViewModel::class.java]

        // --- 2. INIT VIEWS ---
        rvCategories = view.findViewById(R.id.rv_categories)
        fabAdd = view.findViewById(R.id.fab_add)
        etSearch = view.findViewById(R.id.et_search) // Inisialisasi Search Input
        spinnerFilter = view.findViewById(R.id.spinner_filter_type) // Inisialisasi Filter Spinner


        // --- 3. SETUP ADAPTER DAN RECYCLERVIEW ---
        adapter = KategoriAdapter(
            onDelete = { category ->
                // Logika delete (hanya non-default)
                if (!category.isDefault) {
                    Toast.makeText(requireContext(), "Hapus kategori belum diimplementasi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Kategori default tidak bisa dihapus", Toast.LENGTH_SHORT).show()
                }
            },
            onEdit = { category ->
                // Mengizinkan edit SEMUA, termasuk default
                val form = FormKategoriPage.newInstance(category)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, form)
                    .addToBackStack(null)
                    .commit()
            }
        )

        rvCategories.apply {
            adapter = this@KelolaKategoriPage.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // --- 4. OBSERVER DATA ---
        viewModel.filteredCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
        }

        // --- 5. SETUP SEARCH DAN FILTER LISTENERS ---

        // A. Listener Search:
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Meneruskan query ke ViewModel
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // B. Listener Filter Type Spinner:
        // Filter options diambil dari string array yang sudah Anda definisikan
        val filterOptions = resources.getStringArray(R.array.category_filter_options).toList()

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = filterOptions[position]
                val type: TransactionType? = when (selected) {
                    "Semua" -> null
                    TransactionType.PEMASUKAN.name -> TransactionType.PEMASUKAN
                    TransactionType.PENGELUARAN.name -> TransactionType.PENGELUARAN
                    else -> null
                }
                // Meneruskan tipe filter ke ViewModel
                viewModel.setFilterType(type)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        // --- 6. FAB Add ---
        fabAdd.setOnClickListener {
            val form = FormKategoriPage.newInstance(null)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, form)
                .addToBackStack(null)
                .commit()
        }
    }
}