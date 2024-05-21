package com.example.projetointegrador3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HistoricoActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e variáveis de controle
    private lateinit var linearLayoutHistorico: LinearLayout
    private lateinit var radioGroupFiltro: RadioGroup
    private lateinit var radioEncerradas: RadioButton
    private lateinit var radioAndamento: RadioButton
    private lateinit var textViewNenhumaLocacao: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        // Inicialização dos componentes da UI
        linearLayoutHistorico = findViewById(R.id.linearLayoutHistorico)
        radioGroupFiltro = findViewById(R.id.radioGroupFiltro)
        radioEncerradas = findViewById(R.id.radio_encerradas)
        radioAndamento = findViewById(R.id.radio_andamento)
        textViewNenhumaLocacao = findViewById(R.id.textViewNenhumaLocacao)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Obtém o usuário atual
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Configura o filtro de radio buttons para carregar locações conforme o status selecionado
            radioGroupFiltro.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_encerradas -> carregarLocacoes(userId, "encerrado")
                    R.id.radio_andamento -> carregarLocacoes(userId, "Em andamento")
                }
            }

            // Seleciona "Em Andamento" por padrão
            radioAndamento.isChecked = true
        } else {
            Toast.makeText(this, "Nenhum usuário logado", Toast.LENGTH_SHORT).show()
        }
    }

    // Carrega as locações do Firestore conforme o status selecionado
    private fun carregarLocacoes(userId: String, status: String) {
        val query = firestore.collection("locacoes")
            .whereEqualTo("uid", userId)
            .whereEqualTo("status", status)

        query.get()
            .addOnSuccessListener { documents ->
                linearLayoutHistorico.removeAllViews()
                if (documents.isEmpty) {
                    textViewNenhumaLocacao.visibility = View.VISIBLE
                } else {
                    textViewNenhumaLocacao.visibility = View.GONE
                    for (document in documents) {
                        val unidade = document.getString("unidadeId") ?: ""
                        val dataHoraInicio = document.getDate("dataHoraInicio") ?: Date()
                        val dataHoraFim = document.getDate("dataHoraFim")
                        val dataHoraFimPrevisto = document.getDate("dataHoraFimPrevisto")
                        val status = document.getString("status") ?: ""
                        val totalPago = document.getDouble("totalAPagar")
                        val numeroArmario = document.getString("numeroArmario") ?: ""
                        val opcao = document.getString("opcao") ?: ""

                        carregarNomeUnidadeEAdicionarCartaoLocacao(unidade, dataHoraInicio, dataHoraFim, dataHoraFimPrevisto, status, totalPago, numeroArmario, opcao)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar histórico: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Carrega o nome da unidade e adiciona um cartão de locação na UI
    private fun carregarNomeUnidadeEAdicionarCartaoLocacao(unidade: String, dataHoraInicio: Date, dataHoraFim: Date?, dataHoraFimPrevisto: Date?, status: String, totalPago: Double?, numeroArmario: String, opcao: String) {
        firestore.collection("unidades").whereEqualTo("id", unidade).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val nomeUnidade = document.getString("nome") ?: "Nome não encontrado"
                    adicionarCartaoLocacao(nomeUnidade, dataHoraInicio, dataHoraFim, dataHoraFimPrevisto, status, totalPago, numeroArmario, opcao)
                } else {
                    adicionarCartaoLocacao("Nome não encontrado", dataHoraInicio, dataHoraFim, dataHoraFimPrevisto, status, totalPago, numeroArmario, opcao)
                }
            }
            .addOnFailureListener { exception ->
                adicionarCartaoLocacao("Erro ao buscar nome", dataHoraInicio, dataHoraFim, dataHoraFimPrevisto, status, totalPago, numeroArmario, opcao)
                Toast.makeText(this, "Erro ao buscar nome da unidade: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Adiciona um cartão de locação na UI
    private fun adicionarCartaoLocacao(unidade: String, dataHoraInicio: Date, dataHoraFim: Date?, dataHoraFimPrevisto: Date?, status: String, totalPago: Double?, numeroArmario: String, opcao: String) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.item_locacao, linearLayoutHistorico, false)

        val unidadeTextView = cardView.findViewById<TextView>(R.id.text_view_unidade)
        val dataInicioTextView = cardView.findViewById<TextView>(R.id.text_view_data_inicio)
        val dataFimTextView = cardView.findViewById<TextView>(R.id.text_view_data_fim)
        val statusTextView = cardView.findViewById<TextView>(R.id.text_view_status)
        val totalPagoTextView = cardView.findViewById<TextView>(R.id.text_view_total_pago)
        val numeroArmarioTextView = cardView.findViewById<TextView>(R.id.text_view_numero_armario)

        unidadeTextView.text = unidade
        dataInicioTextView.text = "Início: ${sdf.format(dataHoraInicio)}"
        numeroArmarioTextView.text = "Armário: $numeroArmario"

        if (status == "encerrado") {
            dataFimTextView.text = "Fim: ${sdf.format(dataHoraFim)}"
            totalPagoTextView.text = "Total pago: R$ $totalPago"
            statusTextView.text = "Opção: $opcao"
        } else {
            val dataHoraAtual = Date()
            if (dataHoraFimPrevisto != null) {
                val diff = dataHoraFimPrevisto.time - dataHoraAtual.time
                val minutes = Math.abs(diff / (1000 * 60))
                if (diff < 0) {
                    dataFimTextView.text = "Tempo ultrapassado: $minutes minutos"
                    dataFimTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                } else {
                    dataFimTextView.text = "Tempo restante: $minutes minutos"
                }
            } else {
                dataFimTextView.text = "Horário não encontrado"
            }
            totalPagoTextView.visibility = View.GONE
            statusTextView.text = "Opção: $opcao"
        }

        linearLayoutHistorico.addView(cardView)
    }
}
