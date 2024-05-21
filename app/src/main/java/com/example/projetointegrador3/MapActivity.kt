package com.example.projetointegrador3

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.net.Uri
import android.widget.ImageButton
import com.google.firebase.firestore.FirebaseFirestore

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val permissionCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Inicializa o cliente de localização
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val closeButton = findViewById<ImageButton>(R.id.close_button)
        closeButton.setOnClickListener {
            finish()
        }
    }

    // Callback chamado quando o mapa está pronto para ser usado
    override fun onMapReady(googleMap: GoogleMap) {
        // Define os listeners para cliques nos marcadores e nas janelas de informação
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener(this)

        // Busca e marca unidades no mapa
        fetchUnitsAndSetMarkers()
        // Busca a localização atual do usuário
        getCurrentLocationUser()
        // Configura um marcador para a localização do usuário (se permitido)
        setupLocationMarker()
    }

    // Busca as unidades no banco e adiciona marcadores para cada unidade
    private fun fetchUnitsAndSetMarkers() {
        val unidadesRef = FirebaseFirestore.getInstance().collection("unidades")
        unidadesRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document.exists()) {
                        val nome = document.getString("nome") ?: "Unidade sem nome"
                        val latitude = document.getDouble("latitude") ?: continue
                        val longitude = document.getDouble("longitude") ?: continue
                        val latLng = LatLng(latitude, longitude)

                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(nome)
                                .snippet(document.getString("endereco"))
                        )
                    } else {
                        Toast.makeText(this, "Unidade não encontrada", Toast.LENGTH_SHORT).show()
                    }
                }

                if (documents.size() > 0) {
                    val firstLatLng = LatLng(
                        documents.first().getDouble("latitude") ?: -23.550520,
                        documents.first().getDouble("longitude") ?: -46.633308
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 10f))
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar unidades: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Busca a localização atual do usuário e solicita permissões se necessário
    private fun getCurrentLocationUser() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), permissionCode
            )
            return
        }
    }

    // Manipula o clique nas janelas de informação dos marcadores
    override fun onInfoWindowClick(marker: Marker) {
        // Traça uma rota no Google Maps para o destino clicado
        traceRoute(marker.position)
    }

    // Manipula o clique nos marcadores
    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return true
    }

    // Abre o Google Maps para traçar uma rota da localização atual até o destino
    private fun traceRoute(destination: LatLng) {
        val uri = "http://maps.google.com/maps?saddr=${currentLocation.latitude},${currentLocation.longitude}&daddr=${destination.latitude},${destination.longitude}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    // Configura o marcador da localização atual, se as permissões foram concedidas
    private fun setupLocationMarker() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                permissionCode // Certifique-se de que este código é o mesmo que você usa no resto do app para solicitações de permissão
            )
            return
        }

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao obter localização atual", Toast.LENGTH_SHORT).show()
            }
    }

    // Trata os resultados das solicitações de permissão
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocationUser()
                } else {
                    Toast.makeText(applicationContext, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}





