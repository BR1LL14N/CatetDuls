package com.example.catetduls.ui.pages

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.sync.SyncManager
import com.example.catetduls.ui.viewmodel.LoginViewModel
import com.example.catetduls.utils.AppPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ✅ Interface untuk kontrol navbar
interface NavigationController {
    fun setNavBarVisibility(visibility: Int)
}

@AndroidEntryPoint
class LoginPage : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var tvSkipLogin: TextView? = null
    private var tvGoToRegister: TextView? = null
    private var tvForgotPassword: TextView? = null

    // ✅ Sembunyikan navbar saat buka halaman login
    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as? NavigationController)?.setNavBarVisibility(View.GONE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        observeViewModel()
        styleSkipLoginText()
    }

    // ✅ Inisialisasi View
    private fun initViews(view: View) {
        etEmail = view.findViewById(R.id.et_email)
        etPassword = view.findViewById(R.id.et_password)
        btnLogin = view.findViewById(R.id.btn_login)
        progressBar = view.findViewById(R.id.progress_bar)

        tvSkipLogin = view.findViewById(R.id.tv_skip_login)
        tvGoToRegister = view.findViewById(R.id.tv_go_to_register)
        tvForgotPassword = view.findViewById(R.id.tv_forgot_password)
    }

    // ✅ Semua listener terpusat
    private fun setupClickListeners() {

        // 🔐 Login
        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Email dan kata sandi wajib diisi")
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        // 👤 Masuk sebagai tamu
        tvSkipLogin?.setOnClickListener {
            AppPreferences.setFirstRunDone(requireContext())
            showNavbarAndGoToHome()
        }

        // 🔑 Lupa Password
        tvForgotPassword?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ForgotPasswordPage())
                .addToBackStack(null)
                .commit()
        }

        // 📝 Daftar
        tvGoToRegister?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterPage())
                .addToBackStack(null)
                .commit()
        }
    }

    // ✅ Observe ViewModel
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {

            // Loading state
            launch {
                viewModel.isLoading.collect { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    btnLogin.isEnabled = !isLoading
                }
            }

            // Error state
            launch {
                viewModel.errorMessage.collect { message ->
                    if (!message.isNullOrEmpty()) {
                        showToast(message)
                        viewModel.clearState()
                    }
                }
            }

            // Success state
            launch {
                viewModel.loginSuccess.collect { user ->
                    if (user != null) {
                        showToast("Selamat datang, ${user.name}!")

                        // Sync data
                        SyncManager.forceOneTimeSync(requireContext())
                        SyncManager.schedulePeriodicSync(requireContext())

                        AppPreferences.setFirstRunDone(requireContext())
                        showNavbarAndGoToHome()

                        viewModel.clearState()
                    }
                }
            }
        }
    }

    // ✅ Helper navigation ke Home
    private fun showNavbarAndGoToHome() {
        (activity as? NavigationController)?.setNavBarVisibility(View.VISIBLE)

        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TransaksiPage())
                .commit()
        }
    }

    // ✅ Styling teks "Masuk sebagai Tamu"
    private fun styleSkipLoginText() {
        val textView = tvSkipLogin ?: return

        val fullText = "Lewati | Masuk sebagai Tamu"
        val highlightText = "Masuk sebagai Tamu"

        val start = fullText.indexOf(highlightText)
        if (start == -1) return

        val end = start + highlightText.length
        val spannable = SpannableString(fullText)

        val blueColor = ContextCompat.getColor(requireContext(), R.color.blue_primary)

        spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blueColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannable
    }

    // ✅ Helper Toast
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
