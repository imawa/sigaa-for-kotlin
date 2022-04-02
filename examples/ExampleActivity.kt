package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.imawa.sigaaforkotlin.SIGAA
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.INTERNET_INDISPONIVEL
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.RESPOSTA_INVALIDA
import com.imawa.sigaaforkotlin.network.SIGAAException.Companion.SESSAO_EXPIRADA
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class ExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        Completable.fromRunnable { acessarSIGAA() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // Execução finalizada
                onFim()
            }, {
                // Erro
                onErro(it)
            })
    }

    fun acessarSIGAA() {
        val sigaa = SIGAA(this)

        if (sigaa.login("usuario", "senha")) {
            // Logado com sucesso
            val disciplinas = sigaa.getAllDisciplinas()

            // Obter avaliações, tarefas e questionários da primeira disciplina
            val avaliacoes = sigaa.getAvaliacoes(disciplinas[0])
            val tarefas = sigaa.getTarefas(disciplinas[0])
            val questionarios = sigaa.getQuestionarios(disciplinas[0])

            // Obter os arquivos da segunda disciplina
            val arquivos = sigaa.getArquivos(disciplinas[1])

            for (arquivo in arquivos) {
                // Baixar o arquivo
                val file = sigaa.downloadArquivo(arquivo)

                // Salvar o arquivo no armazenamento
                val diretorio = getExternalFilesDir("arquivosBaixados")
                File(diretorio, arquivo.titulo).writeBytes(file.readBytes())
                println("Arquivo ${arquivo.titulo} salvo em ${diretorio!!.path}")
            }
        } else {
            println("Não foi possível logar!")
        }
    }

    fun onFim() {
        println("Execução finalizada com sucesso!")
    }

    fun onErro(erro: Throwable) {
        when (erro.message) {
            INTERNET_INDISPONIVEL -> {
                println("Você não está conectado à Internet.")
            }
            RESPOSTA_INVALIDA -> {
                println("Resposta inválida! Provavelmente, o SIGAA está em manutenção.")
            }
            SESSAO_EXPIRADA -> {
                println("Sessão expirada! É necessário logar mais uma vez.")
            }
        }
    }
}
