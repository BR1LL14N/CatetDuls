package com.example.catetduls.ui.pages

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.ui.adapter.BookAdapter
import com.example.catetduls.ui.viewmodel.KelolaBukuViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KelolaBukuPage : Fragment() {

    private val viewModel: KelolaBukuViewModel by viewModels()
    private lateinit var adapter: BookAdapter

    private lateinit var etSearch: TextInputEditText
    private lateinit var rvBooks: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

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
    }

    private fun setupRecyclerView() {
        adapter =
                BookAdapter(
                        onEdit = { book ->
                            // Navigate to Edit Form
                            val form = FormBukuPage.newInstance(book)
                            parentFragmentManager
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, form)
                                    .addToBackStack(null)
                                    .commit()
                        },
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

                            Toast.makeText(
                                            requireContext(),
                                            "Buku ${book.name} diaktifkan",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
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

        // Add Listener
        fabAdd.setOnClickListener {
            // Navigate to Add Form
            val form = FormBukuPage.newInstance(null)
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, form)
                    .addToBackStack(null)
                    .commit()
        }
    }

    private fun observeData() {
        viewModel.filteredBooks.observe(viewLifecycleOwner) { books -> adapter.submitList(books) }
    }

    private fun showDeleteConfirmation(book: com.example.catetduls.data.Book) {
        AlertDialog.Builder(requireContext())
                .setTitle("Hapus Buku")
                .setMessage(
                        "Apakah Anda yakin ingin menghapus buku '${book.name}'? Semua data transaksi di dalamnya akan ikut terhapus."
                )
                .setPositiveButton("Hapus") { _, _ -> viewModel.deleteBook(book) }
                .setNegativeButton("Batal", null)
                .show()
    }
}
