package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.ui.adapter.MemoAdapter
import com.example.catetduls.viewmodel.MemoViewModel
import com.example.catetduls.viewmodel.MemoViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MemoPage : Fragment() {

    private lateinit var viewModel: MemoViewModel
    private lateinit var adapter: MemoAdapter

    private lateinit var rvMemos: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabAddMemo: FloatingActionButton
    private lateinit var fabManageTags: FloatingActionButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = MemoViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[MemoViewModel::class.java]

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        rvMemos = view.findViewById(R.id.rv_memos)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        fabAddMemo = view.findViewById(R.id.fab_add_memo)
        fabManageTags = view.findViewById(R.id.fab_manage_tags)
    }

    private fun setupRecyclerView() {
        adapter =
                MemoAdapter(
                        onItemClick = { memo -> showMemoDialog(memo) },
                        onItemLongClick = { memo ->
                            showDeleteConfirmation(memo.id)
                            true
                        }
                )

        rvMemos.layoutManager = LinearLayoutManager(requireContext())
        rvMemos.adapter = adapter
    }

    private fun setupListeners() {
        fabAddMemo.setOnClickListener { showMemoDialog(null) }

        fabManageTags.setOnClickListener {
            if (activity is NavigationCallback) {
                (activity as NavigationCallback).navigateTo(ManageTagsFragment())
            }
        }
    }

    private fun observeData() {
        viewModel.memos.observe(viewLifecycleOwner) { memos ->
            adapter.submitList(memos)

            // Show/hide empty state
            if (memos.isEmpty()) {
                layoutEmptyState.visibility = View.VISIBLE
                rvMemos.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                rvMemos.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.clearSuccess()
            }
        }
    }

    // Public method to be called from parent
    fun showFilterDialog() {
        val tagEntities = viewModel.tags.value ?: emptyList()
        val tagNames = tagEntities.map { it.name }.sorted()
        val items = arrayOf("Semua") + tagNames.toTypedArray()

        AlertDialog.Builder(requireContext())
                .setTitle("Filter berdasarkan Tag")
                .setItems(items) { _, which ->
                    if (which == 0) {
                        viewModel.filterByTag(null)
                    } else {
                        viewModel.filterByTag(items[which])
                    }
                }
                .setPositiveButton("Atur Tag") { _, _ ->
                    if (activity is NavigationCallback) {
                        (activity as NavigationCallback).navigateTo(ManageTagsFragment())
                    }
                }
                .show()
    }

    private fun showMemoDialog(memo: com.example.catetduls.data.Memo?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_memo, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_title)
        val etContent = dialogView.findViewById<TextInputEditText>(R.id.et_content)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_tags)


        // Set title
        tvTitle.text = if (memo == null) "Tambah Memo" else "Edit Memo"

        // Populate Chips Dynamically
        chipGroup.removeAllViews()
        val availableTags = viewModel.tags.value ?: emptyList()

        // Auto-select logic: If memo is null (new), select the first tag by default
        var activeTags = memo?.tags?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()

        if (memo == null && activeTags.isEmpty() && availableTags.isNotEmpty()) {
            activeTags = setOf(availableTags[0].name)
        }

        availableTags.forEach { tagEntity ->
            val chip = Chip(requireContext())
            chip.text = tagEntity.name
            chip.isCheckable = true
            chip.isChecked = activeTags.contains(tagEntity.name)
            if (tagEntity.color.isNotEmpty()) {
                try {
                    val colorInt = android.graphics.Color.parseColor(tagEntity.color)
                    chip.chipBackgroundColor =
                            android.content.res.ColorStateList.valueOf(colorInt)
                    chip.setTextColor(android.graphics.Color.WHITE)
                    chip.checkedIconTint =
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                } catch (e: Exception) {
                }
            }
            chip.isCheckedIconVisible = true
            chip.isChecked = activeTags.contains(tagEntity.name)
            chipGroup.addView(chip)
        }

        // Pre-fill if editing
        memo?.let {
            etTitle.setText(it.title)
            etContent.setText(it.content)
        }

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btn_save).setOnClickListener {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            // Collect selected tags
            val selectedTags = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedTags.add(chip.text.toString())
                }
            }
            val tagsString = selectedTags.joinToString(",")

            if (memo == null) {
                viewModel.saveMemo(title, content, tagsString)
            } else {
                viewModel.updateMemo(memo.id, title, content, tagsString)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(memoId: Int) {
        AlertDialog.Builder(requireContext())
                .setTitle("Hapus Memo")
                .setMessage("Apakah Anda yakin ingin menghapus memo ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteMemo(memoId)
                    Toast.makeText(requireContext(), "Memo dihapus", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
    }
}
