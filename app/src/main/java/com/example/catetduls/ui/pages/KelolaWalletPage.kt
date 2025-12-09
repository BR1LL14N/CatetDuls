package com.example.catetduls.ui.pages

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Wallet
import com.example.catetduls.data.WalletType
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.ui.adapter.WalletAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class KelolaWalletPage : Fragment() {

    private lateinit var adapter: WalletAdapter
    private var activeBookId: Int = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Kita bisa reuse layout fragment_kelola_kategori atau buat baru fragment_kelola_wallet
        return inflater.inflate(R.layout.fragment_kelola_kategori, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide elemen search/filter kategori yang tidak perlu jika me-reuse layout
        view.findViewById<View>(R.id.til_search)?.visibility = View.GONE
        view.findViewById<View>(R.id.spinner_filter_type)?.visibility = View.GONE

        val walletRepo = requireContext().getWalletRepository()
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        activeBookId = prefs.getInt("active_book_id", 1)

        val rv = view.findViewById<RecyclerView>(R.id.rv_categories) // Reuse ID
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add)

        adapter = WalletAdapter { wallet ->
            showFormDialog(wallet)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Observe Data
        viewLifecycleOwner.lifecycleScope.launch {
            walletRepo.getWalletsByBook(activeBookId).collect { wallets ->
                adapter.submitList(wallets)
            }
        }

        fab.setOnClickListener {
            showFormDialog(null)
        }
    }

    private fun showFormDialog(wallet: Wallet?) {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle(if (wallet == null) "Tambah Dompet" else "Edit Dompet")

        // Setup Layout Form Sederhana secara Programmatic
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = EditText(context)
        inputName.hint = "Nama Dompet (cth: Bank BCA)"
        inputName.setText(wallet?.name ?: "")
        layout.addView(inputName)

        val inputIcon = EditText(context)
        inputIcon.hint = "Icon (cth: 🏦)"
        inputIcon.setText(wallet?.icon ?: "💰")
        layout.addView(inputIcon)

        // Field Saldo Awal (Hanya bisa diedit saat create baru agar aman, atau disable logic ini)
        val inputBalance = EditText(context)
        inputBalance.hint = "Saldo Awal"
        inputBalance.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (wallet != null) {
            inputBalance.setText(wallet.currentBalance.toString())
            inputBalance.isEnabled = false // Disable edit saldo langsung di sini demi konsistensi data transaksi
        }
        layout.addView(inputBalance)

        builder.setView(layout)

        builder.setPositiveButton("Simpan") { _, _ ->
            val name = inputName.text.toString()
            val icon = inputIcon.text.toString()
            val balanceStr = inputBalance.text.toString()
            val balance = balanceStr.toDoubleOrNull() ?: 0.0

            if (name.isNotBlank()) {
                val repo = context.getWalletRepository()
                viewLifecycleOwner.lifecycleScope.launch {
                    if (wallet == null) {
                        val newWallet = Wallet(
                            bookId = activeBookId,
                            name = name,
                            icon = icon,
                            type = WalletType.CASH, // Default type
                            initialBalance = balance,
                            currentBalance = balance,
                            lastSyncAt = 0L
                        )
                        repo.insert(newWallet)
                    } else {
                        val updatedWallet = wallet.copy(
                            name = name,
                            icon = icon
                            // Jangan update saldo di sini
                        )
                        repo.update(updatedWallet)
                    }
                    Toast.makeText(context, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }
}