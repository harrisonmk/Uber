package br.com.mobile.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import br.com.mobile.uber.R;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.UsuarioFirebase;
import br.com.mobile.uber.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    //Atributos
    private TextInputEditText campoEmail;
    private TextInputEditText campoSenha;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Inicializar Componentes
        campoEmail = findViewById(R.id.editLoginEmail);
        campoSenha = findViewById(R.id.editLoginSenha);

    }

    public void validarLoginUsuario(View view){

        //Recuperar textos dos campos
        String textEmail = campoEmail.getText().toString();
        String textSenha = campoSenha.getText().toString();

        //Verifica Email
        if(!textEmail.isEmpty()){
            //Verifica Senha
            if(!textSenha.isEmpty()){

                Usuario usuario = new Usuario();
                usuario.setEmail(textEmail);
                usuario.setSenha(textSenha);

                logarUsuario(usuario);

            }else{
                Toast.makeText(LoginActivity.this,"Preencha a Senha!",Toast.LENGTH_SHORT).show();

            }

        }else{
            Toast.makeText(LoginActivity.this,"Preencha o email!",Toast.LENGTH_SHORT).show();

        }

    }

    public void logarUsuario(Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(usuario.getEmail(),usuario.getSenha()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){

                    //Verificar o tipo de usuario logado
                    //"Motorista"/"Passageiro"
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);


                }else {

                    String excessao = "";
                    try {
                       throw task.getException();

                    }catch (FirebaseAuthInvalidUserException e){
                        excessao = "Usuario nao esta cadastrado";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excessao = "E-mail e senha nao correspondem a um usuario cadastrado";
                    }catch (Exception e){
                        excessao = "Erro ao cadastrar usuario: "+e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this,excessao,Toast.LENGTH_SHORT).show();

                }

            }
        });

     }

}
