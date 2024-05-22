package com.example.projetointegrador3

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList

class DetalhesFotoActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e controle
    private lateinit var tvNomeUsuario: TextView
    private lateinit var tvCelularUsuario: TextView
    private lateinit var tvOpcao: TextView
    private lateinit var layoutImages: LinearLayout
    private lateinit var btnSalvarLocacao: Button

    private var numeroArmarioDisponivel: String? = null
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var alertDialog: AlertDialog
    private var locacaoId: String? = null
    private var unidadeId: String? = null

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_fotos)

        // Inicialização dos componentes da UI
        tvNomeUsuario = findViewById(R.id.tvNomeUsuario)
        tvCelularUsuario = findViewById(R.id.tvCelularUsuario)
        tvOpcao = findViewById(R.id.tvOpcao)
        layoutImages = findViewById(R.id.layoutImages)

        // Obtém os dados passados pela Intent
        unidadeId = intent.getStringExtra("unidadeId")
        val uid = intent.getStringExtra("uid") ?: ""
        val opcao = intent.getStringExtra("opcao") ?: ""
        val fotosUri = intent.getParcelableArrayListExtra<Uri>("fotosUri") ?: arrayListOf()

        // Exibe a opção escolhida
        tvOpcao.text = "Opção Escolhida: $opcao"

        // Busca dados do usuário e da unidade
        buscarDadosUsuario(uid)
        buscarDadosUnidadePorCampoId(unidadeId!!)
        mostrarFotos(fotosUri)

        // Configura o botão para salvar a locação
        btnSalvarLocacao = findViewById(R.id.btnSalvarLocacao)
        btnSalvarLocacao.setOnClickListener {
            btnSalvarLocacao.text = "Carregando..."

            salvarFotosNoStorage(fotosUri) { urlsFotos ->
                salvarLocacao(unidadeId!!, uid, opcao, urlsFotos)
            }
        }

        // Inicializa o adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    // Extensão para converter dp para pixels
    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density + 0.5f).toInt()

    // Exibe as fotos na UI
    private fun mostrarFotos(fotosUri: ArrayList<Uri>) {
        for (uri in fotosUri) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    200.dpToPx(this@DetalhesFotoActivity), // Convert dp to px
                    200.dpToPx(this@DetalhesFotoActivity)
                ).apply {
                    setMargins(5.dpToPx(this@DetalhesFotoActivity), 0, 5.dpToPx(this@DetalhesFotoActivity), 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
            layoutImages.addView(imageView)
        }
    }

    // Busca dados do usuário no Firestore e preenche os campos da UI
    private fun buscarDadosUsuario(uid: String) {
        FirebaseFirestore.getInstance().collection("pessoas").document(uid).get()
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

    // Busca dados da unidade no Firestore e atualiza o status dos armários
    private fun buscarDadosUnidadePorCampoId(campoId: String) {
        FirebaseFirestore.getInstance().collection("unidades")
            .whereEqualTo("id", campoId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvOpcao.text = "Nenhuma unidade encontrada com o ID: $campoId"
                } else {
                    val document = documents.documents.first()
                    val armarios = document.data?.get("armarios") as? Map<String, Map<String, Any>>
                    armarios?.let {
                        var found = false
                        for ((numeroArmario, detalhesArmario) in it) {
                            if (detalhesArmario["status"] == "disponivel") {
                                val detalhes = detalhesArmario["detalhes"] as? String
                                tvOpcao.append("\nDetalhes: $detalhes")
                                tvOpcao.append("\nArmário $numeroArmario disponível")
                                numeroArmarioDisponivel = numeroArmario // Salvar o número do armário disponível
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            tvOpcao.append("\nNenhum armário disponível")
                            numeroArmarioDisponivel = null
                        }
                    } ?: run {
                        tvOpcao.append("\nNenhum armário disponível")
                        numeroArmarioDisponivel = null
                    }
                }
            }
            .addOnFailureListener { e ->
                tvOpcao.text = "Erro ao buscar unidade: ${e.message}"
                numeroArmarioDisponivel = null
            }
    }

    // Salva as fotos no Firebase Storage e retorna as URLs das fotos salvas
    private fun salvarFotosNoStorage(fotosUri: ArrayList<Uri>, onComplete: (List<String>) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val urls = mutableListOf<String>()
        val expectedUploads = fotosUri.size
        var completedUploads = 0

        for (uri in fotosUri) {
            val fotoRef = storageRef.child("fotos/${uri.lastPathSegment}")
            fotoRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    fotoRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        urls.add(downloadUrl)
                    } else {
                        Toast.makeText(this, "Falha ao salvar foto: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                    completedUploads++
                    if (completedUploads == expectedUploads) {
                        onComplete(urls)
                    }
                }
        }
    }

    // Calcula a data e hora de término da locação com base na opção escolhida
    private fun calcularDataHoraFim(dataInicio: Date, opcao: String): Date {
        val calendar = Calendar.getInstance()
        calendar.time = dataInicio

        when (opcao) {
            "30min" -> calendar.add(Calendar.MINUTE, 30)
            "1h" -> calendar.add(Calendar.HOUR, 1)
            "2h" -> calendar.add(Calendar.HOUR, 2)
            "dia" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 18)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }
        return calendar.time
    }

    // Salva os dados da locação no Firestore e atualiza o status do armário
    private fun salvarLocacao(unidadeId: String, uid: String, opcao: String, urlsFotos: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val dataInicio = Date()
        val dataFim = calcularDataHoraFim(dataInicio, opcao)

        // Buscar o valor da diária
        db.collection("unidades").whereEqualTo("id", unidadeId).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val valorDiaria = document.get("opcoesLocacao.dia") as? Number ?: 0

                    // Cria um mapa de dados para a locação
                    val locacaoMap = hashMapOf(
                        "unidadeId" to unidadeId,
                        "uid" to uid,
                        "opcao" to opcao,
                        "dataHoraInicio" to dataInicio,
                        "dataHoraFimPrevisto" to dataFim,
                        "fotosUrls" to urlsFotos,
                        "numeroArmario" to numeroArmarioDisponivel,
                        "status" to "Em andamento",
                        "valorTotalSemEstorno" to valorDiaria.toDouble()
                    )

                    // Salva os dados da locação no Firestore
                    db.collection("locacoes").add(locacaoMap)
                        .addOnSuccessListener { documentReference ->
                            numeroArmarioDisponivel?.let {
                                atualizarStatusArmario(unidadeId, it, "ocupado")
                            }
                            locacaoId = documentReference.id
                            mostrarDialogoNFC()
                        }
                }
            }
    }

    // Exibe um diálogo solicitando ao usuário que aproxime a pulseira NFC para registrar a locação
    private fun mostrarDialogoNFC() {
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC não está disponível ou habilitado", Toast.LENGTH_LONG).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Aproxime a pulseira NFC")
        val progressBar = ProgressBar(this)
        dialog.setView(progressBar)
        dialog.setCancelable(false)
        alertDialog = dialog.show()
    }

    // Método onResume é chamado quando a atividade é retomada
    override fun onResume() {
        super.onResume()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nfcIntentFilter = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null)
    }

    // Método onPause é chamado quando a atividade é pausada
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Método onNewIntent é chamado quando a atividade recebe uma nova Intent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                writeNfcTag(ndef)
            } else {
                Toast.makeText(this, "Tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Escreve os dados da locação na tag NFC
    private fun writeNfcTag(ndef: Ndef) {
        try {
            ndef.connect()
            val ndefRecord = NdefRecord.createTextRecord(null, locacaoId)
            val ndefMessage = NdefMessage(ndefRecord)
            ndef.writeNdefMessage(ndefMessage)
            ndef.close()

            runOnUiThread {
                alertDialog.dismiss()
                val checkDialog = AlertDialog.Builder(this)
                checkDialog.setTitle("Sucesso")
                checkDialog.setMessage("Locação registrada na pulseira. Redirecionando em 5 segundos...")
                checkDialog.setCancelable(false)
                checkDialog.show()

                // Redireciona para a tela de opções do gerente após 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, GerenteOptionsActivity::class.java)
                    intent.putExtra("unidadeId", unidadeId)
                    startActivity(intent)
                    finish()
                }, 5000)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao escrever na tag NFC: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Atualiza o status do armário no Firestore
    private fun atualizarStatusArmario(unidadeId: String, armarioId: String, novoStatus: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("unidades").whereEqualTo("id", unidadeId).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val docId = document.id
                    db.collection("unidades").document(docId)
                        .update("armarios.$armarioId.status", novoStatus)
                        .addOnSuccessListener {
                            Log.d("Update", "Status do armário atualizado com sucesso")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Update", "Erro ao atualizar status do armário", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Update", "Erro ao buscar unidade", e)
            }
    }
}
