package com.example.catetduls.ui.pages

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Wallet
import com.example.catetduls.data.WalletType
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.ui.adapter.WalletAdapter
import com.example.catetduls.ui.utils.animateClick
import com.example.catetduls.ui.utils.showSnackbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class KelolaWalletPage : Fragment() {

    private lateinit var adapter: WalletAdapter
    private var activeBookId: Int = 1

    private lateinit var etSearch: TextInputEditText
    private lateinit var rvWallets: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyState: View
    private lateinit var loadingOverlay: FrameLayout

    private val searchQuery = MutableStateFlow("")
    private val allWallets = MutableStateFlow<List<Wallet>>(emptyList())

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kelola_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val walletRepo = requireContext().getWalletRepository()
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        activeBookId = prefs.getInt("active_book_id", 1)

        initViews(view)
        setupRecyclerView()
        setupSearch()
        setupFAB()
        observeData(walletRepo)
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        rvWallets = view.findViewById(R.id.rv_wallets)
        fabAdd = view.findViewById(R.id.fab_add)
        emptyState = view.findViewById(R.id.empty_state)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    private fun setupRecyclerView() {
        adapter = WalletAdapter { wallet -> showFormDialog(wallet) }

        rvWallets.apply {
            adapter = this@KelolaWalletPage.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        searchQuery.value = s.toString()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
        )
    }

    private fun setupFAB() {
        fabAdd.setOnClickListener { animateClick(it) { showFormDialog(null) } }
    }

    private fun observeData(walletRepo: com.example.catetduls.data.WalletRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            walletRepo.getWalletsByBook(activeBookId).collect { wallets ->
                allWallets.value = wallets
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(allWallets, searchQuery) { wallets, query ->
                if (query.isBlank()) {
                    wallets
                } else {
                    wallets.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
                    .collect { filtered ->
                        adapter.submitList(filtered)

                        if (filtered.isEmpty()) {
                            rvWallets.visibility = View.GONE
                            emptyState.visibility = View.VISIBLE
                        } else {
                            rvWallets.visibility = View.VISIBLE
                            emptyState.visibility = View.GONE
                        }
                    }
        }
    }

    private fun showFormDialog(wallet: Wallet?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_form_wallet, null)
        val dialog =
                MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

        // Don't set transparent background - use default
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val etBalance = dialogView.findViewById<TextInputEditText>(R.id.et_balance)
        val etCustomIcon = dialogView.findViewById<TextInputEditText>(R.id.et_custom_icon)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        val iconButtons =
                listOf(
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_1) to "💰",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_2) to "🏦",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_3) to "💳",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_4) to "👛",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_5) to "💵",
                        dialogView.findViewById<android.widget.TextView>(R.id.btn_icon_6) to "🪙"
                )

        var selectedIcon = wallet?.icon ?: "💰"

        if (wallet != null) {
            tvTitle.text = "Edit Dompet"
            etName.setText(wallet.name)
            etBalance.setText(wallet.currentBalance.toString())
            etBalance.isEnabled = false
        } else {
            tvTitle.text = "Tambah Dompet"
            etBalance.setText("0")
        }

        iconButtons.forEach { (button, icon) ->
            if (icon == selectedIcon) {
                button.isSelected = true
            }
            button.setOnClickListener {
                selectedIcon = icon
                iconButtons.forEach { (btn, _) -> btn.isSelected = false }
                button.isSelected = true
                etCustomIcon.text = null // Clear custom when preset selected
            }
        }

        // Custom icon input handler
        etCustomIcon.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        if (!s.isNullOrBlank()) {
                            selectedIcon = s.toString()
                            iconButtons.forEach { (btn, _) -> btn.isSelected = false }
                        }
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                }
        )

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val balanceStr = etBalance.text.toString()
            val balance = balanceStr.toDoubleOrNull() ?: 0.0

            if (name.isBlank()) {
                showSnackbar("Nama dompet tidak boleh kosong", true)
                return@setOnClickListener
            }

            val repo = requireContext().getWalletRepository()
            viewLifecycleOwner.lifecycleScope.launch {
                if (wallet == null) {
                    val newWallet =
                            Wallet(
                                    bookId = activeBookId,
                                    name = name,
                                    icon = selectedIcon,
                                    type = WalletType.CASH,
                                    initialBalance = balance,
                                    currentBalance = balance,
                                    lastSyncAt = 0L
                            )
                    repo.insert(newWallet)
                    showSnackbar("Dompet $name berhasil ditambahkan")
                } else {
                    val updatedWallet = wallet.copy(name = name, icon = selectedIcon)
                    repo.update(updatedWallet)
                    showSnackbar("Dompet $name berhasil diupdate")
                }
                dialog.dismiss()
            }
        }

        dialog.show()

        dialogView.alpha = 0f
        dialogView.scaleX = 0.9f
        dialogView.scaleY = 0.9f
        dialogView
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
    }
}
