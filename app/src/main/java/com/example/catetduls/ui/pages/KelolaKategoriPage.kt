package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.ui.adapter.KategoriAdapter
import com.example.catetduls.ui.viewmodel.FormKategoriViewModel
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModel
import com.example.catetduls.ui.viewmodel.KelolaKategoriViewModelFactory

class KelolaKategoriPage : Fragment() {

    private lateinit var viewModel: KelolaKategoriViewModel
    private lateinit var adapter: KategoriAdapter

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
        val factory = KelolaKategoriViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[KelolaKategoriViewModel::class.java]

        adapter = KategoriAdapter(
            onDelete = { category ->
                if (!category.isDefault) {
                    Toast.makeText(requireContext(), "Hapus kategori belum diimplementasi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Kategori default tidak bisa dihapus", Toast.LENGTH_SHORT).show()
                }
            },
            onEdit = { category ->
                if (!category.isDefault) {
                    val form = FormKategoriPage.newInstance(category)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, form)
                        .addToBackStack(null)
                        .commit()
                }
            }
        )

        view.findViewById<RecyclerView>(R.id.rv_categories).apply {
            adapter = this@KelolaKategoriPage.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
        }

        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add)
            .setOnClickListener {
                val form = FormKategoriPage.newInstance(null)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, form)
                    .addToBackStack(null)
                    .commit()
            }
    }
}