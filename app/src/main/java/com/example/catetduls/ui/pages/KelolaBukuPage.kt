package com.example.catetduls.ui.pages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.getBookRepository
import com.example.catetduls.ui.adapter.BookAdapter
import com.example.catetduls.ui.utils.animateClick
import com.example.catetduls.ui.utils.showCustomDialog
import com.example.catetduls.ui.utils.showSnackbar
import com.example.catetduls.ui.viewmodel.KelolaBukuViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class KelolaBukuPage : Fragment() {

    private val viewModel: KelolaBukuViewModel by viewModels()
    private lateinit var adapter: BookAdapter

    private lateinit var etSearch: TextInputEditText
    private lateinit var rvBooks: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyState: View
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kelola_buku, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        rvBooks = view.findViewById(R.id.rv_books)
        fabAdd = view.findViewById(R.id.fab_add)
        emptyState = view.findViewById(R.id.empty_state)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    private fun setupRecyclerView() {
        adapter =
                BookAdapter(
                        onEdit = { book -> showFormDialog(book) },
                        onDelete = { book ->
                            if (book.isActive) {
                                Toast.makeText(
                                                requireContext(),
                                                "Tidak bisa menghapus buku yang sedang aktif",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            } else {
                                showDeleteConfirmation(book)
                            }
                        },
                        onActivate = { book ->
                            viewModel.activateBook(book.id)

                            // Update SharedPreferences for legacy app compatibility
                            val prefs =
                                    requireContext()
                                            .getSharedPreferences(
                                                    "app_settings",
                                                    android.content.Context.MODE_PRIVATE
                                            )
                            prefs.edit().putInt("active_book_id", book.id).apply()

                            showSnackbar("Buku ${book.name} diaktifkan")
                        }
                )

        rvBooks.apply {
            adapter = this@KelolaBukuPage.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        // Search Listener
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

        // Add Listener with animation
        fabAdd.setOnClickListener { animateClick(it) { showFormDialog(null) } }
    }

    private fun observeData() {
        viewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)

            if (books.isEmpty()) {
                rvBooks.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                rvBooks.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmation(book: com.example.catetduls.data.Book) {
        showCustomDialog(
                icon = R.drawable.ic_delete_24,
                iconTint = R.color.danger,
                title = "Hapus Buku",
                message =
                        "Apakah Anda yakin ingin menghapus buku '${book.name}'? Semua data transaksi di dalamnya akan ikut terhapus.",
                confirmText = "Ya, Hapus",
                confirmColor = R.color.danger,
                onConfirm = {
                    viewModel.deleteBook(book)
                    showSnackbar("Buku ${book.name} berhasil dihapus")
                }
        )
    }

    private fun showFormDialog(book: com.example.catetduls.data.Book?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_form_buku, null)
        val dialog =
                MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        if (book != null) {
            tvTitle.text = "Edit Buku"
            etName.setText(book.name)
        } else {
            tvTitle.text = "Tambah Buku"
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()

            if (name.isBlank()) {
                showSnackbar("Nama buku tidak boleh kosong", true)
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val repo = requireContext().getBookRepository()
                if (book == null) {
                    val newBook = com.example.catetduls.data.Book(name = name, isActive = false)
                    repo.insert(newBook)
                    showSnackbar("Buku $name berhasil ditambahkan")
                } else {
                    val updated = book.copy(name = name)
                    repo.update(updated)
                    showSnackbar("Buku $name berhasil diupdate")
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
