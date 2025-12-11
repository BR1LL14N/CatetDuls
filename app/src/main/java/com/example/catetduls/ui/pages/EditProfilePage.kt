package com.example.catetduls.ui.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.catetduls.R
// Pastikan import ini sesuai lokasi file ViewModel Anda
import com.example.catetduls.ui.viewmodel.EditProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfilePage : Fragment() {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivProfile: ImageView
    private lateinit var viewChangePhoto: View

    // Launcher Image Picker
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ivProfile.setImageURI(uri) // Preview lokal
            viewModel.updatePhoto(uri) // Upload ke server
        } else {
            Toast.makeText(requireContext(), "Tidak ada foto yang dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Views
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        btnSave = view.findViewById(R.id.btn_save)
        progressBar = view.findViewById(R.id.progress_bar)
        ivProfile = view.findViewById(R.id.iv_profile)
        viewChangePhoto = view.findViewById(R.id.view_change_photo)

        // Listener Foto
        viewChangePhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Listener Simpan
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            viewModel.updateProfile(name, email)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // A. Data User & Foto
            launch {
                viewModel.currentUser.collect { user ->
                    if (user != null) {
                        if (etName.text.isNullOrEmpty()) etName.setText(user.name)
                        if (etEmail.text.isNullOrEmpty()) etEmail.setText(user.email)

                        // Panggil fungsi load foto
                        loadUserPhoto(user.photo_url)
                    }
                }
            }

            // B. Loading
            launch {
                viewModel.isLoading.collect { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    btnSave.isEnabled = !isLoading
                    viewChangePhoto.isEnabled = !isLoading
                }
            }

            // C. Hasil Update
            launch {
                viewModel.updateResult.collect { result ->
                    result?.onSuccess { updatedUser ->
                        Toast.makeText(requireContext(), "Berhasil disimpan!", Toast.LENGTH_SHORT).show()

                        // Force refresh foto dari data terbaru
                        loadUserPhoto(updatedUser.photo_url)

                        // Reset state agar toast tidak muncul berulang
                        viewModel.resetState()
                    }?.onFailure { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun loadUserPhoto(photoUrl: String?) {
        if (!photoUrl.isNullOrEmpty()) {
            val fullUrl = if (photoUrl.startsWith("http")) {
                photoUrl
            } else {
                "http://10.0.2.2:8000$photoUrl"
            }

            Log.d("EditProfile", "Loading URL: $fullUrl")

            // Hapus tint default (warna abu-abu) agar foto asli terlihat
            ivProfile.imageTintList = null

            Glide.with(requireContext())
                .load(fullUrl)
                .timeout(60000) // <--- SOLUSI UTAMA: Tambah Timeout 60 Detik
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Jangan cache di disk
                .skipMemoryCache(true) // Jangan cache di memori
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .circleCrop() // Agar gambar bulat sempurna
                .into(ivProfile)
        } else {
            // Jika tidak ada foto, kembalikan ke icon default
            ivProfile.setImageResource(R.drawable.ic_person_24)
        }
    }
}