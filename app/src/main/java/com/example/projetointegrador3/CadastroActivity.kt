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

class CadastroActivity : AppCompatActivity() {

    private lateinit var nomeEditText: TextInputEditText
    private lateinit var cpfEditText: TextInputEditText
    private lateinit var dtNascEditText: TextInputEditText
    private lateinit var celEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var cadastrarButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        // Inicialização dos componentes de interface do usuário
        nomeEditText = findViewById(R.id.nome_edit_text)
        cpfEditText = findViewById(R.id.cpf_edit_text)
        dtNascEditText = findViewById(R.id.dtnasc_edit_text)
        celEditText = findViewById(R.id.cel_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        senhaEditText = findViewById(R.id.senha_edit_text)
        loginButton = findViewById(R.id.login_button)
        cadastrarButton = findViewById(R.id.cadastrar_cliente_button)

        // Aplicação de máscaras aos campos de entrada
        cpfEditText.addTextChangedListener(MaskEditUtil.mask(cpfEditText, MaskEditUtil.FORMAT_CPF))
        dtNascEditText.addTextChangedListener(MaskEditUtil.mask(dtNascEditText, MaskEditUtil.FORMAT_DATE))
        celEditText.addTextChangedListener(MaskEditUtil.mask(celEditText, MaskEditUtil.FORMAT_FONE))

        // Configuração do botão para cadastrar um novo cliente
        cadastrarButton.setOnClickListener {
            // Verificar se todos os campos estão preenchidos antes de cadastrar
            if (camposEstaoPreenchidos()) {
                // Alterar o texto do botão para indicar o carregamento
                cadastrarButton.text = "Carregando..."
                cadastrarCliente()
            } else {
                Toast.makeText(this@CadastroActivity, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuração do botão para voltar à tela de login
        loginButton.setOnClickListener {
            val intent = Intent(this@CadastroActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Verifica se todos os campos necessários estão preenchidos
    private fun camposEstaoPreenchidos(): Boolean {
        return nomeEditText.text!!.isNotBlank() &&
                cpfEditText.text!!.isNotBlank() &&
                dtNascEditText.text!!.isNotBlank() &&
                celEditText.text!!.isNotBlank() &&
                emailEditText.text!!.isNotBlank() &&
                senhaEditText.text!!.isNotBlank()
    }

    // Método para cadastrar um novo cliente no servidor
    private fun cadastrarCliente() {
        val nome = nomeEditText.text.toString().trim()
        val cpf = cpfEditText.text.toString().trim()
        val dtNasc = dtNascEditText.text.toString().trim()
        val celular = celEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val senha = senhaEditText.text.toString()

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("nome", nome)
            .add("cpf", cpf)
            .add("dataNascimento", dtNasc)
            .add("celular", celular)
            .add("email", email)
            .add("senha", senha)
            .build()

        val request = Request.Builder()
            .url("https://southamerica-east1-projeto-integrador-3-415419.cloudfunctions.net/funcCadastrarCliente")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Em caso de falha na requisição, exibe mensagem de erro
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erro ao cadastrar cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Trata a resposta do servidor
                val statusCode = response.code()
                if (!response.isSuccessful) {
                    // Se a resposta não for bem-sucedida, exibe mensagem de erro
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Erro ao cadastrar cliente: $statusCode", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                // Lê os dados da resposta
                val responseData = response.body()?.string()
                if (!responseData.isNullOrEmpty()) {
                    // Se a resposta não estiver vazia, exibe os dados e retorna para a tela de login
                    Log.d("CadastroActivity", "Response: $responseData")
                    runOnUiThread {
                        Toast.makeText(applicationContext, responseData, Toast.LENGTH_LONG).show()
                    }

                    val intent = Intent(this@CadastroActivity, LoginActivity::class.java)
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
