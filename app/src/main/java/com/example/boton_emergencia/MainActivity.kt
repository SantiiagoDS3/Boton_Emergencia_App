package com.example.boton_emergencia

import com.example.boton_emergencia.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.boton_emergencia.db.DbHelper


class MainActivity : AppCompatActivity() {
    private var controlNumber: EditText? = null
    private var password: EditText? = null
    private var loginButton: Button? = null
        private var registerButton: Button? = null
        private lateinit var exec: java.util.concurrent.ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlNumber = findViewById(R.id.controlNumber)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        val db = DbHelper(this)
        exec = java.util.concurrent.Executors.newSingleThreadExecutor()
        registerButton?.setOnClickListener {
            val intent = Intent(this@MainActivity, RegisterActivity::class.java)
            startActivity(intent)
        }
        loginButton?.setOnClickListener {
            val userControlNumber = controlNumber?.text.toString().trim()
            val userPassword = password?.text.toString()

            if (userControlNumber.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // basic validation
                if (userControlNumber.length < 4) {
                    controlNumber?.error = "Número de control inválido"
                    return@setOnClickListener
                }

                exec.execute {
                    val ok = db.checkUser(userControlNumber, userPassword)
                    runOnUiThread {
                        if (ok) {
                            // Check if user has contacts; if not, prompt to add
                            val cursor = db.getContactsForUser(userControlNumber)
                            if (cursor == null || !cursor.moveToFirst()) {
                                // ask user to add a contact first
                                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Agregar contacto cercano")
                                    .setMessage("Parece que no tienes un contacto cercano guardado. ¿Quieres agregar uno ahora?")
                                    .setPositiveButton("Sí") { _, _ ->
                                        val i = Intent(this@MainActivity, ContactoActivity::class.java)
                                        i.putExtra(ContactoActivity.EXTRA_CONTROL_NUMBER, userControlNumber)
                                        startActivityForResult(i, 2001)
                                    }
                                    .setNegativeButton("No") { _, _ ->
                                        // go to EmergencyActivity anyway
                                        val intent = Intent(this@MainActivity, EmergencyActivity::class.java)
                                        intent.putExtra("CONTROL_NUMBER", userControlNumber)
                                        startActivity(intent)
                                    }
                                    .setCancelable(false)
                                    .show()
                                cursor?.close()
                            } else {
                                cursor.close()
                                val intent = Intent(this@MainActivity, EmergencyActivity::class.java)
                                intent.putExtra("CONTROL_NUMBER", userControlNumber)
                                startActivity(intent)
                            }
                        } else {
                            Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == Activity.RESULT_OK) {
            // contact added, open EmergencyActivity
            val userControlNumber = controlNumber?.text.toString().trim()
            val intent = Intent(this@MainActivity, EmergencyActivity::class.java)
            intent.putExtra("CONTROL_NUMBER", userControlNumber)
            startActivity(intent)
        }

        // shutdown executor when activity destroyed
    }
    override fun onDestroy() {
        if (::exec.isInitialized) exec.shutdown()
        super.onDestroy()
    }
}