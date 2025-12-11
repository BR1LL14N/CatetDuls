package com.example.catetduls.ui.pages

import android.os.Bundle
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

    // ==========================================================
    // 1. INI YANG HILANG SEBELUMNYA (Variabel pickMedia)
    // ==========================================================
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Tampilkan preview sementara
            ivProfile.setImageURI(uri)
            // Langsung upload ke server via ViewModel
            viewModel.updatePhoto(uri)
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

        // 2. Init Views
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        btnSave = view.findViewById(R.id.btn_save)
        progressBar = view.findViewById(R.id.progress_bar)

        // Pastikan ID ini ada di file XML fragment_edit_profile.xml
        ivProfile = view.findViewById(R.id.iv_profile)
        viewChangePhoto = view.findViewById(R.id.view_change_photo)

        // 3. Listener Ganti Foto
        viewChangePhoto.setOnClickListener {
            // Buka Galeri (Hanya Image)
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 4. Listener Simpan (HANYA Nama & Email)
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            viewModel.updateProfile(name, email)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // A. Isi form otomatis DAN update foto setiap kali user berubah
            launch {
                viewModel.currentUser.collect { user ->
                    if (user != null) {
                        // Update form
                        if (etName.text.isNullOrEmpty()) etName.setText(user.name)
                        if (etEmail.text.isNullOrEmpty()) etEmail.setText(user.email)

                        // ✅ PERBAIKAN: Load foto SETIAP KALI user berubah
                        loadUserPhoto(user.photo_url)
                    }
                }
            }

            // B. Loading State
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
                        Toast.makeText(requireContext(), "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()

                        // ✅ PERBAIKAN: Refresh foto sebelum kembali
                        loadUserPhoto(updatedUser.photo_url)

                        // ✅ Tambahkan delay kecil agar foto sempat ter-load
                        kotlinx.coroutines.delay(500)

                        // Jangan langsung popBackStack jika hanya update foto
                        // parentFragmentManager.popBackStack()
                    }?.onFailure { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    if (result != null) viewModel.resetState()
                }
            }
        }


    }

    private fun loadUserPhoto(photoUrl: String?) {
        android.util.Log.d("EditProfile", "Raw photo_url from DB: $photoUrl")

        if (!photoUrl.isNullOrEmpty()) {
            val fullUrl = if (photoUrl.startsWith("http")) {
                photoUrl
            } else {
                "http://10.0.2.2:8000$photoUrl"
            }

            android.util.Log.d("EditProfile", "Full URL for Glide: $fullUrl")

            // ✅ HAPUS TINT
            ivProfile.imageTintList = null

            Glide.with(requireContext())
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .circleCrop()
                .into(ivProfile)
        } else {
            android.util.Log.w("EditProfile", "photo_url is NULL or EMPTY")
            ivProfile.setImageResource(R.drawable.ic_person_24)
        }
    }
}