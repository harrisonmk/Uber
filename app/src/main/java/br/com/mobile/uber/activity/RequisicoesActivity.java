package br.com.mobile.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import br.com.mobile.uber.R;
import br.com.mobile.uber.adapter.RequisicoesAdapter;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.RecyclerItemClickListener;
import br.com.mobile.uber.helper.UsuarioFirebase;
import br.com.mobile.uber.model.Requisicao;
import br.com.mobile.uber.model.Usuario;

public class RequisicoesActivity extends AppCompatActivity {

    //Componentes
    private RecyclerView recyclerRequisicoes;
    private TextView textResultado;


    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseref;
    private List<Requisicao> listaRequisicoes = new ArrayList<>();
    private RequisicoesAdapter adapter;
    private Usuario motorista;

    private LocationManager locationManager;
    private LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requisicoes);

        inicializarComponentes();

        //Recuperar Localizacao do usuario
        recuperarLocalizacaoUsuario();

    }

    @Override
    protected void onStart() {
        super.onStart();

        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao(){

     Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
     DatabaseReference firebaseref = ConfiguracaoFirebase.getFirebaseDatabase();

     DatabaseReference requisicoes = firebaseref.child("requisicoes");


     Query requisicoesPesquisa = requisicoes.orderByChild("motorista/id").equalTo(usuarioLogado.getId());

     requisicoesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

             for(DataSnapshot ds: dataSnapshot.getChildren()){

                 Requisicao requisicao = ds.getValue(Requisicao.class);

                 if(requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO) || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM) || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)){

                     motorista = requisicao.getMotorista();
                     abrirTelaCorrida(requisicao.getId(),motorista,true);

                 }

             }

         }

         @Override
         public void onCancelled(@NonNull DatabaseError databaseError) {

         }
     });

    }



    //Metodo para pegar a localização do usuario
    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //recuperar latitude e longitude
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());


                //Atualizar GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(location.getLatitude(),location.getLongitude());


                //Verifica se a latitude e longitude nao estao vazio
                if(!latitude.isEmpty() && !longitude.isEmpty()){

                  motorista.setLatitude(latitude);
                  motorista.setLongitude(longitude);


                    adcionaEventoCliqueRecyclerView();

                  locationManager.removeUpdates(locationListener);
                  adapter.notifyDataSetChanged();

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            case R.id.menuSair :
                autenticacao.signOut();
                finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }


    private void abrirTelaCorrida(String idRequisicao,Usuario motorista,boolean requisicaoAtiva){

        Intent i = new Intent(RequisicoesActivity.this,CorridaActivity.class);

        i.putExtra("idRequisicao",idRequisicao);
        i.putExtra("motorista",motorista);
        i.putExtra("requisicaoAtiva",requisicaoAtiva);
        startActivity(i);


    }


    private void inicializarComponentes(){

          getSupportActionBar().setTitle("Requisicoes");

          //Configura componentes
        recyclerRequisicoes = findViewById(R.id.recyclerRequisicoes);
        textResultado = findViewById(R.id.textResultado);

        //Configuracoes Iniciais
        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseref = ConfiguracaoFirebase.getFirebaseDatabase();

        //Configurar o RecyclerView
        adapter = new RequisicoesAdapter(listaRequisicoes,getApplicationContext(),motorista);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerRequisicoes.setLayoutManager(layoutManager);
        recyclerRequisicoes.setHasFixedSize(true);
        recyclerRequisicoes.setAdapter(adapter);





        recuperarRequisicoes();


    }


    private void adcionaEventoCliqueRecyclerView(){

        //Adiciona evento de clique no recycler
        recyclerRequisicoes.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(), recyclerRequisicoes, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                Requisicao requisicao = listaRequisicoes.get(position);
                abrirTelaCorrida(requisicao.getId(),motorista,false);
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        }));



    }

    //Recuperar apenas as requisicoes que estiverem com o status Aguardando
    private void recuperarRequisicoes(){

        DatabaseReference requisicoes = firebaseref.child("requisicoes");

        Query requisicaoPesquisa = requisicoes.orderByChild("status").equalTo(Requisicao.STATUS_AGUARDANDO);

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.getChildrenCount() > 0){

                    textResultado.setVisibility(View.GONE);
                    recyclerRequisicoes.setVisibility(View.VISIBLE);

                }else {

                    textResultado.setVisibility(View.VISIBLE);
                    recyclerRequisicoes.setVisibility(View.GONE);

                }

                listaRequisicoes.clear();

                for(DataSnapshot ds : dataSnapshot.getChildren()){

                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    listaRequisicoes.add(requisicao);

                }

                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

}
