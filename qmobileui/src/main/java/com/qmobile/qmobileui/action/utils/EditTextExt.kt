package com.qmobile.qmobileui.action.utils

import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.widget.EditText

fun EditText.addSuffix(suffix: String) {
    val editText = this
    val formattedSuffix = " $suffix"
    var text = ""
    var isSuffixModified = false

    val setCursorPosition: () -> Unit =
        { Selection.setSelection(editableText, editableText.length - formattedSuffix.length) }

    val setEditText: () -> Unit = {
        editText.setText(text)
        setCursorPosition()
    }

    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            val newText = editable.toString()
            when {
                isSuffixModified -> {
                    // user tried to modify suffix
                    isSuffixModified = false
                    setEditText()
                }
                text.isNotEmpty() && newText.length < text.length && !newText.contains(formattedSuffix) -> {
                    // user tried to delete suffix
                    setEditText()
                }
                !newText.contains(formattedSuffix) -> {
                    // new input, add suffix
                    text = "$newText$formattedSuffix"
                    setEditText()
                }
                else -> {
                    text = newText
                }
            }
        }

        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
            charSequence?.let {
                val textLengthWithoutSuffix = it.length - formattedSuffix.length
                if (it.isNotEmpty() && start > textLengthWithoutSuffix) {
                    isSuffixModified = true
                }
            }
        }

        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
            // Nothing to do
        }
    })
}
