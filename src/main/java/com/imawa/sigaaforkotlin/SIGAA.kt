package com.imawa.sigaaforkotlin

import android.content.Context
import com.imawa.sigaaforkotlin.models.*
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_ARQUIVOS
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_AVALIACOES
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_PARTICIPANTES
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_QUESTIONARIOS
import com.imawa.sigaaforkotlin.models.Disciplina.Companion.PAGINA_TAREFAS
import com.imawa.sigaaforkotlin.network.SIGAAFormBuilder
import com.imawa.sigaaforkotlin.network.SIGAAHistoryManager
import com.imawa.sigaaforkotlin.network.SIGAAInterceptor
import com.imawa.sigaaforkotlin.network.SIGAAParser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class SIGAA(context: Context) {
    var sessionId: String? = null

    var usuario: Usuario? = null

    private val parser = SIGAAParser()
    private val formBuilder = SIGAAFormBuilder(parser)
    private val historyManager = SIGAAHistoryManager(parser)

    private val client = OkHttpClient.Builder().addInterceptor(SIGAAInterceptor(context, parser))
        .connectTimeout(200, TimeUnit.SECONDS).writeTimeout(200, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS).build()

    init {
        Timber.plant(Timber.DebugTree())
    }

    private fun networkGet(caminho: String): Response {
        val requestBuilder = Request.Builder().url(urlBase + caminho)
            .header("Content-Type", "application/x-www-form-urlencoded")

        if (sessionId != null) {
            requestBuilder.header("Cookie", "JSESSIONID=$sessionId")
        }

        historyManager.addToHistory(client.newCall(requestBuilder.build()).execute())
        return historyManager.getLastResponse()
    }

    private fun networkPost(caminho: String, formBody: FormBody): Response {
        val requestBuilder = Request.Builder().url("${urlBase}${caminho}")
            .header("Content-Type", "application/x-www-form-urlencoded").post(formBody)

        if (sessionId != null) {
            requestBuilder.addHeader("Cookie", "JSESSIONID=$sessionId")
        }

        historyManager.addToHistory(client.newCall(requestBuilder.build()).execute())
        return historyManager.getLastResponse()
    }

    /**
     * Realiza o login no SIGAA com as credenciais inseridas
     * Retorna um boolean indicando se o login ocorreu com sucesso
     */
    fun login(login: String, senha: String): Boolean {
        logout()

        // Pegar novo sessionId
        val responseTelaLogin = networkGet("/verTelaLogin.do")
        sessionId = parser.getSessionId(responseTelaLogin)

        if (sessionId == null) {
            return false
        }
        Timber.d("Novo sessionId: $sessionId")

        // Logar sessionId
        val formLogin = formBuilder.buildLoginForm(login, senha)
        val responseLogin =
            networkPost("/logar.do", formLogin)

        val location = parser.getLocation(responseLogin)

        // Retornar false se for um docente
        if (location?.contains("vinculos.jsf") == true || location?.contains("verMenuPrincipal.do") == true) {
            Timber.d("Usuário logado é um docente")
            return false
        }

        // Pular telas de aviso e de questionários
        if (location?.contains("telaAvisoLogon.jsf") == true || location?.contains("questionarios.jsf") == true) {
            Timber.d("Pulando aviso ou questionário")
            getPortalDiscente()
        }

        // Conferir se logou
        if (!parser.getLogado(historyManager.getLastPageBodyString())) {
            // Conferir o portal do discente
            Timber.d("Conferindo o login no portal do discente")
            getPortalDiscente()

            if (!parser.getLogado(historyManager.getLastPageBodyString())) {
                Timber.d("Não foi possível logar")
                return false
            }
        }

        usuario = parser.getUsuarioPortalDiscente(historyManager.getLastPageBodyString(), login)
        Timber.d("Logado como ${usuario!!.nome}")

        return true
    }

    /**
     * Desloga do SIGAA
     */
    fun logout() {
        sessionId = null
        usuario = null
        historyManager.clearHistory()
    }

    /**
     * Retorna todas as disciplinas que o participa ou participou
     */
    fun getAllDisciplinas(): ArrayList<Disciplina> {
        networkGet("/portais/discente/turmas.jsf")
        return parser.getDisciplinasTodasAsTurmas(historyManager.getLastPageBodyString())
    }

    /**
     * Retorna os participantes da disciplina inserida
     */
    fun getParticipantes(disciplina: Disciplina): ArrayList<Usuario> {
        getPaginaPortalDisciplina(disciplina, PAGINA_PARTICIPANTES)
        return parser.getParticipantesDisciplina(historyManager.getLastPageBodyString())
    }

    /**
     * Retorna a lista de arquivos publicados na disciplina inserida
     * Os objetos retornados aqui não contém o conteúdo dos arquivos!
     * Para obter o conteúdo dos arquivos, utilize downloadArquivo()
     */
    fun getArquivos(disciplina: Disciplina): ArrayList<Arquivo> {
        getPaginaPortalDisciplina(disciplina, PAGINA_ARQUIVOS)
        return parser.getArquivosDisciplina(historyManager.getLastPageBodyString(), disciplina)
    }

    /**
     * Retorna as avaliações cadastradas na disciplina inserida
     */
    fun getAvaliacoes(disciplina: Disciplina): ArrayList<Avaliacao> {
        getPaginaPortalDisciplina(disciplina, PAGINA_AVALIACOES)
        return parser.getAvaliacoesDisciplina(historyManager.getLastPageBodyString(), disciplina)
    }

    /**
     * Retorna as tarefas cadastradas na disciplina inserida
     */
    fun getTarefas(disciplina: Disciplina): ArrayList<Tarefa> {
        getPaginaPortalDisciplina(disciplina, PAGINA_TAREFAS)
        return parser.getTarefasDisciplina(historyManager.getLastPageBodyString(), disciplina)
    }

    /**
     * Retorna os questionários cadastrados na disciplina inserida
     */
    fun getQuestionarios(disciplina: Disciplina): ArrayList<Questionario> {
        getPaginaPortalDisciplina(disciplina, PAGINA_QUESTIONARIOS)
        val questionarios =
            parser.getQuestionariosDisciplina(historyManager.getLastPageBodyString(), disciplina)

        // Identificar se os questionários ainda abertos já foram enviados
        val dataAtual = Date()

        for (questionario in questionarios.filter {
            it.dataInicio.before(dataAtual) and it.dataFim.after(
                dataAtual
            )
        }) {
            getPaginaQuestionario(questionario, disciplina)
            questionarios[questionarios.indexOf(questionario)] =
                parser.getQuestionarioCompletoPaginaQuestionario(
                    historyManager.getLastPageBodyString(),
                    questionario
                )
        }

        return questionarios
    }

    /**
     * Abre o portal do discente por meio do botão do menu do discente
     * É utilizado somente para pular avisos no login
     */
    private fun getPortalDiscente(): Response {
        Timber.d("Abrindo portal do discente")
        return networkGet("/verPortalDiscente.do")
    }

    /**
     * Abre o portal da disciplina inserida
     */
    private fun getPortalDisciplina(disciplina: Disciplina): Response {
        if (historyManager.currentDisciplina?.frontEndIdTurma != disciplina.frontEndIdTurma) {
            // Portal da disciplina não está atualmente aberto
            Timber.d("Portal da disciplina ${disciplina.nome} ainda não aberto")
            if (disciplina.id == null) {
                // Disciplina da página com todas as turmas
                Timber.d("Abrindo lista de turmas")
                networkGet("/portais/discente/turmas.jsf")

                Timber.d("Abrindo portal da disciplina")
                val formBody = formBuilder.buildOpenPortalDisciplinaPelasTurmasForm(
                    disciplina,
                    historyManager.lastJavaxViewState
                )
                networkPost("/portais/discente/turmas.jsf", formBody)
            } else {
                // Disciplina do portal do discente
                Timber.d("Abrindo portal do discente")
                networkGet("/portais/discente/discente.jsf")

                Timber.d("Abrindo portal da disciplina")
                val formBody = formBuilder.buildOpenPortalDisciplinaPeloPortalDiscenteForm(
                    disciplina,
                    historyManager.lastJavaxViewState
                )
                networkPost("/portais/discente/discente.jsf#", formBody)
            }
        } else {
            Timber.d("Portal da disciplina ${disciplina.nome} já aberto")
        }

        historyManager.currentDisciplina = disciplina

        return historyManager.getLastResponse()
    }

    /**
     * Abre uma página da esquerda no portal da disciplina
     */
    private fun getPaginaPortalDisciplina(disciplina: Disciplina, pagina: Int): Response {
        // Abrir o portal da disciplina
        getPortalDisciplina(disciplina)

        // Conferir se a página já não está aberta
        // Cada página tem um caminho individual para qual o próximo POST é feito
        // Baseado nesse caminho, é possível identificar a página atual
        if (parser.getCaminhoBotaoPortalDisciplina(historyManager.getLastDisciplinaPageBodyString()) == parser.getCaminhoBotaoPortalDisciplina(
                pagina
            )
        ) {
            // Página já aberta
            Timber.d("Página $pagina já aberta no portal da disciplina ${disciplina.nome}")
        } else {
            // Página não aberta -> abrir a página no portal da disciplina
            val caminho =
                parser.getCaminhoBotaoPortalDisciplina(historyManager.getLastDisciplinaPageBodyString())
            val bodyPaginaPortalDisciplina = formBuilder.buildOpenPaginaPortalDisciplinaForm(
                historyManager.getLastDisciplinaPageBodyString(),
                pagina,
                historyManager.lastJavaxViewState
            )
            Timber.d("Abrindo a página $pagina no portal da disciplina ${disciplina.nome}")
            networkPost(caminho, bodyPaginaPortalDisciplina)
        }

        return historyManager.getLastResponse()
    }

    /**
     * Abre a página inicial do questionário inserido (a página com os botões de voltar e de iniciar o questionário)
     */
    private fun getPaginaQuestionario(
        questionario: Questionario,
        disciplina: Disciplina
    ): Response {
        // Abrir portal do discente
        Timber.d("Abrindo portal do discente")
        networkGet("/portais/discente/discente.jsf")

        // Abrir página do questionário
        Timber.d("Abrindo página do questionário ${questionario.titulo}")
        val formBody = formBuilder.buildOpenPaginaQuestionarioPeloPortalDiscenteForm(
            questionario,
            disciplina,
            historyManager.lastJavaxViewState
        )
        networkPost("/portais/discente/discente.jsf#", formBody)

        // Atualizar a disciplina aberta atualmente salva no historyManager
        if (disciplina.id != null) {
            historyManager.currentDisciplina = disciplina
        } else {
            // Abriu uma turma pública que não vai mais ser usada futuramente
            historyManager.currentDisciplina = null
        }

        return historyManager.getLastResponse()
    }

    companion object {
        const val urlBase = "https://sig.ifc.edu.br/sigaa"
    }
}