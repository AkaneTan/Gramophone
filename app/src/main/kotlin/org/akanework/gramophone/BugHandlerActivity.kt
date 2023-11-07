package org.akanework.gramophone

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class BugHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug_handler)

        val bugText = findViewById<TextView>(R.id.error)
        val receivedText = intent.getStringExtra("exception_message")
        bugText.typeface = Typeface.MONOSPACE
        bugText.text = receivedText
        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("error msg", receivedText)
        clipboard.setPrimaryClip(clip)

    }
}