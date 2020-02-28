package br.com.mobile.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.mobile.uber.R;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.Local;
import br.com.mobile.uber.helper.UsuarioFirebase;
import br.com.mobile.uber.model.Destino;
import br.com.mobile.uber.model.Requisicao;
import br.com.mobile.uber.model.Usuario;

public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {


    //Componentes
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;


    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseref;
    private Requisicao requisicao;
    private Usuario passageiro;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private Usuario motorista;
    private LatLng localMotorista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);


        inicializarComponentes();

        //Adicionar listener para status da requisicao
        verificarStatusRequisicao();


    }


    private void verificarStatusRequisicao() {

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        DatabaseReference requisicoes = firebaseref.child("requisicoes");
        Query requisicoesPesquisa = requisicoes.orderByChild("passageiro/id").equalTo(usuarioLogado.getId());

        requisicoesPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    lista.add(ds.getValue(Requisicao.class));


                }

                Collections.reverse(lista);

                if (lista != null && lista.size() > 0) {

                    requisicao = lista.get(0);

                    if (requisicao != null) {
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)) {
                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(Double.parseDouble(passageiro.getLatitude()), Double.parseDouble(passageiro.getLongitude()));

                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(Double.parseDouble(motorista.getLatitude()), Double.parseDouble(motorista.getLongitude()));
                            }
                            alterarInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void alterarInterfaceStatusRequisicao(String status) {

        if (status != null && !status.isEmpty()) {
           cancelarUber = false;
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

        }else {
            //Adiciona marcador passageiro
            adicionarMarcadorPassageiro(localPassageiro,"Seu local");
            centralizarMarcador(localPassageiro);

        }

    }

    private void requisicaoCancelada(){

        linearLayoutDestino.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("Chamar Uber");
        cancelarUber = false;



    }

    private void requisicaoAguardando() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Cancelar Uber");
        cancelarUber = true;

        //Adiciona marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);


    }

    private void requisicaoACaminho() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Motorista a caminho");
        buttonChamarUber.setEnabled(false);

        //Adicionar marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Adiciona marcador motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //Centralizar passageiro / motorista
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);


    }

    private void requisicaoVigem() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("A caminho do destino");
        buttonChamarUber.setEnabled(false);

        //Adiciona marcador motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //Adicionar marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");

        //Centraliza marcadores motorista / destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

    }

    private void requisicaoFinalizada() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setEnabled(false);

        //Adicionar marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //Calcular distancia pelo kilometro percorrido
        //4 reais por kilometro
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 4;
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        buttonChamarUber.setText("corrida finalizada - R$ " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Total da viagem").setMessage("Sua Viagem ficou: R$ " + resultado).setCancelable(false).setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                requisicao.atualizarStatus();

                finish();
                startActivity(new Intent(getIntent()));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions().position(localizacao).title(titulo).icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));


    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions().position(localizacao).title(titulo).icon(BitmapDescriptorFactory.fromResource(R.drawable.carro)));


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

    private void centralizarMarcador(LatLng local) {

        //Zoom na camera de 20
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 20));


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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localizacao do usuario
        recuperarLocalizacaoUsuario();


    }

    public void chamarUber(View view) {

        //false -> uber nao pode ser cancelado ainda
        //true -> uber pode ser cancelado
        if (cancelarUber) { //uber pode ser cancelado

            //Cancelar a requisicao
            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();




        } else {

            //Inicio
            String enderecoDestino = editDestino.getText().toString();

            if (!enderecoDestino.equals("") || enderecoDestino != null) {

                Address adressDestino = recuperaEndereco(enderecoDestino);

                if (adressDestino != null) {

                    final Destino destino = new Destino();

                    //pega a cidade
                    destino.setCidade(adressDestino.getAdminArea());

                    //pega o cep de destino
                    destino.setCep(adressDestino.getPostalCode());

                    //pega o bairro
                    destino.setBairro(adressDestino.getSubLocality());

                    //pega a rua
                    destino.setRua(adressDestino.getThoroughfare());

                    //pega o numero
                    destino.setNumero(adressDestino.getFeatureName());

                    //pega a latitude
                    destino.setLatitude(String.valueOf(adressDestino.getLatitude()));

                    //pega a longitude
                    destino.setLongitude(String.valueOf(adressDestino.getLongitude()));


                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNumero: " + destino.getNumero());
                    mensagem.append("\nCep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Confirme seu endereco!").setMessage(mensagem).setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            //Salvar Requisicao
                            salvarRequisicao(destino);


                        }
                    }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            } else {
                Toast.makeText(this, "Informe o endereco de destino!", Toast.LENGTH_SHORT).show();
            }


            //Fim


        }


    }


    //Metodo para salvar requisicao no Firebase
    private void salvarRequisicao(Destino destino) {

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        //Oculta o linear Layout
        linearLayoutDestino.setVisibility(View.GONE);


        //Troca o texto do botao
        buttonChamarUber.setText("Cancelar Uber");

    }


    //metodo para retornar a localizaco de destino
    private Address recuperaEndereco(String endereco) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);

            if (listaEnderecos != null && listaEnderecos.size() > 0) {
                Address address = listaEnderecos.get(0);

                //double lat = address.getLatitude();
                //double lon = address.getLatitude();

                return address;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
                localPassageiro = new LatLng(latitude, longitude);


                //Atualizar GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);


                //Altera a interface de acordo com o status
                alterarInterfaceStatusRequisicao(statusRequisicao);


                if (statusRequisicao != null && !statusRequisicao.isEmpty()) {
                    //verifica as atualizacoes do passageiro e faz a remocao (para de fazer o monitoramento da localizacao do passageiro)
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM) || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    }else {
                        //Solicitar atualizacoes de localizacao
                        // tempo em segundos | distancia em metros
                        // no padrao 10000 e 10 para ficar mais rapido mas ele vai queimar mais bateria vou colocar 0 e 0
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
                        }

                    }
                }

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    //Metodo para inicializar os componentes
    private void inicializarComponentes() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem"); //titulo da tela
        setSupportActionBar(toolbar);


        //Inicializar componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);


        //Configuracoes iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseref = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

}
