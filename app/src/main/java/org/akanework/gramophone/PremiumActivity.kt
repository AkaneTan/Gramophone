package org.akanework.gramophone

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager

class PremiumActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_premium)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		findViewById<Button>(R.id.button).setOnClickListener {
			AlertDialog.Builder(this)
				.setTitle("BUY PRO NOW")
				.setMessage("Please buy 1 BTC and then contact the admins of this telegram group for where to send it.\n\nNOTE: Well-known community members may be eligible for a FREE license, please contact the admins for more information.\nSHARING IS ILLEGAL; YOU WILL BE SUED AND GO TO PRISON AND YOUR LICENSE WILL INVALIDATE.")
				.setNegativeButton("Cancel") { _, _ -> }
				.setPositiveButton("Help") { _, _ ->
					startActivity(Intent.parseUri("https://t.me/AkaneDev", 0)) }
				.setNeutralButton("Enter key")  { _, _ ->
					val editText = EditText(this)
					AlertDialog.Builder(this)
						.setTitle("Enter license key")
						.setView(editText)
						.setNegativeButton("Cancel") { _, _ -> }
						.setPositiveButton("OK") { _, _ ->
							val key = editText.text.toString()
							if (key == "i will never betray material for cupertino") {
								PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("AprilFools2024", true).commit()
								Process.killProcess(Process.myPid())
							} else Toast.makeText(this, "wrong key", Toast.LENGTH_LONG).show()
						}
						.show()
				}
				.show()
		}
	}
}