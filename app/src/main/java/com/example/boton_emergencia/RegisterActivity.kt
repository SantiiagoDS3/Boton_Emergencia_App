package com.example.boton_emergencia

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.boton_emergencia.db.DbHelper
import java.util.concurrent.Executors

class RegisterActivity : AppCompatActivity() {
    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val controlInput = findViewById<EditText>(R.id.registerControlNumber)
        val passwordInput = findViewById<EditText>(R.id.registerPassword)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val db = DbHelper(this)

        registerButton?.setOnClickListener {
            val control = controlInput?.text.toString().trim()
            val pass = passwordInput?.text.toString()
            if (control.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Basic validation: control length and password length
            if (control.length < 4) {
                controlInput.error = "Número de control inválido"
                return@setOnClickListener
            }
            if (pass.length < 4) {
                passwordInput.error = "Contraseña muy corta"
                return@setOnClickListener
            }

            exec.execute {
                val id = db.addUser(control, pass)
                runOnUiThread {
                    if (id == -1L) {
                        Toast.makeText(this, "Error: el número de control ya existe o ocurrió un error", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exec.shutdown()
    }
}
