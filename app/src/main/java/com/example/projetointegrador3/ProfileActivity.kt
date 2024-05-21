package com.example.projetointegrador3

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var cpfEditText: EditText
    private lateinit var celEditText: EditText
    private lateinit var dtNascEditText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var saveButton: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        cpfEditText = findViewById(R.id.cpf_edit_text)
        celEditText = findViewById(R.id.cel_edit_text)
        dtNascEditText = findViewById(R.id.dtNasc_edit_text)
        backButton = findViewById(R.id.back_button)
        saveButton = findViewById(R.id.save_button)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = firebaseAuth.currentUser?.uid ?: ""

        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            showConfirmationDialog()
        }

        // Carregar e exibir os dados do usuário
        loadUserData()
    }

    private fun loadUserData() {
        val userRef = firestore.collection("pessoas").document(userId)
        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    nameEditText.setText(user?.nome ?: "")
                    emailEditText.setText(user?.email ?: "")
                    cpfEditText.setText(user?.cpf ?: "")
                    celEditText.setText(user?.celular ?: "")
                    dtNascEditText.setText(user?.dataNascimento ?: "")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar dados do usuário: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfirmationDialog() {
        val newName = nameEditText.text.toString()
        val newEmail = emailEditText.text.toString()
        val newCPF = cpfEditText.text.toString()
        val newCel = celEditText.text.toString()
        val newDtNasc = dtNascEditText.text.toString()

        val confirmationDialog = AlertDialog.Builder(this)
        confirmationDialog.apply {
            setTitle("Salvar alterações")
            setMessage("Deseja salvar as alterações?")
            setPositiveButton("Salvar", DialogInterface.OnClickListener { dialog, _ ->
                saveChanges(newName, newEmail, newCPF, newCel, newDtNasc)
                dialog.dismiss()
            })
            setNegativeButton("Cancelar", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            setCancelable(false)
            show()
        }
    }

    private fun saveChanges(name: String, email: String, cpf: String, cel: String, dtNasc: String) {
        val userRef = firestore.collection("pessoas").document(userId)
        userRef.update(mapOf(
            "nome" to name,
            "email" to email,
            "cpf" to cpf,
            "celular" to cel,
            "dataNascimento" to dtNasc
        ))
            .addOnSuccessListener {
                Toast.makeText(this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ocorreu um erro. Tente novamente mais tarde.", Toast.LENGTH_SHORT).show()
            }
    }

    // Classe de dados para representar o usuário
    data class User(
        val nome: String? = null,
        val email: String? = null,
        val cpf: String? = null,
        val celular: String? = null,
        val dataNascimento: String? = null
    )
}
