package com.example.projetointegrador3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class CameraFotosActivity : AppCompatActivity() {

    // Declaração de variáveis para os componentes da UI e variáveis de controle
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var btnCapturePhoto: Button
    private lateinit var btnNext: Button
    private lateinit var tvPhotoCount: TextView
    private lateinit var imageCapture: ImageCapture

    private var totalFotos = 0
    private var fotosTiradas = 0
    private val fotosUri = arrayListOf<Uri>()

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_fotos)

        // Inicialização dos componentes da UI
        cameraPreviewView = findViewById(R.id.viewFinder)
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto)
        btnNext = findViewById(R.id.btnNext)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)

        // Obtém os dados da Intent passada
        val unidadeId = intent.getStringExtra("unidadeId") ?: ""
        totalFotos = intent.getIntExtra("numeroPessoas", 1)
        val uid = intent.getStringExtra("uid") ?: ""
        val opcao = intent.getStringExtra("opcao") ?: ""
        tvPhotoCount.text = "$fotosTiradas/$totalFotos"

        // Configura o botão para capturar fotos
        btnCapturePhoto.setOnClickListener {
            if (fotosTiradas < totalFotos) {
                capturarFoto()
            } else {
                Toast.makeText(this, "Todas as fotos necessárias já foram tiradas", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura o botão para avançar para a próxima atividade
        btnNext.setOnClickListener {
            val intent = Intent(this, DetalhesFotoActivity::class.java).apply {
                putExtra("unidadeId", unidadeId)
                putExtra("uid", uid)
                putExtra("opcao", opcao)
                putParcelableArrayListExtra("fotosUri", fotosUri)
            }
            startActivity(intent)
        }

        // Inicia a câmera
        startCamera()
    }

    // Inicia a câmera e configura a preview e captura de imagem
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Usado para vincular o ciclo de vida das câmeras ao ciclo de vida do proprietário
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configura a PreviewUseCase para exibir a pré-visualização
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(cameraPreviewView.surfaceProvider) }

            // Inicializa ImageCapture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)  // Priorizar a velocidade sobre a qualidade
                .build()

            try {
                // Desvincula todas as use cases antes de vincular
                cameraProvider.unbindAll()

                // Vincula use cases ao ciclo de vida da câmera
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraFotosActivity", "Falha ao vincular use cases", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Captura uma foto e salva em um arquivo
    private fun capturarFoto() {
        val photoFile = File(getOutputDirectory(), "foto_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        Toast.makeText(this@CameraFotosActivity, "Foto Capturada", Toast.LENGTH_SHORT).show()
                        mostrarPreviaFoto(Uri.fromFile(photoFile))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@CameraFotosActivity, "Erro ao capturar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Exibe um diálogo de pré-visualização da foto capturada
    private fun mostrarPreviaFoto(photoUri: Uri) {
        val builder = AlertDialog.Builder(this)
        val imageView = ImageView(this)
        imageView.setImageURI(photoUri)

        builder.setView(imageView)
        builder.setPositiveButton("Confirmar") { dialog, _ ->
            fotosTiradas++
            tvPhotoCount.text = "$fotosTiradas/$totalFotos"
            fotosUri.add(photoUri)
            if (fotosTiradas == totalFotos) {
                btnNext.visibility = View.VISIBLE
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            // Apaga a foto do armazenamento se o usuário cancelar
            photoUri.path?.let { File(it).delete() }
            dialog.dismiss()
        }
        builder.show()
    }

    // Obtém o diretório de saída para salvar as fotos capturadas
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
}
