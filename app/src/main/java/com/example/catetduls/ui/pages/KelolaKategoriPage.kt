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
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.ui.adapter.KategoriAdapter
import com.example.catetduls.ui.utils.animateClick
import com.example.catetduls.ui.utils.showCustomDialog
import com.example.catetduls.ui.utils.showSnackbar
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModel
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class KelolaKategoriPage : Fragment() {

    private lateinit var viewModel: KelolaKategoriViewModel
    private lateinit var adapter: KategoriAdapter
    private var activeBookId: Int = 1

    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerFilter: Spinner
    private lateinit var rvCategories: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyState: View
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kelola_kategori, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = requireContext().getCategoryRepository()
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        activeBookId = prefs.getInt("active_book_id", 1)

        val factory = KelolaKategoriViewModelFactory(repo, activeBookId)
        viewModel = ViewModelProvider(this, factory)[KelolaKategoriViewModel::class.java]

        initViews(view)
        setupRecyclerView()
        setupSearch()
        setupFilter()
        setupFAB()
        observeData()
    }

    private fun initViews(view: View) {
        rvCategories = view.findViewById(R.id.rv_categories)
        fabAdd = view.findViewById(R.id.fab_add)
        etSearch = view.findViewById(R.id.et_search)
        spinnerFilter = view.findViewById(R.id.spinner_filter_type)
        emptyState = view.findViewById(R.id.empty_state)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    private fun setupRecyclerView() {
        adapter =
                KategoriAdapter(
                        onDelete = { category ->
                            if (!category.isDefault) {
                                showCustomDialog(
                                        icon = R.drawable.ic_delete_24,
                                        iconTint = R.color.danger,
                                        title = "Hapus Kategori",
                                        message = "Yakin hapus kategori '${category.name}'?",
                                        confirmText = "Ya, Hapus",
                                        confirmColor = R.color.danger,
                                        onConfirm = {
                                            // TODO: Implement delete in ViewModel
                                            showSnackbar("Fitur hapus belum diimplementasi")
                                        }
                                )
                            } else {
                                showSnackbar("Kategori default tidak bisa dihapus", true)
                            }
                        },
                        onEdit = { category -> showFormDialog(category) }
                )

        rvCategories.apply {
            adapter = this@KelolaKategoriPage.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        viewModel.setSearchQuery(s.toString())
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
        )
    }

    private fun setupFilter() {
        val filterOptions = resources.getStringArray(R.array.category_filter_options).toList()
        val spinnerAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        spinnerFilter.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val type: TransactionType? =
                                when (filterOptions[position]) {
                                    "Semua" -> null
                                    TransactionType.PEMASUKAN.name -> TransactionType.PEMASUKAN
                                    TransactionType.PENGELUARAN.name -> TransactionType.PENGELUARAN
                                    else -> null
                                }
                        viewModel.setFilterType(type)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupFAB() {
        fabAdd.setOnClickListener { animateClick(it) { showFormDialog(null) } }
    }

    private fun observeData() {
        viewModel.filteredCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)

            if (categories.isEmpty()) {
                rvCategories.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                rvCategories.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
            }
        }
    }

    private fun showFormDialog(category: Category?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_form_kategori, null)
        val dialog =
                MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val etCustomIcon = dialogView.findViewById<TextInputEditText>(R.id.et_custom_icon)
        val btnTypeIncome = dialogView.findViewById<MaterialButton>(R.id.btn_type_income)
        val btnTypeExpense = dialogView.findViewById<MaterialButton>(R.id.btn_type_expense)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        val iconButtons =
                listOf(
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_1) to "🍔",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_2) to "🏠",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_3) to "🚗",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_4) to "💼",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_5) to "📚",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_6) to "🎮"
                )

        var selectedIcon = category?.icon ?: "🍔"
        var selectedType = category?.type ?: TransactionType.PENGELUARAN

        if (category != null) {
            tvTitle.text = "Edit Kategori"
            etName.setText(category.name)
        } else {
            tvTitle.text = "Tambah Kategori"
        }

        // Type selector
        fun updateTypeButtons() {
            if (selectedType == TransactionType.PEMASUKAN) {
                btnTypeIncome.strokeWidth = 8
                btnTypeExpense.strokeWidth = 2
            } else {
                btnTypeIncome.strokeWidth = 2
                btnTypeExpense.strokeWidth = 8
            }
        }
        updateTypeButtons()

        btnTypeIncome.setOnClickListener {
            selectedType = TransactionType.PEMASUKAN
            updateTypeButtons()
        }

        btnTypeExpense.setOnClickListener {
            selectedType = TransactionType.PENGELUARAN
            updateTypeButtons()
        }

        // Icon selector
        iconButtons.forEach { (button, icon) ->
            if (icon == selectedIcon) {
                button.isSelected = true
            }
            button.setOnClickListener {
                selectedIcon = icon
                iconButtons.forEach { (btn, _) -> btn.isSelected = false }
                button.isSelected = true
                etCustomIcon.text = null
            }
        }

        // Custom icon
        etCustomIcon.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        if (!s.isNullOrBlank()) {
                            selectedIcon = s.toString()
                            iconButtons.forEach { (btn, _) -> btn.isSelected = false }
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
        )

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()

            if (name.isBlank()) {
                showSnackbar("Nama kategori tidak boleh kosong", true)
                return@setOnClickListener
            }

            val repo = requireContext().getCategoryRepository()
            viewLifecycleOwner.lifecycleScope.launch {
                if (category == null) {
                    val newCategory =
                            Category(
                                    bookId = activeBookId,
                                    name = name,
                                    icon = selectedIcon,
                                    type = selectedType,
                                    isDefault = false,
                                    lastSyncAt = System.currentTimeMillis()
                            )
                    repo.insertCategory(newCategory)
                    showSnackbar("Kategori $name berhasil ditambahkan")
                } else {
                    val updated =
                            category.copy(name = name, icon = selectedIcon, type = selectedType)
                    repo.updateCategory(updated)
                    showSnackbar("Kategori $name berhasil diupdate")
                }
                dialog.dismiss()
            }
        }

        dialog.show()

        dialogView.alpha = 0f
        dialogView.scaleX = 0.9f
        dialogView.scaleY = 0.9f
        dialogView
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
    }
}
