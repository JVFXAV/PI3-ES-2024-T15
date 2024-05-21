package com.example.projetointegrador3

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class GerenteOptionsActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e variáveis de controle
    private lateinit var btnLiberarLocacao: Button
    private lateinit var btnLerNFC: Button
    private lateinit var logout: ImageButton
    private lateinit var nomeTextView: TextView
    private lateinit var infosGerenteTextView: TextView
    private lateinit var managerNameTextView: TextView
    private lateinit var managerContactTextView: TextView
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val CAMERA_REQUEST_CODE = 101
    }

    private var nfcAdapter: NfcAdapter? = null
    private var loadingDialog: AlertDialog? = null

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gerente_options)

        // Inicialização dos componentes da UI
        btnLiberarLocacao = findViewById(R.id.btnLiberarLocacao)
        btnLerNFC = findViewById(R.id.btnLerNFC)
        logout = findViewById(R.id.logout_button)
        nomeTextView = findViewById(R.id.nome_text_view)
        infosGerenteTextView = findViewById(R.id.tvinfosGerente)
        managerNameTextView = findViewById(R.id.manager_name)
        managerContactTextView = findViewById(R.id.manager_contact)

        // Obtém o ID da unidade passado pela Intent
        val unidadeId = intent.getStringExtra("unidadeId")

        if (unidadeId != null) {
            carregarInformacoesGerente(unidadeId)
        }

        // Configura o botão para liberar locação
        btnLiberarLocacao.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            } else {
                if (unidadeId != null) {
                    startCameraActivity(unidadeId)
                } else {
                    Toast.makeText(this, "UnidadeId não encontrada", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Configura o botão para ler NFC
        btnLerNFC.setOnClickListener {
            showLoadingDialog()
        }

        // Configura o botão de logout
        logout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
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
                readNfcTag(ndef)
            } else {
                Toast.makeText(this, "Tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Lê os dados da tag NFC
    private fun readNfcTag(ndef: Ndef) {
        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            if (ndefMessage == null || ndefMessage.records.isEmpty()) {
                ndef.close()
                loadingDialog?.dismiss()
                showEmptyTagDialog()
                return
            }
            val ndefRecord = ndefMessage.records[0]
            val message = String(ndefRecord.payload, Charsets.UTF_8)
            val locacaoId = message.substring(3)
            ndef.close()

            loadingDialog?.dismiss()
            verificarLocacaoNoBanco(locacaoId)
        } catch (e: Exception) {
            loadingDialog?.dismiss()
            Toast.makeText(this, "NFC disponível para uso", Toast.LENGTH_SHORT).show()
        }
    }

    // Exibe um diálogo de carregamento enquanto espera a aproximação da tag NFC
    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aproxime a pulseira NFC")
        builder.setMessage("Esperando pela aproximação da tag NFC...")
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    // Exibe um diálogo informando que a tag NFC está vazia
    private fun showEmptyTagDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tag NFC Vazia")
        builder.setMessage("A tag NFC está vazia. Não há nenhuma informação para exibir.")
        builder.setPositiveButton("Ok", null)
        builder.show()
    }

    // Verifica a locação no Firestore com base no ID da locação
    private fun verificarLocacaoNoBanco(locacaoId: String) {
        firestore.collection("locacoes").document(locacaoId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    showConfirmationDialog(locacaoId)
                } else {
                    showTagNotFoundDialog(locacaoId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar locação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Exibe um diálogo informando que a locação não foi encontrada e oferece a opção de limpar a pulseira
    private fun showTagNotFoundDialog(locacaoId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Locação Não Encontrada")
        builder.setMessage("Informação não encontrada em nosso banco de dados. Deseja limpar a pulseira?")
        builder.setPositiveButton("Sim") { _, _ ->
            showCleaningDialog()
        }
        builder.setNegativeButton("Não", null)
        builder.show()
    }

    // Exibe um diálogo de carregamento enquanto espera a aproximação da tag NFC para limpá-la
    private fun showCleaningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aproxime a pulseira NFC")
        builder.setMessage("Esperando pela aproximação da tag NFC para limpá-la...")
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    // Exibe um diálogo de confirmação quando uma tag NFC válida é detectada
    private fun showConfirmationDialog(locacaoId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tag NFC Detectada")
        builder.setMessage("Locação encontrada!!\nDeseja prosseguir para os detalhes da locação?")
        builder.setPositiveButton("Sim") { _, _ ->
            val intent = Intent(this, LeituraNFCActivity::class.java)
            intent.putExtra("locacaoId", locacaoId)
            startActivity(intent)
        }
        builder.setNegativeButton("Não", null)
        builder.show()
    }

    // Inicia a atividade da câmera para escanear QR Code
    private fun startCameraActivity(unidadeId: String) {
        val intent = Intent(this, CameraQrActivity::class.java)
        intent.putExtra("unidadeId", unidadeId)
        startActivity(intent)
    }

    // Carrega as informações do gerente da unidade a partir do Firestore
    private fun carregarInformacoesGerente(unidadeId: String) {
        firestore.collection("unidades")
            .whereEqualTo("id", unidadeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val unidade = documents.firstOrNull()
                    unidade?.let {
                        val gerente = it.getString("gerente")
                        val nomeUnidade = it.getString("nome")
                        val enderecoUnidade = it.getString("endereco")
                        val emailGerente = it.getString("usuarioGerente")

                        nomeTextView.text = "Olá, $gerente"
                        infosGerenteTextView.text = "$nomeUnidade\n$enderecoUnidade"
                        managerNameTextView.text = "Gerente: $gerente"
                        managerContactTextView.text = "$emailGerente"
                    }
                } else {
                    nomeTextView.text = "Dados não encontrados"
                    infosGerenteTextView.text = "Não foi possível carregar informações"
                }
            }
            .addOnFailureListener { e ->
                nomeTextView.text = "Erro ao carregar"
                infosGerenteTextView.text = "Erro: ${e.message}"
            }
    }
}
