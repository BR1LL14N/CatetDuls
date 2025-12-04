package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.compose.material3.MaterialTheme
import com.example.catetduls.R
import com.example.catetduls.ui.page.ApiTestPage


class ApiTestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Set ID untuk ComposeView (diperlukan untuk FragmentManager)
            id = R.id.fragment_container

            // Menggunakan MaterialTheme standar (Wajib untuk semua Composable)
            setContent {
                MaterialTheme {
                    ApiTestPage()
                }
            }
        }
    }
}