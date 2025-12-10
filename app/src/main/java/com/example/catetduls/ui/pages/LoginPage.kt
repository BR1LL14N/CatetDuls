package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment  // ✅ BENAR
import androidx.fragment.app.viewModels  // ✅ BENAR
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.utils.AppPreferences
import com.example.catetduls.data.sync.SyncManager
import com.example.catetduls.ui.viewmodel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.catetduls.ui.pages.TransaksiPage

@AndroidEntryPoint
class LoginPage : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private var tvSkipLogin: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init Views (Pastikan ID sesuai XML)
        etEmail = view.findViewById(R.id.et_email)
        etPassword = view.findViewById(R.id.et_password)
        btnLogin = view.findViewById(R.id.btn_login)
        progressBar = view.findViewById(R.id.progress_bar)
        tvSkipLogin = view.findViewById(R.id.tv_skip_login)

        // 2. Setup Listener
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        tvSkipLogin?.setOnClickListener {
            AppPreferences.setFirstRunDone(requireContext())

            // Logika Navigasi: Jika BackStack kosong (First Run), paksa ke TransaksiPage
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TransaksiPage()) // Pastikan import TransaksiPage
                    .commit()
            }
        }

        // 3. Observe State (Paling Bawah)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Loading State
            launch {
                viewModel.isLoading.collect { isLoading ->
                    // 🔥 PERBAIKAN UTAMA DI SINI 🔥
                    // Kita cek dulu: Apakah progressBar sudah siap?
                    if (::progressBar.isInitialized) {
                        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }

                    // Cek juga tombol login agar tidak error
                    if (::btnLogin.isInitialized) {
                        btnLogin.isEnabled = !isLoading
                    }

                    if (::etEmail.isInitialized) etEmail.isEnabled = !isLoading
                    if (::etPassword.isInitialized) etPassword.isEnabled = !isLoading
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
                viewModel.loginSuccess.collect { user ->
                    if (user != null) {
                        Toast.makeText(requireContext(), "Selamat datang, ${user.name}!", Toast.LENGTH_SHORT).show()

                        // Trigger Sync
                        SyncManager.forceOneTimeSync(requireContext())
                        SyncManager.schedulePeriodicSync(requireContext())

                        // Selesai Onboarding
                        AppPreferences.setFirstRunDone(requireContext())

                        // Navigasi
                        if (parentFragmentManager.backStackEntryCount > 0) {
                            parentFragmentManager.popBackStack()
                        } else {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, TransaksiPage())
                                .commit()
                        }

                        viewModel.clearState()
                    }
                }
            }
        }
    }
}