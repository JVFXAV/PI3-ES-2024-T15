package com.example.projetointegrador3

import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions

class CameraQrActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e controle
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var cardView: CardView
    private lateinit var tvQrCodeData: TextView
    private lateinit var btnScanAgain: Button
    private lateinit var btnProceed: Button

    private var opcao: String? = null
    private var uid: String? = null

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_qr)

        // Inicialização dos componentes da UI
        cameraPreviewView = findViewById(R.id.viewFinder)
        cardView = findViewById(R.id.cardView)
        tvQrCodeData = findViewById(R.id.tvQrCodeData)
        btnScanAgain = findViewById(R.id.btnScanAgain)
        btnProceed = findViewById(R.id.btnProceed)

        val unidadeId = intent.getStringExtra("unidadeId") ?: ""

        // Configura o botão para escanear novamente
        btnScanAgain.setOnClickListener {
            cardView.visibility = View.GONE
            startCamera(unidadeId)
        }

        // Configura o botão para prosseguir após escanear o QR Code
        btnProceed.setOnClickListener {
            checkAvailableLockers(unidadeId)
        }

        // Inicia a câmera para escanear o QR Code
        startCamera(unidadeId)
    }

    // Inicia a câmera e configura a preview e análise de imagem
    private fun startCamera(unidadeId: String) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    // Vincula as use cases de preview e análise de imagem à câmera
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(cameraPreviewView.surfaceProvider) }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer(this, options, ::showQrCodeData))
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (e: Exception) {
            Toast.makeText(this, "Falha na vinculação das configurações de câmera", Toast.LENGTH_SHORT).show()
        }
    }

    // Exibe os dados do QR Code escaneado na UI
    private fun showQrCodeData(dataMap: Map<String, String>) {
        val camposDesejados = listOf("Locação", "Preço", "Gerente", "Unidade")
        val displayText = dataMap.entries
            .filter { it.key in camposDesejados }
            .joinToString("\n") { "${it.key}: ${it.value}" }

        // Verifica se o gerente escaneado é o gerente da unidade
        val unidadeId = intent.getStringExtra("unidadeId") ?: ""
        val gerenteEscaneado = dataMap["Gerente"] ?: ""

        FirebaseFirestore.getInstance().collection("unidades")
            .whereEqualTo("id", unidadeId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val gerenteUnidade = document.getString("gerente")
                    if (gerenteUnidade == gerenteEscaneado) {
                        tvQrCodeData.text = displayText
                        cardView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, "Gerente não autorizado para esta unidade", Toast.LENGTH_SHORT).show()
                        cardView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this, "Unidade não encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao verificar gerente: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Verifica a disponibilidade de armários na unidade
    private fun checkAvailableLockers(unidadeId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("unidades")
            .whereEqualTo("id", unidadeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhuma unidade encontrada com o ID: $unidadeId", Toast.LENGTH_SHORT).show()
                } else {
                    val documentSnapshot = documents.documents[0]
                    val armarios = documentSnapshot.data?.get("armarios") as? Map<String, Map<String, Any>>
                    val armariosDisponiveis = armarios?.values?.count { it["status"] == "disponivel" } ?: 0

                    if (armariosDisponiveis == 0) {
                        Toast.makeText(this, "Nenhum armário disponível", Toast.LENGTH_SHORT).show()
                    } else {
                        perguntarQuantidadeDePessoas(unidadeId)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao acessar armários: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Pergunta ao usuário quantas pessoas acessarão o armário e prossegue para a próxima atividade
    private fun perguntarQuantidadeDePessoas(unidadeId: String) {
        val opcoes = arrayOf("Uma pessoa", "Duas pessoas")
        AlertDialog.Builder(this)
            .setTitle("Quantas pessoas acessarão este armário?")
            .setItems(opcoes) { dialog, which ->
                val numeroPessoas = when (which) {
                    0 -> 1
                    1 -> 2
                    else -> 1 // caso padrão, nunca deverá acontecer
                }
                val intent = Intent(this, CameraFotosActivity::class.java)
                intent.putExtra("numeroPessoas", numeroPessoas)
                intent.putExtra("unidadeId", unidadeId)
                intent.putExtra("uid", uid)
                intent.putExtra("opcao", opcao)
                startActivity(intent)
                dialog.dismiss()
            }
            .show()
    }

    // Analisador de QR Code para processar imagens da câmera
    private class QRCodeAnalyzer(
        private val activity: CameraQrActivity,
        private val options: BarcodeScannerOptions,
        private val onQRCodeDetected: (Map<String, String>) -> Unit
    ) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient(options)

        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val valueType = barcode.valueType
                            if (valueType == Barcode.TYPE_TEXT) {
                                val data = parseQrCodeData(barcode.displayValue ?: "")
                                val uid = data["uid"]
                                val opcao = data["Locação"]
                                activity.uid = uid
                                activity.opcao = opcao
                                onQRCodeDetected(data)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Tratamento de falha no processamento do QR Code
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        // Método para analisar o texto do QR Code e extrair dados chave-valor
        private fun parseQrCodeData(text: String): Map<String, String> {
            val dataMap = mutableMapOf<String, String>()
            text.lines().forEach { line ->
                val parts = line.split(": ").map { it.trim() }
                if (parts.size == 2) {
                    dataMap[parts[0]] = parts[1]
                }
            }
            return dataMap
        }
    }
}
