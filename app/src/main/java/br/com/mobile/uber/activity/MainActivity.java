package br.com.mobile.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

import br.com.mobile.uber.R;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.Permissoes;
import br.com.mobile.uber.helper.UsuarioFirebase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth autenticacao;
    private String[] permissoes = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //esconde a actionBars
        getSupportActionBar().hide();

        //Validar permissoes
        Permissoes.validarPermissoes(permissoes,this,1);

        //Desloga usuario logado
       /* autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signOut(); */

    }

    public void abrirTelaLogin(View view){ //abre a tela de Login

          startActivity(new Intent(this,LoginActivity.class));

    }

    public void abrirTelaCadastro(View view){ //abre a tela de cadastro

        startActivity(new Intent(this,CadastroActivity.class));

    }

    @Override
    protected void onStart(){
        super.onStart();

        UsuarioFirebase.redirecionaUsuarioLogado(MainActivity.this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int permissaoResultado : grantResults){
            if( permissaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();
            }
        }

    }

    //metodo para alertar o usuario da permissao
    private void alertaValidacaoPermissao(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

}
