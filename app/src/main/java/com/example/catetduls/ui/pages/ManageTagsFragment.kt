package com.example.catetduls.ui.pages

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.TagEntity
import com.example.catetduls.data.getTagRepository
import com.example.catetduls.ui.adapter.TagAdapter
import com.example.catetduls.viewmodel.ManageTagsViewModel
import com.example.catetduls.viewmodel.ManageTagsViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ManageTagsFragment : Fragment() {

    private lateinit var viewModel: ManageTagsViewModel
    private lateinit var adapter: TagAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = requireContext().getTagRepository()
        val factory = ManageTagsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ManageTagsViewModel::class.java]

        setupRecyclerView(view)
        setupListeners(view)
        observeData()
    }

    private fun setupRecyclerView(view: View) {
        val rvTags: RecyclerView = view.findViewById(R.id.rv_tags)
        adapter =
                TagAdapter(
                        onEditClick = { tag -> showAddEditDialog(tag) },
                        onDeleteClick = { tag ->
                            AlertDialog.Builder(requireContext())
                                    .setTitle("Hapus Tag")
                                    .setMessage(
                                            "Apakah Anda yakin ingin menghapus tag '${tag.name}'?"
                                    )
                                    .setPositiveButton("Hapus") { _, _ -> viewModel.delete(tag) }
                                    .setNegativeButton("Batal", null)
                                    .show()
                        }
                )
        rvTags.layoutManager = LinearLayoutManager(requireContext())
        rvTags.adapter = adapter
    }

    private fun setupListeners(view: View) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.fab_add_tag).setOnClickListener { showAddEditDialog(null) }
    }

    private fun observeData() {
        viewModel.allTags.observe(viewLifecycleOwner) { tags -> adapter.submitList(tags) }
    }

    private fun showAddEditDialog(tag: TagEntity?) {
        val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tag, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_tag_name)
        val chipGroupColors = dialogView.findViewById<ChipGroup>(R.id.cg_colors)

        // Pre-fill if editing
        var selectedColor = if (tag != null && tag.color.isNotEmpty()) {
            try {
                Color.parseColor(tag.color)
            } catch (e: Exception) {
                Color.parseColor("#4CAF50")
            }
        } else {
            Color.parseColor("#4CAF50")
        }

        if (tag != null) {
            etName.setText(tag.name)
        }

        chipGroupColors.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            chip?.let { selectedColor = it.chipBackgroundColor?.defaultColor ?: Color.GRAY }
        }

        AlertDialog.Builder(requireContext())
                .setTitle(if (tag == null) "Tambah Tag" else "Edit Tag")
                .setView(dialogView)
                .setPositiveButton("Simpan") { _, _ ->
                    val name = etName.text.toString()
                    val colorHex = String.format("#%06X", (0xFFFFFF and selectedColor))
                    
                    if (name.isNotBlank()) {
                        if (tag == null) {
                            viewModel.insert(name, colorHex)
                        } else {
                            viewModel.update(tag, name, colorHex)
                        }
                    } else {
                        Toast.makeText(
                                        requireContext(),
                                        "Nama tag tidak boleh kosong",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
    }
}
