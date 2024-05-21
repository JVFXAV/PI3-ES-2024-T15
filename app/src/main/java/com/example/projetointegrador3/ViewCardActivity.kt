package com.example.projetointegrador3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewCardActivity : AppCompatActivity() {

    private lateinit var numCartaoTextView: TextView
    private lateinit var nomeCartaoTextView: TextView
    private lateinit var validadeCartaoTextView: TextView
    private lateinit var excluirCartaoButton: ImageButton
    private lateinit var adicionarNovoCartaoButton: Button
    private lateinit var voltarButton: ImageButton

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_viewer)

        // Inicialização de componentes de interface do usuário
        numCartaoTextView = findViewById(R.id.numero_cartao_text_view)
        nomeCartaoTextView = findViewById(R.id.nome_titular_text_view)
        validadeCartaoTextView = findViewById(R.id.validade_text_view)
        excluirCartaoButton = findViewById(R.id.delete_button)
        adicionarNovoCartaoButton = findViewById(R.id.adicionar_novo_cartao_button)
        voltarButton = findViewById(R.id.back_button)

        firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        // Verifica se o usuário está autenticado e obtém os dados do cartão do Firestore
        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            val cartaoRef = firestore.collection("pessoas").document(userId)
            cartaoRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val cartaoCredito = document.get("cartaoCredito") as? Map<*, *>
                        if (cartaoCredito != null) {
                            val numero = cartaoCredito["numeroCartao"] as? String
                            val nomeTitular = cartaoCredito["nomeTitular"] as? String
                            val dataExpiracao = cartaoCredito["dataExpiracao"] as? String
                            numCartaoTextView.text = "Número Cartão: $numero"
                            nomeCartaoTextView.text = "Nome do Titular: $nomeTitular"
                            validadeCartaoTextView.text = "Validade: $dataExpiracao"
                        } else {
                            Log.d("ViewCardActivity", "Dados do cartão de crédito não encontrados")
                        }
                    } else {
                        Log.d("ViewCardActivity", "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("ViewCardActivity", "Erro ao recuperar dados do cartão", exception)
                    Toast.makeText(this@ViewCardActivity, "Erro ao recuperar dados do cartão", Toast.LENGTH_SHORT).show()
                }
        }

        excluirCartaoButton.setOnClickListener {
            // Cria um AlertDialog.Builder
            val builder = AlertDialog.Builder(this@ViewCardActivity)
            builder.setTitle("Confirmar exclusão")
            builder.setMessage("Tem certeza que deseja excluir o cartão de crédito?")

            builder.setPositiveButton("Sim") { _, _ ->
                // Implementação para excluir o cartão do banco de dados
                val firestore = FirebaseFirestore.getInstance()
                val cartaoRef = firestore.collection("pessoas").document(userId!!)
                cartaoRef.update("cartaoCredito", null)
                    .addOnSuccessListener {
                        Toast.makeText(this@ViewCardActivity, "Cartão excluído com sucesso", Toast.LENGTH_SHORT).show()
                        numCartaoTextView.text = "Nenhum cartão cadastrado até o momento..."
                        nomeCartaoTextView.text = ""
                        validadeCartaoTextView.text = ""
                    }
                    .addOnFailureListener { exception ->
                        Log.d("ViewCardActivity", "Erro ao excluir cartão", exception)
                        Toast.makeText(this@ViewCardActivity, "Erro ao excluir cartão", Toast.LENGTH_SHORT).show()
                    }
            }
            builder.setNegativeButton("Cancelar") { _, _ ->
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        adicionarNovoCartaoButton.setOnClickListener {
            val intent = Intent(this@ViewCardActivity, CardActivity::class.java)
            startActivity(intent)
        }

        voltarButton.setOnClickListener {
            val intent = Intent(this@ViewCardActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
