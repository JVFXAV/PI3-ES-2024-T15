package com.example.projetointegrador3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import java.io.IOException

class CardActivity : AppCompatActivity() {

    private lateinit var numCartao: EditText
    private lateinit var nomeCartao: EditText
    private lateinit var validadeCartao: EditText
    private lateinit var cvvCartao: EditText
    private lateinit var cadastrarCartaoButton: Button
    private lateinit var voltarButton: ImageButton

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Inicialização de componentes de interface do usuário
        numCartao = findViewById(R.id.numCartao_edit_text)
        nomeCartao = findViewById(R.id.nomeCartao_edit_text)
        validadeCartao = findViewById(R.id.validade_edit_text)
        cvvCartao = findViewById(R.id.cvv_edit_text)
        voltarButton = findViewById(R.id.back_button)
        cadastrarCartaoButton = findViewById(R.id.cadastrarCartao_edit_text)

        // Aplicação de máscaras aos campos de entrada
        numCartao.addTextChangedListener(MaskEditUtil.mask(numCartao, MaskEditUtil.FORMAT_NUMBER_CARD))
        validadeCartao.addTextChangedListener(MaskEditUtil.mask(validadeCartao, MaskEditUtil.FORMAT_DATE_CARD))
        cvvCartao.addTextChangedListener(MaskEditUtil.mask(cvvCartao, MaskEditUtil.FORMAT_CVV))

        firebaseAuth = FirebaseAuth.getInstance()

        cadastrarCartaoButton.setOnClickListener {
            // Verificar se todos os campos estão preenchidos antes de cadastrar o cartão
            if (camposEstaoPreenchidos()) {
                // Alterar o texto do botão para indicar o carregamento
                cadastrarCartaoButton.text = "Carregando..."
                cadastrarCartao()
            } else {
                Toast.makeText(this@CardActivity, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        voltarButton.setOnClickListener {
            // Voltar para a tela inicial ao clicar no botão "Voltar"
            val intent = Intent(this@CardActivity, ViewCardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Verifica se todos os campos necessários estão preenchidos
    private fun camposEstaoPreenchidos(): Boolean {
        return numCartao.text!!.isNotBlank() &&
                nomeCartao.text!!.isNotBlank() &&
                validadeCartao.text!!.isNotBlank() &&
                cvvCartao.text!!.isNotBlank()
    }

    // Método para cadastrar o cartão no servidor
    private fun cadastrarCartao() {
        val nome = nomeCartao.text.toString().trim()
        val numero = numCartao.text.toString().trim()
        val validade = validadeCartao.text.toString().trim()
        val cvv = cvvCartao.text.toString().trim()

        val currentUser = firebaseAuth.currentUser
        val userId = currentUser!!.uid

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("userId", userId)
            .add("numeroCartao", numero)
            .add("nomeTitular", nome)
            .add("dataExpiracao", validade)
            .add("cvv", cvv)
            .build()

        val request = Request.Builder()
            .url("https://southamerica-east1-projeto-integrador-3-415419.cloudfunctions.net/funcCadastrarCartao")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Em caso de falha na requisição, exibe mensagem de erro
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "Erro ao cadastrar Cartão: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Trata a resposta do servidor
                val statusCode = response.code()
                if (!response.isSuccessful) {
                    // Se a resposta não for bem-sucedida, exibe mensagem de erro
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Erro ao cadastrar Cartão: $statusCode", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                // Lê os dados da resposta
                val responseData = response.body()?.string()
                if (!responseData.isNullOrEmpty()) {
                    // Se a resposta não estiver vazia, exibe os dados e retorna para a tela inicial
                    Log.d("CardActivity", "Response: $responseData")
                    runOnUiThread {
                        Toast.makeText(applicationContext, responseData, Toast.LENGTH_LONG).show()
                    }

                    val intent = Intent(this@CardActivity, HomeActivity::class.java)
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
