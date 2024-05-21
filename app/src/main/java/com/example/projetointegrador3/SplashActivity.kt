package com.example.projetointegrador3

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private val loadingTimeMillis: Long = 3000 // Tempo de carregamento
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        // Verificar conectividade com a Internet antes de continuar
        if (isConnectedToInternet()) {
            // Simulação de carregamento
            Handler(mainLooper).postDelayed({
                verificarAutenticacao()
            }, loadingTimeMillis)
        } else {
            // Mostrar mensagem de erro se não houver conexão à Internet
            Toast.makeText(applicationContext, "Conecte-se à Internet e tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    // Verifica se o dispositivo está conectado à Internet
    private fun isConnectedToInternet(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Verifica se há autenticação do usuário
    private fun verificarAutenticacao() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Verifica se o email do usuário contém a palavra 'smart'
            if (currentUser.email?.contains("smart") == true) {
                // Usuário é gerente (logar novamente)
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Usuário comum (entrar direto)
                val intent = Intent(this@SplashActivity, HomeActivity::class.java)
                startActivity(intent)
            }
        } else {
            // Se não houver autenticação, inicia a tela de login
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}
