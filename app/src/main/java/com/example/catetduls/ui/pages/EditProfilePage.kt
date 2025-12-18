package com.example.catetduls.ui.pages

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.catetduls.R
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

    private var retryCount = 0
    private val maxRetries = 3

    // Launcher Image Picker
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("EditProfile", "Image selected: $uri")

            // Preview lokal langsung
            Glide.with(requireContext())
                .load(uri)
                .circleCrop()
                .into(ivProfile)

            // Upload ke server
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

                        // Reset retry count saat load foto baru
                        retryCount = 0
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

                        // Delay untuk memastikan server sudah siap
                        Handler(Looper.getMainLooper()).postDelayed({
                            retryCount = 0
                            loadUserPhoto(updatedUser.photo_url)
                        }, 500)

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
        if (photoUrl.isNullOrEmpty()) {
            Log.d("EditProfile", "No photo URL provided")
            ivProfile.setImageResource(R.drawable.ic_person_24)
            return
        }

        // Build full URL - pastikan format benar
        val fullUrl = when {
            photoUrl.startsWith("http://") || photoUrl.startsWith("https://") -> photoUrl
            photoUrl.startsWith("/api/") -> "http://10.0.2.2:8000$photoUrl"
            photoUrl.startsWith("/storage/") -> "http://10.0.2.2:8000$photoUrl"
            else -> "http://10.0.2.2:8000/api/photos/$photoUrl"
        }

        Log.d("EditProfile", "Loading URL: $fullUrl (Attempt ${retryCount + 1}/$maxRetries)")

        // Hapus tint default
        ivProfile.imageTintList = null

        Glide.with(requireContext())
            .load(fullUrl)
            .apply(
                RequestOptions()
                    .override(800, 800) // Resize untuk performa
            )
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_person_24)
            .error(R.drawable.ic_person_24)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("EditProfile", "Failed to load image: ${e?.message}")
                    e?.logRootCauses("EditProfile")

                    // Retry logic
                    if (retryCount < maxRetries) {
                        retryCount++
                        Log.d("EditProfile", "Retrying... ($retryCount/$maxRetries)")

                        Handler(Looper.getMainLooper()).postDelayed({
                            loadUserPhoto(photoUrl)
                        }, 1000) // Retry setelah 1 detik
                    } else {
                        Log.e("EditProfile", "Max retries reached. Giving up.")
                        Toast.makeText(
                            requireContext(),
                            "Gagal memuat foto profil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("EditProfile", "Image loaded successfully from: ${dataSource?.name}")
                    retryCount = 0 // Reset retry count on success
                    return false
                }
            })
            .circleCrop()
            .into(ivProfile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear Glide untuk mencegah memory leak
        Glide.with(requireContext()).clear(ivProfile)
    }
}