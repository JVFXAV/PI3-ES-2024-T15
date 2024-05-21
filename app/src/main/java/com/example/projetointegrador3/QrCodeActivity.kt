package com.example.projetointegrador3

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        // Recebe os dados passados pela Intent que iniciou essa atividade na tela Home.
        val opcao = intent.getStringExtra("opcao")
        val preco = intent.getStringExtra("preco")
        val gerente = intent.getStringExtra("gerente")
        val unidade = intent.getStringExtra("unidade")
        val uid = intent.getStringExtra("uid")

        // Monta uma string com todas as informações que serão incluídas no QR Code.
        val qrText = "Locação: $opcao\nPreço: $preco\nGerente: $gerente\nUnidade: $unidade\nuid: $uid"

        val qrCodeImageView = findViewById<ImageView>(R.id.qrCodeImageView)
        // Gera o QR Code como um Bitmap e define no ImageView.
        qrCodeImageView.setImageBitmap(generateQrCode(qrText))

        val infoTextView = findViewById<TextView>(R.id.tvQrCodeInfo)
        infoTextView.text = "Tempo: $opcao\nPreço: R$$preco,00\nGerente: $gerente\n$unidade"
    }

    // Método para gerar o QR Code a partir de uma string.
    private fun generateQrCode(text: String): Bitmap {
        val size = 512 // tamanho do QR Code em pixels
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size) // Gera uma matriz de bits para o QR Code.
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565) // Cria um Bitmap vazio para o QR Code.
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Preenche o Bitmap com pixels pretos onde a matriz de bits é verdadeira, e brancos onde é falsa.
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
