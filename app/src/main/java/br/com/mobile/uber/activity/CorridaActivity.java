package br.com.mobile.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.DecimalFormat;

import br.com.mobile.uber.R;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.Local;
import br.com.mobile.uber.helper.UsuarioFirebase;
import br.com.mobile.uber.model.Destino;
import br.com.mobile.uber.model.Requisicao;
import br.com.mobile.uber.model.Usuario;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Componente
    private Button buttonAceitarCorrida;
    private FloatingActionButton fabRota;


    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseref;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private boolean requisicaoAtiva;
    private Destino destino;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);

        inicializarComponentes();

        //Recupera dados do usuario
        if (getIntent().getExtras().containsKey("idRequisicao") && getIntent().getExtras().containsKey("motorista")) {

            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(Double.parseDouble(motorista.getLatitude()), Double.parseDouble(motorista.getLongitude()));
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");

            verificaStatusRequisicao();

        }

    }

    private void verificaStatusRequisicao() {

        DatabaseReference requisicoes = firebaseref.child("requisicoes").child(idRequisicao);

        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //Recupera requisicao
                requisicao = dataSnapshot.getValue(Requisicao.class);
                if (requisicao != null) {
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(Double.parseDouble(passageiro.getLatitude()), Double.parseDouble(passageiro.getLongitude()));

                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alterarInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void alterarInterfaceStatusRequisicao(String status) {


        switch (status) {

            case Requisicao.STATUS_AGUARDANDO:

                requisicaoAguardando();

                break;
            case Requisicao.STATUS_A_CAMINHO:

                requisicaoACaminho();

                break;
            case Requisicao.STATUS_VIAGEM:

                requisicaoVigem();

                break;
            case Requisicao.STATUS_FINALIZADA:

                requisicaoFinalizada();

                break;
            case Requisicao.STATUS_CANCELADA:

                requisicaoCancelada();

                break;
        }


    }

    private void requisicaoCancelada(){

        Toast.makeText(this,"Requisicao foi cancelada pelo passageiro",Toast.LENGTH_SHORT).show();

        startActivity(new Intent(CorridaActivity.this,RequisicoesActivity.class));

    }


    private void requisicaoFinalizada() {

        fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;

        if (marcadorMotorista != null) {

            marcadorMotorista.remove();

        }

        if (marcadorDestino != null) {
            marcadorDestino.remove();
        }

        //Exibe marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()),Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino,"Destino");
        centralizarMarcador(localDestino);


        //Calcular distancia pelo kilometro percorrido
        //4 reais por kilometro
        float distancia = Local.calcularDistancia(localPassageiro,localDestino);
        float valor = distancia*4;
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        buttonAceitarCorrida.setText("corrida finalizada - R$ "+resultado);

    }


    private void centralizarMarcador(LatLng local){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 20));


    }


    private void requisicaoAguardando() {

        buttonAceitarCorrida.setText("Aceitar corrida");

        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        centralizarMarcador(localMotorista);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localMotorista, 20));

    }

    private void requisicaoACaminho() {

        buttonAceitarCorrida.setText("A caminho do passageiro");
        fabRota.setVisibility(View.VISIBLE);

        //Exibir marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());


        //Exibir marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar Dois Marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //Inicia monitoramento do motorista / passageiro
        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);

    }


    private void requisicaoVigem() {

        //Altera interface
        fabRota.setVisibility(View.VISIBLE);
        buttonAceitarCorrida.setText("A caminho do destino");

        //Exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());


        //Exibe marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");

        //Centraliza marcadores motorista/destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

        //Inicia monitoramento do motorista / passageiro
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);


    }

    private void iniciarMonitoramento(final Usuario uOrigem, LatLng localDestino, final String status) {

        //Inicializar GeoFire
        //Define no de local de usuario
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase().child("local_usuario");

        GeoFire geoFire = new GeoFire(localUsuario);

        //Adiciona circulo no passageiro
        // o radius eh 50 em metros
        final Circle circulo = mMap.addCircle(new CircleOptions().center(localDestino).radius(50).fillColor(Color.argb(90, 255, 153, 0)).strokeColor(Color.argb(190, 255, 152, 0)));

        //o radius aqui eh em quilometros 0.05 eh 50 metros
        final GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(localDestino.latitude, localDestino.longitude), 0.05);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {


                if (key.equals(uOrigem.getId())) {

                    //Log.d("onKeyEntered","onKeyEntered: passageiro esta dentro da area!");

                    //
                    //Altera status da requisicao
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();

                    //Remove listener
                    geoQuery.removeAllListeners();
                    circulo.remove();

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    //Metodo para centralizar dois marcadores carros e passageiros poderia usar mais de dois com List
    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2) {

        //Define quais marcadores serao exibido na tela
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        //Poderia usar um for para percorrer uma lista de marcadores se tivesse mais marcadores
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        //Recupera a largura do mapa
        int largura = getResources().getDisplayMetrics().widthPixels;

        //Recupera a altura do mapa
        int altura = getResources().getDisplayMetrics().heightPixels;

        //Espacamento interno igual ao padding do css
        int padding = (int) (largura * 0.20);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, padding));

    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions().position(localizacao).title(titulo).icon(BitmapDescriptorFactory.fromResource(R.drawable.carro)));




    }


    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions().position(localizacao).title(titulo).icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));


    }

    private void adicionarMarcadorDestino(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null) {

            marcadorPassageiro.remove();

        }

        if (marcadorDestino != null) {
            marcadorDestino.remove();
        }

        marcadorDestino = mMap.addMarker(new MarkerOptions().position(localizacao).title(titulo).icon(BitmapDescriptorFactory.fromResource(R.drawable.destino)));


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localizacao do usuario
        recuperarLocalizacaoUsuario();


    }


    //Metodo para pegar a localização do usuario
    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);


                //Atualizar GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Atualizar localizacao motorista no firebase
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                alterarInterfaceStatusRequisicao(statusRequisicao);


            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //Solicitar atualizacoes de localizacao
        // tempo em segundos | distancia em metros
        // no padrao 10000 e 10 para ficar mais rapido mas ele vai queimar mais bateria vou colocar 0 e 0
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
        }


    }


    public void aceitarCorrida(View view) {

        //Configura Requisicao
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);

        requisicao.atualizar();


    }


    private void inicializarComponentes() {


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle("Iniciar corrida");


        buttonAceitarCorrida = findViewById(R.id.buttonAceitarCorrida);

        //Configuracoes iniciais

        firebaseref = ConfiguracaoFirebase.getFirebaseDatabase();


        // Obtenha o SupportMapFragment e seja notificado quando o mapa estiver pronto para ser usado.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Adiciona evento de clique no FabRota
        fabRota = findViewById(R.id.fabRota);
        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String status = statusRequisicao;
                if (status != null && !status.isEmpty()) {

                    String lat = "";
                    String lon = "";

                    switch (status) {

                        case Requisicao.STATUS_A_CAMINHO:

                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);

                            break;

                        case Requisicao.STATUS_VIAGEM:

                            lat = destino.getLatitude();
                            lon = destino.getLongitude();

                            break;

                    }

                    //Abrir rota
                    String latLong = lat + "," + lon;

                    Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);

                }

            }
        });

    }


    @Override
    public boolean onSupportNavigateUp() {

        if (requisicaoAtiva) {
            Toast.makeText(CorridaActivity.this, "Necessario encerrar a requisicao atual!", Toast.LENGTH_SHORT).show();
        } else {

            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(i);
        }

        //Verifica o status da requisicao para encerrar
        if(statusRequisicao != null && statusRequisicao.isEmpty()){
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }

        return false;
    }
}