package com.example.projetointegrador3

import android.content.Intent
import android.graphics.Color
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

    // Declaração de variáveis para os componentes da UI e controle
    private lateinit var linearLayoutHistorico: LinearLayout
    private lateinit var radioGroupFiltro: RadioGroup
    private lateinit var radioEncerradas: RadioButton
    private lateinit var radioAndamento: RadioButton
    private lateinit var backButton: ImageButton
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
        backButton = findViewById(R.id.back_button)
        radioEncerradas = findViewById(R.id.radio_encerradas)
        radioAndamento = findViewById(R.id.radio_andamento)
        textViewNenhumaLocacao = findViewById(R.id.textViewNenhumaLocacao)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configura o botão de voltar para retornar à tela principal
        backButton.setOnClickListener {
            val intent = Intent(this@HistoricoActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Obtém o usuário atual autenticado
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
                // Limpa a visualização anterior
                linearLayoutHistorico.removeAllViews()
                if (documents.isEmpty) {
                    textViewNenhumaLocacao.visibility = View.VISIBLE
                } else {
                    textViewNenhumaLocacao.visibility = View.GONE
                    for (document in documents) {
                        // Obtém os dados da locação
                        val unidade = document.getString("unidadeId") ?: ""
                        val dataHoraInicio = document.getDate("dataHoraInicio") ?: Date()
                        val dataHoraFim = document.getDate("dataHoraFim")
                        val dataHoraFimPrevisto = document.getDate("dataHoraFimPrevisto")
                        val status = document.getString("status") ?: ""
                        val totalPago = document.getDouble("totalAPagar")
                        val numeroArmario = document.getString("numeroArmario") ?: ""
                        val opcao = document.getString("opcao") ?: ""
                        val valorEstorno = document.getDouble("valorEstorno")
                        val valorTotalSemEstorno = document.getDouble("valorTotalSemEstorno")

                        // Carrega o nome da unidade e adiciona o cartão de locação na UI
                        carregarNomeUnidadeEAdicionarCartaoLocacao(
                            unidade, numeroArmario, dataHoraInicio, dataHoraFim, dataHoraFimPrevisto,
                            status, totalPago, opcao, valorEstorno, valorTotalSemEstorno
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar histórico: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Carrega o nome da unidade e detalhes do armário, adiciona um cartão de locação na UI
    private fun carregarNomeUnidadeEAdicionarCartaoLocacao(
        unidade: String, numeroArmario: String, dataHoraInicio: Date, dataHoraFim: Date?,
        dataHoraFimPrevisto: Date?, status: String, totalPago: Double?, opcao: String,
        valorEstorno: Double?, valorTotalSemEstorno: Double?
    ) {
        firestore.collection("unidades").whereEqualTo("id", unidade).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val nomeUnidade = document.getString("nome") ?: "Nome não encontrado"
                    val armarios = document.get("armarios") as Map<String, Map<String, Any>>
                    val detalhes = armarios[numeroArmario]?.get("detalhes") as? String ?: "Detalhes não encontrados"
                    adicionarCartaoLocacao(
                        nomeUnidade, detalhes, dataHoraInicio, dataHoraFim, dataHoraFimPrevisto,
                        status, totalPago, opcao, valorEstorno, valorTotalSemEstorno, numeroArmario
                    )
                } else {
                    adicionarCartaoLocacao(
                        "Nome não encontrado", "Detalhes não encontrados", dataHoraInicio,
                        dataHoraFim, dataHoraFimPrevisto, status, totalPago, opcao,
                        valorEstorno, valorTotalSemEstorno, numeroArmario
                    )
                }
            }
            .addOnFailureListener { exception ->
                adicionarCartaoLocacao(
                    "Erro ao buscar nome", "Erro ao buscar detalhes", dataHoraInicio, dataHoraFim,
                    dataHoraFimPrevisto, status, totalPago, opcao, valorEstorno,
                    valorTotalSemEstorno, numeroArmario
                )
                Toast.makeText(this, "Erro ao buscar nome da unidade: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Adiciona um cartão de locação na UI
    private fun adicionarCartaoLocacao(
        unidade: String, detalhes: String, dataHoraInicio: Date, dataHoraFim: Date?,
        dataHoraFimPrevisto: Date?, status: String, totalPago: Double?, opcao: String,
        valorEstorno: Double?, valorTotalSemEstorno: Double?, numeroArmario: String
    ) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.item_locacao, linearLayoutHistorico, false)

        // Inicializa os componentes do cardView
        val unidadeTextView = cardView.findViewById<TextView>(R.id.text_view_unidade)
        val dataInicioTextView = cardView.findViewById<TextView>(R.id.text_view_data_inicio)
        val dataFimTextView = cardView.findViewById<TextView>(R.id.text_view_data_fim)
        val statusTextView = cardView.findViewById<TextView>(R.id.text_view_status)
        val totalPagoTextView = cardView.findViewById<TextView>(R.id.text_view_total_pago)
        val valorEstornoTextView = cardView.findViewById<TextView>(R.id.text_view_valor_estorno)
        val numeroArmarioTextView = cardView.findViewById<TextView>(R.id.text_view_numero_armario)
        val mensagemAndamentoTextView = cardView.findViewById<TextView>(R.id.text_view_mensagem_andamento)
        val detalhesTextView = cardView.findViewById<TextView>(R.id.text_view_detalhes)

        // Preenche os campos do cardView com os dados da locação
        unidadeTextView.text = unidade
        dataInicioTextView.text = "Início: ${sdf.format(dataHoraInicio)}"
        numeroArmarioTextView.text = "Armário: $numeroArmario"

        if (status == "encerrado") {
            dataFimTextView.text = "Fim: ${sdf.format(dataHoraFim)}"
            totalPagoTextView.text = "Total pago: R$ $totalPago"
            valorEstornoTextView.text = "Valor estorno: R$ $valorEstorno"
            statusTextView.text = "Opção: $opcao"
            mensagemAndamentoTextView.visibility = View.GONE
            detalhesTextView.visibility = View.GONE
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
            valorEstornoTextView.visibility = View.GONE
            statusTextView.text = "Opção: $opcao"
            detalhesTextView.text = detalhes
            mensagemAndamentoTextView.text = "Valores aparecerão após o encerramento da locação."
            mensagemAndamentoTextView.setTextColor(Color.RED)
        }

        // Adiciona o cardView ao linearLayoutHistorico
        linearLayoutHistorico.addView(cardView)
    }
}
