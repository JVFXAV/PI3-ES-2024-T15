package com.example.projetointegrador3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var recuperarSenhaButton: Button
    private lateinit var gotoCadastroButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var gotoMapButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Instância do Firebase Authentication para gerenciar a autenticação
        firebaseAuth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.email_edit_text)
        senhaEditText = findViewById(R.id.senha_edit_text)
        loginButton = findViewById(R.id.login_button)
        recuperarSenhaButton = findViewById(R.id.recuperarSenha_button)
        gotoCadastroButton = findViewById(R.id.cadastro_button)
        gotoMapButton = findViewById(R.id.mapa_button) as ImageButton

        loginButton.setOnClickListener {
            if (camposEstaoPreenchidos()) {
                // loginButton.text = "Carregando..." // Muda o texto do botão para carregando enquanto verifica no banco.
                loginCliente()
            } else {
                Toast.makeText(this@LoginActivity, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        gotoCadastroButton.setOnClickListener {
            val intent = Intent(this@LoginActivity, CadastroActivity::class.java)
            startActivity(intent)
        }

        recuperarSenhaButton.setOnClickListener {
            val intent = Intent(this@LoginActivity, RecuperarSenhaActivity::class.java)
            startActivity(intent)
        }

        gotoMapButton.setOnClickListener {
            val intent = Intent(this@LoginActivity, MapActivity::class.java)
            startActivity(intent)
        }
    }

    // Método para verificar se os campos de e-mail e senha estão preenchidos.
    private fun camposEstaoPreenchidos(): Boolean {
        return emailEditText.text!!.isNotBlank() && senhaEditText.text!!.isNotBlank()
    }

    // Método para autenticar o usuário usando e-mail e senha.
    private fun loginCliente() {
        val email = emailEditText.text.toString().trim()
        val senha = senhaEditText.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        if (email.contains("smart")) {
                            verificaGerente(email)
                        } else {
                            // Verificar se teve a confirmação de email
                            if (user.isEmailVerified) {
                                iniciarSessaoUsuarioComum(user)
                            } else {
                                Toast.makeText(applicationContext, "E-mail não verificado. Por favor, verifique seu e-mail.", Toast.LENGTH_SHORT).show()
                                firebaseAuth.signOut() // Faz logout do usuário
                            }
                        }
                    }
                } else {
                    // Trata possíveis erros de login.
                    try {
                        throw task.exception!!
                    } catch (invalidEmail: FirebaseAuthInvalidUserException) {
                        Toast.makeText(applicationContext, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                    } catch (invalidPassword: FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(applicationContext, "Credenciais inválidas", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Erro ao fazer login: ${e.message}")
                        Toast.makeText(applicationContext, "Erro ao fazer login", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    // Método para iniciar sessão como usuário comum
    private fun iniciarSessaoUsuarioComum(user: FirebaseUser) {
        Toast.makeText(applicationContext, "Usuário logado: ${user.email}", Toast.LENGTH_SHORT).show()
        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        startActivity(intent)
    }

    // Método para verificar se o usuário é um gerente
    private fun verificaGerente(email: String) {
        val db = FirebaseFirestore.getInstance()
        val unidadeRef = db.collection("unidades")
        unidadeRef.whereEqualTo("usuarioGerente", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(applicationContext, "Acesso negado: Não registrado como gerente.", Toast.LENGTH_SHORT).show()
                } else {
                    val unidadeData = documents.first()
                    val unidadeId = unidadeData.data["id"] as String
                    iniciarSessaoGerente(unidadeId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Erro ao verificar gerente: ${e.message}")
                Toast.makeText(applicationContext, "Erro ao verificar status de gerente", Toast.LENGTH_SHORT).show()
            }
    }

    // Método para iniciar sessão como gerente
    private fun iniciarSessaoGerente(unidadeId: String) {
        val intent = Intent(this@LoginActivity, GerenteOptionsActivity::class.java)
        intent.putExtra("unidadeId", unidadeId)
        startActivity(intent)
    }
}
