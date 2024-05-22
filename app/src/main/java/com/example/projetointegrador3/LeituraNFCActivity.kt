package com.example.projetointegrador3

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.ExifInterface
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.projetointegrador3.R.id.dialog_imageview
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class LeituraNFCActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e variáveis de controle
    private lateinit var tvStatus: TextView
    private lateinit var tvNomeUsuario: TextView
    private lateinit var tvCelularUsuario: TextView
    private lateinit var tvOpcao: TextView
    private lateinit var layoutImages: LinearLayout
    private lateinit var btnEncerrarLocacao: Button
    private lateinit var btnAbrirArmario: Button
    private lateinit var backButton: ImageButton

    private var nfcAdapter: NfcAdapter? = null
    private var loadingDialog: AlertDialog? = null
    private var locacaoId: String? = null
    private var numeroArmarioDisponivel: String? = null
    private var dataHoraInicio: Date? = null
    private var dataHoraFim: Date? = null
    private var dataHoraFimPrevisto: Date? = null
    private var totalAPagar: Double? = null

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leitura_nfc)

        // Inicialização dos componentes da UI
        tvStatus = findViewById(R.id.tvStatus)
        tvNomeUsuario = findViewById(R.id.tvNomeUsuario)
        tvCelularUsuario = findViewById(R.id.tvCelularUsuario)
        tvOpcao = findViewById(R.id.tvOpcao)
        layoutImages = findViewById(R.id.layoutImages)
        btnEncerrarLocacao = findViewById(R.id.btnEncerrarLocacao)
        btnAbrirArmario = findViewById(R.id.btnAbrirArmario)
        backButton = findViewById(R.id.back_button)

        // Obtém o ID da locação passado pela Intent
        locacaoId = intent.getStringExtra("locacaoId")
        if (locacaoId != null) {
            buscarDadosLocacao(locacaoId!!)
        } else {
            Toast.makeText(this, "ID da locação não encontrado", Toast.LENGTH_SHORT).show()
        }

        // Configura o botão para encerrar a locação
        btnEncerrarLocacao.setOnClickListener {
            showConfirmationDialog()
        }

        // Configura o botão para abrir o armário
        btnAbrirArmario.setOnClickListener {
            showOpenLockerDialog()
        }

        // Obtém o adaptador NFC do dispositivo
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Configura o botão de voltar
        backButton.setOnClickListener { finish() }
    }

    // Método onResume é chamado quando a atividade é retomada
    override fun onResume() {
        super.onResume()
        // Configuração do PendingIntent para captura de tags NFC
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Configura os filtros de intent para diferentes ações NFC
        val nfcIntentFilter = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
        // Ativa o dispatch em foreground para captura de tags NFC
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null)
    }

    // Método onPause é chamado quando a atividade é pausada
    override fun onPause() {
        super.onPause()
        // Desativa o dispatch em foreground para NFC
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Método onNewIntent é chamado quando a atividade recebe uma nova Intent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Obtém a tag NFC da intent
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                cleanNfcTag(ndef)
            } else {
                Toast.makeText(this, "Tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Exibe um diálogo de confirmação para encerrar a locação
    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Encerrar Locação")
        builder.setMessage("Tem certeza de que deseja encerrar a locação? Isso adicionará uma data de término e calculará o valor a ser pago.")
        builder.setPositiveButton("Sim") { _, _ ->
            showLoadingDialog()
        }
        builder.setNegativeButton("Não", null)
        builder.show()
    }

    // Exibe um diálogo para confirmar a abertura do armário
    private fun showOpenLockerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Armário Aberto")
        builder.setMessage("O armário foi aberto corretamente?")
        builder.setPositiveButton("Não, tentar novamente") { _, _ ->
            showOpenLockerDialog()
        }
        builder.setNegativeButton("Sim") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // Exibe um diálogo de carregamento enquanto espera a aproximação da tag NFC
    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aproxime a pulseira NFC")
        builder.setMessage("Esperando pela aproximação da tag NFC... A tag será limpa e a locação encerrada.")
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    // Limpa a tag NFC e encerra a locação
    private fun cleanNfcTag(ndef: Ndef) {
        try {
            ndef.connect()
            val emptyNdefRecord = NdefRecord.createTextRecord(null, "")
            val emptyNdefMessage = NdefMessage(emptyNdefRecord)
            ndef.writeNdefMessage(emptyNdefMessage)
            ndef.close()

            // Encerrar locação no Firestore
            encerrarLocacao()

            loadingDialog?.dismiss()
            Toast.makeText(this, "Tag NFC limpa e locação encerrada com sucesso", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            loadingDialog?.dismiss()
            Toast.makeText(this, "Falha ao limpar a tag NFC: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Encerra a locação no Firestore e atualiza os dados da locação
    private fun encerrarLocacao() {
        val db = FirebaseFirestore.getInstance()
        dataHoraFim = Date()
        locacaoId?.let {
            db.collection("locacoes").document(it).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtém dados da locação do Firestore
                    dataHoraInicio = document.getDate("dataHoraInicio")
                    dataHoraFimPrevisto = document.getDate("dataHoraFimPrevisto")
                    val opcao = document.getString("opcao")
                    val numeroArmario = document.getString("numeroArmario")
                    val unidadeId = document.getString("unidadeId")
                    val valorTotalSemEstorno = document.getDouble("valorTotalSemEstorno") ?: 0.0

                    if (dataHoraInicio != null && opcao != null && numeroArmario != null && unidadeId != null) {
                        // Calcula o valor total a pagar
                        calcularTotalAPagar(unidadeId, dataHoraInicio!!, dataHoraFim!!, opcao) { totalLocacao ->
                            totalAPagar = totalLocacao

                            val valorEstorno = valorTotalSemEstorno - totalLocacao
                            val updates = hashMapOf(
                                "dataHoraFim" to dataHoraFim,
                                "totalAPagar" to totalLocacao,
                                "status" to "encerrado",
                                "valorEstorno" to valorEstorno,
                                "valorTotalComEstorno" to (valorTotalSemEstorno - valorEstorno)
                            )

                            // Atualiza os dados da locação no Firestore
                            db.collection("locacoes").document(it).update(updates as Map<String, Any>)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Locação encerrada com sucesso", Toast.LENGTH_SHORT).show()
                                    // Atualiza o status do armário
                                    atualizarStatusArmario(unidadeId, numeroArmario)
                                    // Mostra um resumo da locação
                                    mostrarResumoLocacao(dataHoraInicio!!, dataHoraFimPrevisto!!, dataHoraFim!!, totalLocacao, valorEstorno)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Erro ao encerrar locação: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Erro: dados da locação estão incompletos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Locação não encontrada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Atualiza o status do armário no Firestore para "disponível"
    private fun atualizarStatusArmario(unidadeId: String, numeroArmario: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("unidades").whereEqualTo("id", unidadeId).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val docId = document.id
                    db.collection("unidades").document(docId)
                        .update("armarios.$numeroArmario.status", "disponivel")
                        .addOnSuccessListener {
                            Toast.makeText(this, "Status do armário atualizado com sucesso", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao atualizar status do armário: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar unidade: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Calcula o total a pagar com base na duração da locação e nas opções de preços da unidade
    private fun calcularTotalAPagar(unidadeId: String, dataHoraInicio: Date, dataHoraFim: Date, opcao: String, callback: (Double) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("unidades").whereEqualTo("id", unidadeId).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val opcoesLocacao = document.get("opcoesLocacao") as Map<String, Number>
                    val diff = dataHoraFim.time - dataHoraInicio.time
                    val diffMinutes = diff / (1000 * 60)

                    // Calcula o valor baseado na duração da locação
                    val valorDiaria = (opcoesLocacao["dia"]?.toDouble()) ?: 0.0
                    val valorLocacao = when {
                        diffMinutes <= 30 -> (opcoesLocacao["30min"]?.toDouble()) ?: 0.0
                        diffMinutes <= 60 -> (opcoesLocacao["1h"]?.toDouble()) ?: 0.0
                        diffMinutes <= 120 -> (opcoesLocacao["2h"]?.toDouble()) ?: 0.0
                        else -> valorDiaria
                    }

                    callback(valorLocacao)
                } else {
                    Toast.makeText(this, "Erro ao buscar preços da unidade", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar preços da unidade: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Exibe um resumo da locação encerrada
    private fun mostrarResumoLocacao(dataHoraInicio: Date, dataHoraFimPrevisto: Date, dataHoraFim: Date, totalAPagar: Double, valorEstorno: Double) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_resumo_locacao, null)
        val builder = AlertDialog.Builder(this).setView(dialogView)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val tvResumoInicio: TextView = dialogView.findViewById(R.id.tvResumoInicio)
        val tvResumoPrevisto: TextView = dialogView.findViewById(R.id.tvResumoPrevisto)
        val tvResumoFim: TextView = dialogView.findViewById(R.id.tvResumoFim)
        val tvResumoTotal: TextView = dialogView.findViewById(R.id.tvResumoTotal)
        val tvResumoEstorno: TextView = dialogView.findViewById(R.id.tvResumoEstorno)

        // Preenche os campos do resumo com os dados da locação
        tvResumoInicio.text = "Horário de Início: ${sdf.format(dataHoraInicio)}"
        tvResumoPrevisto.text = "Horário Previsto para Término: ${sdf.format(dataHoraFimPrevisto)}"
        tvResumoFim.text = "Horário de Término: ${sdf.format(dataHoraFim)}"
        tvResumoTotal.text = "Total a Pagar: R$ $totalAPagar"
        tvResumoEstorno.text = "Valor Estorno: R$ $valorEstorno"

        builder.setPositiveButton("Retornar") { _, _ ->
            finish()
        }
        builder.show()
    }

    // Busca os dados da locação no Firestore e preenche os campos da UI
    private fun buscarDadosLocacao(locacaoId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val uid = document.getString("uid")
                    val opcao = document.getString("opcao")
                    val fotosUrls = document.get("fotosUrls") as List<String>
                    numeroArmarioDisponivel = document.getString("numeroArmario")

                    tvOpcao.text = "Opção Escolhida: $opcao"
                    mostrarFotos(fotosUrls)
                    uid?.let { buscarDadosUsuario(it) }
                } else {
                    tvStatus.text = "Locação não encontrada"
                }
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Erro ao buscar locação: ${e.message}"
            }
    }

    // Busca os dados do usuário no Firestore e preenche os campos da UI
    private fun buscarDadosUsuario(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("pessoas").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    tvNomeUsuario.text = document.getString("nome")
                    tvCelularUsuario.text = document.getString("celular")
                } else {
                    tvNomeUsuario.text = "Usuário não encontrado"
                }
            }
            .addOnFailureListener { e ->
                tvNomeUsuario.text = "Erro ao buscar usuário: ${e.message}"
            }
    }

    // Exibe as fotos da locação na UI
    private fun mostrarFotos(fotosUrls: List<String>) {
        layoutImages.removeAllViews()
        for (url in fotosUrls) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    200.dpToPx(this@LeituraNFCActivity), // Convert dp to px
                    200.dpToPx(this@LeituraNFCActivity)
                ).apply {
                    setMargins(5.dpToPx(this@LeituraNFCActivity), 0, 5.dpToPx(this@LeituraNFCActivity), 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener {
                    showImageDialog(url)
                }
                // Carrega a imagem com o Picasso
                Picasso.get().load(url).into(this, object : Callback {
                    override fun onSuccess() {
                        // Corrige a orientação da imagem
                        correctImageOrientation(this@apply, url)
                    }
                    override fun onError(e: Exception?) {
                        Toast.makeText(this@LeituraNFCActivity, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            layoutImages.addView(imageView)
        }
    }

    // Corrige a orientação da imagem com base nos dados EXIF
    private fun correctImageOrientation(imageView: ImageView, imageUrl: String) {
        try {
            val uri = Uri.parse(imageUrl)
            val inputStream = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            imageView.rotation = rotation.toFloat()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Exibe um diálogo com a imagem em tamanho maior
    private fun showImageDialog(imageUrl: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_image, null)
        dialogBuilder.setView(dialogView)

        val imageView = dialogView.findViewById<ImageView>(dialog_imageview)
        Picasso.get().load(imageUrl).into(imageView)

        dialogBuilder.setPositiveButton("Fechar") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    // Extensão para converter dp para pixels
    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density + 0.5f).toInt()
}
