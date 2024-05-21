package com.example.projetointegrador3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import java.io.IOException

class RecuperarSenhaActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var voltarButton: Button
    private lateinit var confirmarRecuperacaoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperarsenha)

        // Inicialização dos componentes de interface do usuário
        emailEditText = findViewById(R.id.email_edit_text)
        voltarButton = findViewById(R.id.alternative_button)
        confirmarRecuperacaoButton = findViewById(R.id.confirmar_rec_button)

        // Configuração do botão "Voltar" para retornar à tela de login
        voltarButton.setOnClickListener {
            val intent = Intent(this@RecuperarSenhaActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Configuração do botão "Confirmar Recuperação" para iniciar o processo de recuperação de senha
        confirmarRecuperacaoButton.setOnClickListener {
            recuperarSenha()
        }
    }

    // Método para realizar a recuperação de senha
    private fun recuperarSenha() {
        val email = emailEditText.text.toString().trim()

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("email", email)
            .build()

        val request = Request.Builder()
            .url("https://southamerica-east1-projeto-integrador-3-415419.cloudfunctions.net/funcRecuperarSenha")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Em caso de falha na requisição, exibe mensagem de erro
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erro ao realizar recuperação de senha: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Trata a resposta do servidor
                val statusCode = response.code()
                if (!response.isSuccessful) {
                    // Se a resposta não for bem-sucedida, exibe mensagem de erro
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Erro ao realizar recuperação de senha: $statusCode", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                // Lê os dados da resposta
                val responseData = response.body()?.string()
                if (!responseData.isNullOrEmpty()) {
                    // Se a resposta não estiver vazia, exibe os dados e retorna para a tela de login
                    Log.d("RecuperarSenhaActivity", "Response: $responseData")
                    runOnUiThread {
                        Toast.makeText(applicationContext, responseData, Toast.LENGTH_SHORT).show()
                    }

                    val intent = Intent(this@RecuperarSenhaActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    // Se a resposta estiver vazia, exibe mensagem de erro
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Erro ao obter resposta do servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
