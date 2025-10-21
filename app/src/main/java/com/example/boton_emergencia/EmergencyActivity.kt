package com.example.boton_emergencia

import com.example.boton_emergencia.R
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.boton_emergencia.db.DbHelper


class EmergencyActivity : AppCompatActivity() {
    private var controlNumber: String? = null
    private var pendingReason: String? = null
    private var selectedContactIdForLogging: Long = -1L
    private lateinit var db: DbHelper

    companion object {
        private const val REQUEST_CODE_CONTACTO = 1001
        private const val PREFS_NAME = "boton_prefs"
        private const val PREF_CONTACTO = "contacto_cercano"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

    controlNumber = intent.getStringExtra("CONTROL_NUMBER")
    // If launched with a selected contact id (from ContactList), use it
    val selectedContactId = intent.getLongExtra("selectedContactId", -1L)
    if (selectedContactId > 0) {
        selectedContactIdForLogging = selectedContactId
        // try to fetch the contact and send immediately to that contact
        try {
            val c = db.getContactById(selectedContactId)
            if (c != null && c.moveToFirst()) {
                val phone = c.getString(c.getColumnIndexOrThrow("phone"))
                c.close()
                // default reason when invoked directly from contact list
                sendWhatsAppMessage("ayuda a mi contacto cercano", phone)
            } else {
                c?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    db = DbHelper(this)

    val enfermeriaButton = findViewById<Button?>(R.id.enfermeriaButton)
    val contactoButton = findViewById<Button?>(R.id.contactoButton)
    val contactoEditButton = findViewById<android.widget.ImageButton?>(R.id.contactoEditButton)
    val tutorButton = findViewById<Button?>(R.id.tutorButton)

        val listener = View.OnClickListener { v ->
            var reason = ""
            when (v.id) {
                R.id.enfermeriaButton -> reason = "atención médica urgente en enfermería"
                R.id.contactoButton -> reason = "ayuda a mi contacto cercano"
                R.id.tutorButton -> reason = "ayuda a mi tutor"
            }

            if (v.id == R.id.contactoButton) {
                val cursor = db.getContactsForUser(controlNumber ?: "")
                if (cursor == null || !cursor.moveToFirst()) {
                    // No hay número guardado: abrir pantalla para ingresar contacto
                    pendingReason = reason
                    val intent = Intent(this, ContactoActivity::class.java)
                    intent.putExtra(ContactoActivity.EXTRA_CONTROL_NUMBER, controlNumber)
                    startActivityForResult(intent, REQUEST_CODE_CONTACTO)
                    cursor?.close()
                    return@OnClickListener
                } else {
                    // use the first contact's phone
                    val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                    cursor.close()
                    sendWhatsAppMessage(reason, phone)
                }
            } else {
                // Para enfermería/tutor usamos el número del chatbot (o cambiar según necesidad)
                val chatbotNumber = "521234567890"
                sendWhatsAppMessage(reason, chatbotNumber)
            }
        }

        enfermeriaButton?.setOnClickListener(listener)
        contactoButton?.setOnClickListener(listener)
        contactoEditButton?.setOnClickListener {
            // Open the contact list to manage contacts
            val i = Intent(this, ContactListActivity::class.java)
            i.putExtra(ContactListActivity.EXTRA_CONTROL, controlNumber)
            startActivity(i)
        }
        tutorButton?.setOnClickListener(listener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CONTACTO && resultCode == Activity.RESULT_OK) {
            val cursor = db.getContactsForUser(controlNumber ?: "")
            if (cursor != null && cursor.moveToFirst() && !pendingReason.isNullOrEmpty()) {
                val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                cursor.close()
                sendWhatsAppMessage(pendingReason, phone)
            }
            pendingReason = null
        }
    }

    private fun sendWhatsAppMessage(reason: String?, phoneNumber: String) {
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val message = "ALERTA DE EMERGENCIA\n\n" +
                "Estudiante: " + controlNumber + "\n" +
                "Solicito " + reason + "\n" +
                "Hora: " + currentTime

        val packageManager = getPackageManager()
        val i = Intent(Intent.ACTION_VIEW)

        try {
            val url =
                "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + URLEncoder.encode(
                    message,
                    "UTF-8"
                )
            i.setPackage("com.whatsapp")
            i.setData(Uri.parse(url))
            if (i.resolveActivity(packageManager) != null) {
                // Log the alert before launching WhatsApp (non-blocking can be improved later)
                try {
                    db.addAlert(controlNumber ?: "", if (selectedContactIdForLogging > 0) selectedContactIdForLogging else null, message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startActivity(i)
            } else {
                Toast.makeText(this, "WhatsApp no está instalado.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al enviar el mensaje.", Toast.LENGTH_SHORT).show()
        }
    }
}