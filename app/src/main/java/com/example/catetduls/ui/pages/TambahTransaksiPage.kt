package com.example.catetduls.ui.pages

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.TambahViewModel
import com.example.catetduls.viewmodel.TambahViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class TambahTransaksiPage : Fragment() {

    private lateinit var viewModel: TambahViewModel

    // Views
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioPemasukan: RadioButton
    private lateinit var radioPengeluaran: RadioButton
    private lateinit var tvLabelPemasukan: TextView
    private lateinit var tvLabelPengeluaran: TextView
    private lateinit var tvIconPemasukan: TextView
    private lateinit var tvIconPengeluaran: TextView
    private lateinit var cardPemasukan: MaterialCardView
    private lateinit var cardPengeluaran: MaterialCardView
    private lateinit var spinnerKategori: Spinner
    private lateinit var etAmount: TextInputEditText
    private lateinit var tvDate: TextView
    private lateinit var cardSelectDate: MaterialCardView
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Views BARU untuk Foto dan Lanjut
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var ivProofImage: ImageView
    private lateinit var btnLanjut: MaterialButton

    private var categoriesList: List<Category> = emptyList()
    private var imageUri: Uri? = null // Menyimpan URI gambar

    // Launcher untuk Galeri
    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                setImageProof(it)
            }
        }

    // Launcher untuk Kamera
    private val cameraLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                imageUri?.let { setImageProof(it) }
            } else {
                imageUri = null
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val factory = TambahViewModelFactory(transactionRepo, categoryRepo)
        viewModel = ViewModelProvider(this, factory)[TambahViewModel::class.java]

        initViews(view)

        setupRadioGroup()
        setupDatePicker()
        setupPhotoPicker()
        setupButtons()

        observeData()
    }

    private fun initViews(view: View) {
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioPemasukan = view.findViewById(R.id.radio_pemasukan)
        radioPengeluaran = view.findViewById(R.id.radio_pengeluaran)
        tvLabelPemasukan = view.findViewById(R.id.tv_label_pemasukan)
        tvLabelPengeluaran = view.findViewById(R.id.tv_label_pengeluaran)
        tvIconPemasukan = view.findViewById(R.id.tv_icon_pemasukan)
        tvIconPengeluaran = view.findViewById(R.id.tv_icon_pengeluaran)
        cardPemasukan = view.findViewById(R.id.card_pemasukan)
        cardPengeluaran = view.findViewById(R.id.card_pengeluaran)
        spinnerKategori = view.findViewById(R.id.spinner_kategori)
        etAmount = view.findViewById(R.id.et_amount)
        tvDate = view.findViewById(R.id.tv_date)

        cardSelectDate = view.findViewById(R.id.card_select_date)

        etNotes = view.findViewById(R.id.et_notes)
        btnSimpan = view.findViewById(R.id.btn_simpan)
        btnReset = view.findViewById(R.id.btn_reset)
        progressBar = view.findViewById(R.id.progress_bar)

        // Views Baru
        btnAddPhoto = view.findViewById(R.id.btn_add_photo)
        ivProofImage = view.findViewById(R.id.iv_proof_image)
        btnLanjut = view.findViewById(R.id.btn_lanjut)
    }

    private fun setupPhotoPicker() {
        btnAddPhoto.setOnClickListener {
            if (imageUri == null) {
                showImageSourceDialog()
            } else {
                showClearImageDialog()
            }
        }

        ivProofImage.setOnClickListener {
            if (imageUri != null) {
                showClearImageDialog()
            }
        }
        resetImageProof()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun launchCamera() {
        val context = requireContext()
        val filesDir = context.getExternalFilesDir(null)
        val imageFile = java.io.File(filesDir, "temp_photo_${System.currentTimeMillis()}.jpg")

        try {
            imageUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            // PERBAIKAN: Menggunakan let untuk menjamin non-null
            imageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }

        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Error: FileProvider belum dikonfigurasi.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showClearImageDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus Foto")
            .setMessage("Apakah Anda ingin menghapus bukti foto ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                resetImageProof()
                Toast.makeText(requireContext(), "Foto dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setImageProof(uri: Uri) {
        imageUri = uri
        ivProofImage.setImageURI(uri)
        ivProofImage.visibility = View.VISIBLE
        btnAddPhoto.setText("Hapus Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_baseline_close_24)
    }

    private fun resetImageProof() {
        imageUri = null
        ivProofImage.visibility = View.GONE
        ivProofImage.setImageResource(0)
        btnAddPhoto.setText("Tambah Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_photo_24)
    }

    private fun setupRadioGroup() {
        val cardPemasukan = view?.findViewById<MaterialCardView>(R.id.card_pemasukan)
        val cardPengeluaran = view?.findViewById<MaterialCardView>(R.id.card_pengeluaran)

        val typeClickListener = View.OnClickListener { v ->
            val type = if (v.id == R.id.card_pemasukan) TransactionType.PEMASUKAN else TransactionType.PENGELUARAN
            viewModel.setType(type)
            updateCardSelection()
            btnLanjut.visibility = View.GONE
        }
        cardPemasukan?.setOnClickListener(typeClickListener)
        cardPengeluaran?.setOnClickListener(typeClickListener)

        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.radio_pemasukan) TransactionType.PEMASUKAN else TransactionType.PENGELUARAN
            viewModel.setType(type)
            updateCardSelection()
            btnLanjut.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedType.collect { type ->
                if (type == TransactionType.PEMASUKAN) {
                    radioPemasukan.isChecked = true
                    radioPengeluaran.isChecked = false
                } else {
                    radioPengeluaran.isChecked = true
                    radioPemasukan.isChecked = false
                }
                updateCardSelection()
            }
        }
        updateCardSelection()
    }

    private fun updateCardSelection() {
        val cardPemasukan = view?.findViewById<MaterialCardView>(R.id.card_pemasukan)
        val cardPengeluaran = view?.findViewById<MaterialCardView>(R.id.card_pengeluaran)
        val tvLabelPemasukan = view?.findViewById<TextView>(R.id.tv_label_pemasukan)
        val tvLabelPengeluaran = view?.findViewById<TextView>(R.id.tv_label_pengeluaran)
        val tvIconPemasukan = view?.findViewById<TextView>(R.id.tv_icon_pemasukan)
        val tvIconPengeluaran = view?.findViewById<TextView>(R.id.tv_icon_pengeluaran)

        val colorSuccess = ContextCompat.getColor(requireContext(), R.color.success)
        val colorDanger = ContextCompat.getColor(requireContext(), R.color.danger)
        val colorBorder = ContextCompat.getColor(requireContext(), R.color.border)

        val isPemasukanChecked = radioPemasukan.isChecked

        if (isPemasukanChecked) {
            cardPemasukan?.apply {
                strokeColor = colorSuccess
                strokeWidth = 3
                cardElevation = 4f
            }
            tvLabelPemasukan?.setTypeface(null, android.graphics.Typeface.BOLD)
            tvIconPemasukan?.setTypeface(null, android.graphics.Typeface.BOLD)

            cardPengeluaran?.apply {
                strokeColor = colorBorder
                strokeWidth = 1
                cardElevation = 2f
            }
            tvLabelPengeluaran?.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvIconPengeluaran?.setTypeface(null, android.graphics.Typeface.NORMAL)

        } else {
            cardPengeluaran?.apply {
                strokeColor = colorDanger
                strokeWidth = 3
                cardElevation = 4f
            }
            tvLabelPengeluaran?.setTypeface(null, android.graphics.Typeface.BOLD)
            tvIconPengeluaran?.setTypeface(null, android.graphics.Typeface.BOLD)

            cardPemasukan?.apply {
                strokeColor = colorBorder
                strokeWidth = 1
                cardElevation = 2f
            }
            tvLabelPemasukan?.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvIconPemasukan?.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }


    private fun setupDatePicker() {
        cardSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = viewModel.date.value

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
            btnLanjut.visibility = View.GONE

            val amount = etAmount.text.toString()
            val notes = etNotes.text.toString()

            // TODO: Simpan URI gambar ke ViewModel
            // viewModel.setImageUri(imageUri)

            viewModel.setAmount(amount)
            viewModel.setNotes(notes)

            if (viewModel.isFormValid()) {
                viewModel.saveTransaction()
            } else {
                Toast.makeText(requireContext(), "Form belum lengkap", Toast.LENGTH_SHORT).show()
            }
        }

        btnLanjut.setOnClickListener {
            // Reset form, KEEP TYPE, dan set Tanggal Hari Ini
            viewModel.resetForm(keepType = true, keepCategory = false, setTodayDate = true)

            btnLanjut.visibility = View.GONE
            resetImageProof()

            etAmount.requestFocus()
            Toast.makeText(requireContext(), "Siap mencatat transaksi ${viewModel.selectedType.value} berikutnya!", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            viewModel.resetForm(keepType = false, keepCategory = false, setTodayDate = true)
            btnLanjut.visibility = View.GONE
            resetImageProof()
        }
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK STATEFLOW
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe tanggal
                launch {
                    viewModel.date.collect { timestamp ->
                        tvDate.text = viewModel.formatDate(timestamp)
                    }
                }

                // Observe amount & notes untuk reset
                launch { viewModel.amount.collect { if (it.isEmpty() && etAmount.text.toString().isNotEmpty()) etAmount.text?.clear() } }
                launch { viewModel.notes.collect { if (it.isEmpty() && etNotes.text.toString().isNotEmpty()) etNotes.text?.clear() } }

                // Observe loading state
                launch {
                    viewModel.isSaving.collect { isSaving ->
                        progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
                        btnSimpan.isEnabled = !isSaving
                        btnReset.isEnabled = !isSaving
                        btnLanjut.isEnabled = !isSaving
                    }
                }

                // Observe success message
                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()

                            // Setelah Sukses: Reset form, tampilkan tombol Lanjut
                            viewModel.resetForm(keepType = true, keepCategory = false, setTodayDate = true)

                            btnLanjut.visibility = View.VISIBLE
                            resetImageProof()

                            viewModel.clearMessages()
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

        // 2. OBSERVE UNTUK LIVE DATA (Categories)
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

            val selectedId = viewModel.selectedCategoryId.value
            val selectedIndex = categoriesList.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            spinnerKategori.setSelection(selectedIndex)

            // Listener untuk set category ID
            spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (categoriesList.isNotEmpty() && position < categoriesList.size) {
                        viewModel.setCategory(categoriesList[position].id)
                        btnLanjut.visibility = View.GONE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.setCategory(0)
                }
            }
        }
    }
}