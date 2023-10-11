package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        showUserName()
        setupRetrofit()
        setupListeners()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        //Recuperar os Id's da tela para a Activity com o findViewById
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        btnConfirmar = findViewById(R.id.btn_confirmar)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        //colocar a acao de click do botao confirmar
        btnConfirmar.setOnClickListener {
            saveUserLocal(nomeUsuario.text.toString())
            getAllReposByUserName()

        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal(user: String) {
        //Persistir o usuario preenchido na editText com a SharedPref no listener do botao salvar
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return

        with(sharedPref.edit()) {
            putString(R.string.saved_user.toString(), user)
            apply()
        }
    }

    private fun showUserName() {
        //depois de persistir o usuario exibir sempre as informacoes no EditText
        // se a sharedpref possuir algum valor, exibir no proprio editText o valor salvo
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        sharedPref.getString(R.string.saved_user.toString(), "")?.let {
            nomeUsuario.hint = it
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        //Documentacao oficial do retrofit - https://square.github.io/retrofit/
        // URL_BASE da API do  GitHub= https://api.github.com/
        // lembre-se de utilizar o GsonConverterFactory mostrado no curso
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName() {

        githubApi.getAllRepositoriesByUser(nomeUsuario.text.toString())
            .enqueue(object : Callback<List<Repository>>{
                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    if(response.isSuccessful){
                        response.body()?.let { listRep ->
                            setupAdapter(listRep)
                        }
                    }else{
                        Toast.makeText(applicationContext, R.string.error_network, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(applicationContext, R.string.error_network, Toast.LENGTH_LONG).show()
                }

            })
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        val repAdapter = RepositoryAdapter(list)

        listaRepositories.apply {
            adapter = repAdapter
        }

        repAdapter.carItemLister = { repository ->
            openBrowser(repository.htmlUrl)
        }

        repAdapter.btnShareLister = {repository ->
            shareRepositoryLink(repository.htmlUrl)
        }
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio
    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

}