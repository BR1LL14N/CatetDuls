package com.example.catetduls.ui.utils

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.catetduls.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

fun Fragment.showCustomDialog(
        icon: Int,
        iconTint: Int,
        title: String,
        message: String,
        confirmText: String,
        confirmColor: Int,
        onConfirm: () -> Unit
) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
    val dialog =
            MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val iconView = dialogView.findViewById<ImageView>(R.id.dialog_icon)
    val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
    val messageView = dialogView.findViewById<TextView>(R.id.dialog_message)
    val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
    val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)

    iconView.setImageResource(icon)
    iconView.setColorFilter(resources.getColor(iconTint, null))
    titleView.text = title
    messageView.text = message
    btnConfirm.text = confirmText
    btnConfirm.setBackgroundColor(resources.getColor(confirmColor, null))

    btnCancel.setOnClickListener { dialog.dismiss() }

    btnConfirm.setOnClickListener {
        animateClick(it) {
            onConfirm()
            dialog.dismiss()
        }
    }

    dialog.show()

    dialogView.alpha = 0f
    dialogView.scaleX = 0.8f
    dialogView.scaleY = 0.8f
    dialogView
            .animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
}

fun Fragment.showSnackbar(message: String, isError: Boolean = false) {
    val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
    val snackbarView = snackbar.view

    snackbarView.setBackgroundColor(
            resources.getColor(if (isError) R.color.danger else R.color.success, null)
    )

    snackbar.setTextColor(resources.getColor(R.color.white, null))
    snackbar.show()
}

fun animateClick(view: View, action: () -> Unit) {
    val scaleDown =
            android.animation.ObjectAnimator.ofPropertyValuesHolder(
                            view,
                            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.95f),
                            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.95f)
                    )
                    .apply { duration = 100 }

    val scaleUp =
            android.animation.ObjectAnimator.ofPropertyValuesHolder(
                            view,
                            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f),
                            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f)
                    )
                    .apply { duration = 100 }

    scaleDown.addListener(
            object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    scaleUp.start()
                    action()
                }
            }
    )

    scaleDown.start()
}
