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
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.ui.viewmodel.FormKategoriViewModel
import com.example.catetduls.ui.viewmodel.FormKategoriViewModelFactory
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText

class FormKategoriPage : Fragment() {

    private lateinit var viewModel: FormKategoriViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_form_kategori, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = requireContext().getCategoryRepository()
        val category = arguments?.getParcelable<Category>("category")
        val factory = FormKategoriViewModelFactory(repo, category)
        viewModel = ViewModelProvider(this, factory)[FormKategoriViewModel::class.java]

        val etName = view.findViewById<TextInputEditText>(R.id.et_name)
        val etIcon = view.findViewById<TextInputEditText>(R.id.et_icon)
        val radioPemasukan = view.findViewById<MaterialRadioButton>(R.id.radio_pemasukan)
        val radioPengeluaran = view.findViewById<MaterialRadioButton>(R.id.radio_pengeluaran)
        val radioGroup = view.findViewById<android.widget.RadioGroup>(R.id.radio_group_type)
        val btnSave = view.findViewById<android.widget.Button>(R.id.btn_simpan)

        // Set default: Pengeluaran
        if (viewModel.type.value == TransactionType.PEMASUKAN) {
            radioPemasukan.isChecked = true
        } else {
            radioPengeluaran.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.radio_pemasukan) {
                TransactionType.PEMASUKAN
            } else {
                TransactionType.PENGELUARAN
            }
            viewModel.setType(type)
        }

        // TextWatcher untuk nama
        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setName(s.toString())
            }
        })

        // TextWatcher untuk icon
        etIcon.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setIcon(s.toString())
            }
        })

        btnSave.setOnClickListener {
            // Force commit text
            etName.clearFocus()
            etIcon.clearFocus()

            val nameInput = etName.text?.toString()?.trim() ?: ""
            val iconInput = etIcon.text?.toString() ?: "⚙️"

            if (nameInput.isEmpty()) {
                Toast.makeText(requireContext(), "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (viewModel.saveCategory(nameInput, iconInput)) {
                        // ✅ Tampilkan Toast DULU, baru kembali
                        Toast.makeText(
                            requireContext(),
                            "Kategori disimpan",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Gunakan delay kecil agar Toast sempat muncul
                        kotlinx.coroutines.delay(300)
                        requireActivity().onBackPressed()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Nama kategori tidak boleh kosong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal menyimpan: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        fun newInstance(category: Category? = null): FormKategoriPage {
            val fragment = FormKategoriPage()
            if (category != null) {
                val args = Bundle()
                args.putParcelable("category", category)
                fragment.arguments = args
            }
            return fragment
        }
    }
}