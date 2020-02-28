package br.com.mobile.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import br.com.mobile.uber.R;
import br.com.mobile.uber.config.ConfiguracaoFirebase;
import br.com.mobile.uber.helper.UsuarioFirebase;
import br.com.mobile.uber.model.Usuario;

public class CadastroActivity extends AppCompatActivity {

    //atributos da tela de cadastro
    private TextInputEditText campoNome;
    private TextInputEditText campoEmail;
    private TextInputEditText campoSenha;
    private Switch switchTipoUsuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);


        //inicializar componentes
        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);


    }

    public void validarCadastroUsuario(View view) {

        //Recuperar textos dos campos
        String textNome = campoNome.getText().toString();
        String textEmail = campoEmail.getText().toString();
        String textSenha = campoSenha.getText().toString();

        //verifica se o campo nome nao esta vazio
        if (!textNome.isEmpty()) {

            //verifica se o campo E-mail nao esta vazio
            if (!textEmail.isEmpty()) {

                //verifica se o campo Senha nao esta vazio
                if (!textSenha.isEmpty()) {

                    Usuario usuario = new Usuario();
                    usuario.setNome(textNome);
                    usuario.setEmail(textEmail);
                    usuario.setSenha(textSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    //chama o metodo cadastrarUsuario() que vai salvar no banco;
                    cadastrarUsuario(usuario);

                } else {
                    //se o campo estiver vazio vai entrar nessa condigo e exibir a mensagem preencha a senha
                    Toast.makeText(CadastroActivity.this, "Preencha a Senha!", Toast.LENGTH_SHORT).show();
                }


            } else {
                //se o campo estiver vazio vai entrar nessa condigo e exibir a mensagem preencha o e-mail
                Toast.makeText(CadastroActivity.this, "Preencha o E-mail!", Toast.LENGTH_SHORT).show();
            }

        } else {
            //se o campo estiver vazio vai entrar nessa condigo e exibir a mensagem preencha o nome
            Toast.makeText(CadastroActivity.this, "Preencha o nome!", Toast.LENGTH_SHORT).show();
        }


    }

    //metodo para salvar o usuario no banco de dados
    public void cadastrarUsuario(final Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        //passa o email do usuario e a senha
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {

                    try {

                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Atualizar o nome no UserProfile
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());


                        //Redireciona o usuario com base no seu tipo
                        //se o usuario for passageiro chama a activity maps
                        //se nao chama a activity requisicoes
                        if (verificaTipoUsuario() == "P") {
                            //chama a tela do mapa
                            startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                            //fecha a tela de cadastro de usuario
                            finish();
                            //imprime uma mensagem ao usuario avisando que deu tudo certo

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Passageiro!", Toast.LENGTH_SHORT).show();


                        } else {

                            startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            //fecha a tela de cadastro de usuario
                            finish();

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Motorista!", Toast.LENGTH_SHORT).show();
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                } else {
                    //faz o tratamento de excessoes
                    String excessao = "";
                    try {
                        throw task.getException();

                    } catch (FirebaseAuthWeakPasswordException e) {
                        excessao = "Digite uma senha mais forte!";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        excessao = "Por favor, Digite um e-mail valido";
                    } catch (FirebaseAuthUserCollisionException e) {
                        excessao = "Esta conta ja foi cadastrada";
                    } catch (Exception e) {
                        excessao = "Erro ao cadastrar Usuario: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(CadastroActivity.this, excessao, Toast.LENGTH_SHORT).show();

                }
            }
        });


    }


    //metodo para verificar se o usuario eh passageiro ou motorista
    public String verificaTipoUsuario() {

        //if ternario
        return switchTipoUsuario.isChecked() ? "M" : "P";


    }


}
