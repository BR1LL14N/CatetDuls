package com.example.catetduls.ui.pages

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.ui.viewmodel.RegisterViewModel
import com.example.catetduls.utils.AppPreferences
import com.example.catetduls.data.sync.SyncManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch



@AndroidEntryPoint
class RegisterPage : Fragment() {

    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirmation: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar
    private var tvGoToLogin: TextView? = null

    // ⭐ 1. Panggil setNavBarVisibility di onAttach (Sembunyikan Navbar)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Sembunyikan navbar segera setelah Fragment di-attach ke Activity
        (activity as? NavigationController)?.setNavBarVisibility(View.GONE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Layout: fragment_register
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Views
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPassword = view.findViewById(R.id.et_password)
        etPasswordConfirmation = view.findViewById(R.id.et_password_confirmation)
        btnRegister = view.findViewById(R.id.btn_register)
        progressBar = view.findViewById(R.id.progress_bar)
        tvGoToLogin = view.findViewById(R.id.tv_go_to_login)

        // Setup Listeners
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordConfirmation = etPasswordConfirmation.text.toString().trim()

            // Basic Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirmation.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua kolom.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != passwordConfirmation) {
                Toast.makeText(requireContext(), "Konfirmasi password tidak cocok.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, password, passwordConfirmation)
        }

        tvGoToLogin?.setOnClickListener {
            // Kembali ke LoginPage (yang seharusnya sudah ada di back stack)
            // Navbar tidak perlu diatur karena LoginPage juga menyembunyikannya
            parentFragmentManager.popBackStack()
        }

        // Observe ViewModel
        observeViewModel()
    }

    // ⭐ onDestroyView tidak perlu menyetel visibilitas.
    // Kita bergantung pada Success State untuk menampilkan Navbar
    // saat navigasi ke TransaksiPage.
    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Loading State
            launch {
                viewModel.isLoading.collect { isLoading ->
                    if (::progressBar.isInitialized) {
                        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }

                    if (::btnRegister.isInitialized) {
                        btnRegister.isEnabled = !isLoading
                    }

                    if (::etName.isInitialized) etName.isEnabled = !isLoading
                    if (::etEmail.isInitialized) etEmail.isEnabled = !isLoading
                    if (::etPassword.isInitialized) etPassword.isEnabled = !isLoading
                    if (::etPasswordConfirmation.isInitialized) etPasswordConfirmation.isEnabled = !isLoading
                }
            }

            // Error State
            launch {
                viewModel.errorMessage.collect { error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        viewModel.clearState()
                    }
                }
            }

            // Success State
            launch {
                viewModel.registerSuccess.collect { user ->
                    if (user != null) {
                        Toast.makeText(
                            requireContext(),
                            "Registrasi berhasil! Selamat datang, ${user.name}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Trigger Sync
                        SyncManager.forceOneTimeSync(requireContext())
                        SyncManager.schedulePeriodicSync(requireContext())

                        // Selesai Onboarding
                        AppPreferences.setFirstRunDone(requireContext())

                        // ⭐ Tampilkan kembali Navbar sebelum navigasi ke halaman utama
                        (activity as? NavigationController)?.setNavBarVisibility(View.VISIBLE)

                        // Navigasi ke TransaksiPage
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, TransaksiPage())
                            .commit()

                        viewModel.clearState()
                    }
                }
            }
        }
    }
}