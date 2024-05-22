package com.example.projetointegrador3

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HomeActivity : AppCompatActivity() {

    // Declaração de componentes da interface
    private lateinit var nomeTextView: TextView
    private lateinit var logoutButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var cadastrarCartaoButton: FloatingActionButton
    private lateinit var visualizarLocacaoContainer: RelativeLayout
    private lateinit var visualizarMapaContainer: RelativeLayout
    private lateinit var historicoButton: Button
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val permissionCode = 101

    private lateinit var firebaseAuth: FirebaseAuth

    // Método onCreate é executado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Inicialização do Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Verifica se o usuário saiu do app antes de completar a locação
        verificarLocacaoPendente()

        // Inicialização dos componentes de interface do usuário
        nomeTextView = findViewById(R.id.nome_text_view)
        logoutButton = findViewById(R.id.logout_button)
        profileButton = findViewById(R.id.visualizar_perfil_button)
        cadastrarCartaoButton = findViewById(R.id.adicionar_cartao_fab)
        historicoButton = findViewById(R.id.historico_button)
        visualizarLocacaoContainer = findViewById(R.id.visualizarLocacaoContainer)
        visualizarMapaContainer = findViewById(R.id.visualizarUnidadeContainer)

        // Configura o botão de visualização de locação para verificar se há um cartão cadastrado
        visualizarLocacaoContainer.setOnClickListener {
            verificarCartaoCadastrado()
        }

        // Configura o botão de visualização do mapa para abrir a atividade do mapa
        visualizarMapaContainer.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // Inicializa o provedor de localização
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtendo o usuário atualmente autenticado
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Buscando o nome do usuário no Firestore usando o UID
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("pessoas").document(userId)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        nomeTextView.text = "Olá, ${user?.nome}"
                    } else {
                        Log.d("HomeActivity", "Documento não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("HomeActivity", "Falha ", exception)
                }
        } else {
            Toast.makeText(applicationContext, "Nenhum usuário logado", Toast.LENGTH_SHORT).show()
        }

        // Configura o botão de logout
        logoutButton.setOnClickListener {
            realizarLogout()
        }

        // Configura o botão de perfil para abrir a atividade de perfil
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão de cadastro de cartão para abrir a atividade de visualização de cartão
        cadastrarCartaoButton.setOnClickListener {
            val intent = Intent(this, ViewCardActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão de histórico para abrir a atividade de histórico
        historicoButton.setOnClickListener {
            val intent = Intent(this, HistoricoActivity::class.java)
            startActivity(intent)
        }
    }

    // Verifica se há locações pendentes e leva para a atividade do QR code se o usuário não cancelar
    private fun verificarLocacaoPendente() {
        val currentUser = firebaseAuth.currentUser
        val uid = currentUser!!.uid

        val prefs = getSharedPreferences("LocacaoPrefs", MODE_PRIVATE)

        if (prefs.getBoolean("${uid}_locacaoPendente", false)) {
            val opcao = prefs.getString("${uid}_opcao", "")
            val preco = prefs.getInt("${uid}_preco", 0)
            val gerente = prefs.getString("${uid}_gerente", "")
            val unidade = prefs.getString("${uid}_unidade", "")

            if (opcao.isNullOrBlank() || preco == 0 || gerente.isNullOrBlank() || unidade.isNullOrBlank()) {
                Toast.makeText(this, "Informação de locação está incompleta.", Toast.LENGTH_SHORT).show()
                limparLocacaoPendente(uid)
                return
            }

            AlertDialog.Builder(this)
                .setTitle("Locação Pendente")
                .setMessage("Você tem uma locação de $opcao em $unidade pendente. Deseja continuar?")
                .setPositiveButton("Continuar") { dialog, _ ->
                    mostrarQrCode(opcao, preco.toString(), gerente, unidade, uid)
                    dialog.dismiss()
                }
                .setNegativeButton("Não") { dialog, _ ->
                    limparLocacaoPendente(uid)
                    dialog.dismiss()
                }
                .show()
        }
    }

    // Limpa as preferências compartilhadas das locações pendentes
    private fun limparLocacaoPendente(uid: String) {
        val editor = getSharedPreferences("LocacaoPrefs", MODE_PRIVATE).edit()
        editor.remove("${uid}_opcao")
        editor.remove("${uid}_preco")
        editor.remove("${uid}_gerente")
        editor.remove("${uid}_unidade")
        editor.remove("${uid}_locacaoPendente")
        editor.apply()
    }

    // Estrutura de dados para representar um usuário
    data class User(
        val nome: String? = null,
        val email: String? = null
    )

    // Mostra unidades próximas ao usuário atual
    private fun mostrarUnidadesProximas() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                buscarUnidades(it)
            } ?: Toast.makeText(this, "Localização não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    // Busca unidades próximas baseadas na localização
    private fun buscarUnidades(location: Location) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("unidades").get().addOnSuccessListener { documents ->
            val unidadesEProximidade = documents.mapNotNull { document ->
                val nome = document.getString("nome") ?: return@mapNotNull null
                val endereco = document.getString("endereco") ?: return@mapNotNull null
                val latitude = document.getDouble("latitude") ?: return@mapNotNull null
                val longitude = document.getDouble("longitude") ?: return@mapNotNull null
                val gerente = document.getString("gerente") ?: return@mapNotNull null
                val opcoesLocacaoMap = document.get("opcoesLocacao") as? Map<String, Long> ?: return@mapNotNull null
                val armarios = document.get("armarios") as? Map<String, Map<String, Any>>
                val armariosDisponiveis = armarios?.values?.count { it["status"] == "disponivel" } ?: 0
                val unidadeLocal = LatLng(latitude, longitude)
                val distancia = calcularDistancia(location, unidadeLocal)
                UnidadeProximidade(nome, endereco, gerente, unidadeLocal, distancia, opcoesLocacaoMap, armariosDisponiveis)
            }.filter { it.distancia <= 1000 }

            exibirDialogoDeUnidades(unidadesEProximidade)
        }.addOnFailureListener { exception ->
            Log.e("HomeActivity", "Erro ao buscar unidades", exception)
            Toast.makeText(this, "Erro ao buscar unidades.", Toast.LENGTH_SHORT).show()
        }
    }

    // Calcula a distância entre a localização do usuário e uma unidade utilizando Location
    private fun calcularDistancia(location: Location, unidadeLocal: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            unidadeLocal.latitude, unidadeLocal.longitude,
            results
        )
        return results[0]  // Retorna a distância em metros
    }

    // Mostra um diálogo com as unidades próximas disponíveis
    private fun exibirDialogoDeUnidades(unidadesEProximidade: List<UnidadeProximidade>) {
        if (unidadesEProximidade.isEmpty()) {
            Toast.makeText(this, "Nenhuma unidade próxima encontrada", Toast.LENGTH_LONG).show()
            return
        }

        val unidadesArray = unidadesEProximidade.map { unidade ->
            val distanciaFormatada = if (unidade.distancia >= 1000) {
                String.format("%.1f km", unidade.distancia / 1000)
            } else {
                String.format("%.0f m", unidade.distancia)
            }
            "${unidade.nome} - $distanciaFormatada - Armários Disponíveis: ${unidade.armariosDisponiveis}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Escolha uma unidade próxima")
            .setItems(unidadesArray) { dialog, which ->
                val unidadeEscolhida = unidadesEProximidade[which]
                exibirOpcoesLocacao(unidadeEscolhida)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Mostra opções de locação para uma unidade selecionada
    private fun exibirOpcoesLocacao(unidade: UnidadeProximidade) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"))
        val horaAtual = calendar.get(Calendar.HOUR_OF_DAY)
        val minutosAtual = calendar.get(Calendar.MINUTE)

        val opcoesLocacaoArray = unidade.opcoesLocacao.keys.map { chave ->
            val isDisponivel = when (chave) {
                "dia" -> horaAtual in 7..8 // Opção "dia" disponível entre 07:00 e 08:00
                "2h" -> (horaAtual < 16 || (horaAtual == 16 && minutosAtual == 0)) // Opção "2h" disponível até 16:00 (para completar até as 18:00)
                "1h" -> (horaAtual < 17 || (horaAtual == 17 && minutosAtual == 0)) // Opção "1h" disponível até 17:00
                "30min" -> (horaAtual <= 17 && minutosAtual <= 30) // Opção "30min" disponível até 17:30
                else -> true
            }
            chave to isDisponivel
        }

        val opcoesDisponiveis = opcoesLocacaoArray.filter { it.second }.map { it.first }
        val opcoesIndisponiveis = opcoesLocacaoArray.filter { !it.second }.map { it.first }

        val builder = StringBuilder()
        builder.append("Disponíveis no momento:\n")
        opcoesDisponiveis.forEach { opcao ->
            builder.append(opcao).append("\n")
        }
        builder.append("\nIndisponíveis no momento:\n")
        opcoesIndisponiveis.forEach { opcao ->
            builder.append(opcao).append("\n")
        }

        val dialogItems = builder.toString().split("\n").toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Escolha o tempo de locação")
            .setItems(dialogItems) { dialog, which ->
                val escolha = dialogItems[which]
                if (escolha in opcoesDisponiveis) {
                    val preco = unidade.opcoesLocacao[escolha]!!.toInt()
                    confirmarLocacao(escolha, preco, unidade.gerente, unidade)
                } else if (escolha in opcoesIndisponiveis) {
                    Toast.makeText(this, "Opção indisponível no momento", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Confirma a locação com detalhes selecionados
    private fun confirmarLocacao(opcao: String, preco: Int, gerente: String, unidade: UnidadeProximidade) {
        val uid = firebaseAuth.currentUser?.uid ?: return // Garante que temos um UID válido

        AlertDialog.Builder(this)
            .setTitle("Confirmar Locação")
            .setMessage("Deseja confirmar locação de $opcao por R$$preco,00?")
            .setPositiveButton("Confirmar") { dialog, _ ->
                val editor = getSharedPreferences("LocacaoPrefs", MODE_PRIVATE).edit()
                editor.putString("${uid}_opcao", opcao)
                editor.putInt("${uid}_preco", preco)
                editor.putString("${uid}_gerente", gerente)
                editor.putString("${uid}_unidade", unidade.nome)
                editor.putBoolean("${uid}_locacaoPendente", true)
                editor.apply()

                mostrarQrCode(opcao, preco.toString(), gerente, unidade.nome, uid)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Leva para a activity com o QR Code para a locação confirmada
    private fun mostrarQrCode(opcao: String, preco: String, gerente: String, unidadeNome: String, uid: String) {
        val intent = Intent(this, QrCodeActivity::class.java)
        intent.putExtra("opcao", opcao)
        intent.putExtra("preco", preco)
        intent.putExtra("gerente", gerente)
        intent.putExtra("unidade", unidadeNome)
        intent.putExtra("uid", uid)
        startActivity(intent)
    }

    // Lida com os resultados das solicitações de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mostrarUnidadesProximas()
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Verifica se um cartão de crédito está cadastrado para o usuário
    private fun verificarCartaoCadastrado() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Nenhum usuário logado", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("pessoas").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val cartaoCredito = documentSnapshot.data?.get("cartaoCredito")
                if (cartaoCredito != null) {
                    mostrarUnidadesProximas()
                } else {
                    Toast.makeText(this, "Nenhum cartão de crédito cadastrado", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("HomeActivity", "Falha ao verificar cartão", exception)
                Toast.makeText(this, "Erro ao verificar cartão de crédito", Toast.LENGTH_SHORT).show()
            }
    }

    // Estrutura de dados para representar uma unidade com suas devidas informações
    data class UnidadeProximidade(
        val nome: String,
        val endereco: String,
        val gerente: String,
        val local: LatLng,
        val distancia: Float,
        val opcoesLocacao: Map<String, Long>,
        val armariosDisponiveis: Int
    )

    // Método para realizar logout do usuário
    private fun realizarLogout() {
        firebaseAuth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

}
