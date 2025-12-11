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
import com.example.catetduls.ui.viewmodel.LoginViewModel // ⭐ Menggunakan LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ⭐ Pastikan NavigationController sudah didefinisikan di suatu tempat
// interface NavigationController { fun setNavBarVisibility(visibility: Int) }

@AndroidEntryPoint
class ForgotPasswordPage : Fragment() {

    // ⭐ Menggunakan LoginViewModel, seperti pada LoginPage
    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendReset: MaterialButton
    private lateinit var progressBar: ProgressBar
    private var tvGoBack: TextView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Sembunyikan navbar segera setelah Fragment di-attach ke Activity
        (activity as? NavigationController)?.setNavBarVisibility(View.GONE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Asumsi layout: fragment_forgot_password
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Views (Menggunakan ID yang didefinisikan di fragment_forgot_password.xml sebelumnya)
        etEmail = view.findViewById(R.id.et_forgot_email)
        btnSendReset = view.findViewById(R.id.btn_send_reset_link)
        progressBar = view.findViewById(R.id.progress_bar)
        tvGoBack = view.findViewById(R.id.tv_go_back_login) // ID di layout Forgot Password

        // Setup Listener
        btnSendReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan alamat email Anda.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Memanggil fungsi forgotPassword di LoginViewModel
            viewModel.forgotPassword(email)
        }

        tvGoBack?.setOnClickListener {
            parentFragmentManager.popBackStack() // Kembali ke LoginPage
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Loading State
            launch {
                viewModel.isLoading.collect { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    btnSendReset.isEnabled = !isLoading
                    etEmail.isEnabled = !isLoading
                }
            }

            // Error State
            launch {
                viewModel.errorMessage.collect { error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), "Gagal: $error", Toast.LENGTH_LONG).show()
                        viewModel.clearState()
                    }
                }
            }

            // Success State (Asumsi LoginViewModel memiliki forgotPasswordSuccess Flow)
            launch {
                // Catatan: Anda perlu memastikan LoginViewModel mengekspos Flow<String?> untuk forgotPasswordSuccess
                viewModel.forgotPasswordSuccess.collect { message ->
                    if (message != null) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                        // Setelah sukses, kembali ke halaman login (Navbar tetap tersembunyi)
                        parentFragmentManager.popBackStack()
                        viewModel.clearState()
                    }
                }
            }
        }
    }
}