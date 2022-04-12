package com.imawa.sigaaforkotlin.network

import com.imawa.sigaaforkotlin.SIGAA.Companion.urlBase
import com.imawa.sigaaforkotlin.entities.*
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_ARQUIVO
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_CONTEUDO
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_DESCONHECIDO
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_FORUM
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_QUESTIONARIO
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_REFERENCIA
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_TAREFA
import com.imawa.sigaaforkotlin.entities.Anexo.Companion.ANEXO_VIDEO
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_ARQUIVOS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_AVALIACOES
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_CONTEUDOS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_FREQUENCIAS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_NOTAS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_NOTICIAS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_PARTICIPANTES
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_PRINCIPAL
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_QUESTIONARIOS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_REFERENCIAS
import com.imawa.sigaaforkotlin.entities.Disciplina.Companion.PAGINA_TAREFAS
import com.imawa.sigaaforkotlin.entities.Referencia.Companion.REFERENCIA_LIVRO
import com.imawa.sigaaforkotlin.entities.Referencia.Companion.REFERENCIA_OUTROS
import com.imawa.sigaaforkotlin.entities.Referencia.Companion.REFERENCIA_SITE
import com.imawa.sigaaforkotlin.entities.Usuario.Companion.USUARIO_DISCENTE
import com.imawa.sigaaforkotlin.entities.Usuario.Companion.USUARIO_DOCENTE
import okhttp3.Response
import org.jsoup.Jsoup
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Classe com funções para interpretar o conteúdo das páginas do SIGAA.
 */
class SIGAAParser {
    private val formatoData = SimpleDateFormat("dd/MM/yyyy HH:mm")

    fun getSessionId(response: Response): String? =
        response.header("Set-Cookie")?.split("JSESSIONID=")?.get(1)?.split(";")?.get(0)

    /**
     * Retorna o URL para que o cliente foi redirecionado.
     */
    fun getLocation(response: Response): String? = response.priorResponse?.header("Location")

    fun getJavaxViewState(body: String): String? {
        val document = Jsoup.parse(body)
        return document.body().getElementById("javax.faces.ViewState")?.attr("value")
    }

    fun getUsuarioPortalDiscente(body: String, login: String): Usuario {
        val document = Jsoup.parse(body)

        val matricula = Integer.parseInt(
            document.body().getElementById("perfil-docente")!!.child(4).child(1).child(0).child(0)
                .child(1).text()
        )
        val nome = document.body().getElementsByClass("nome")[0].text()
        val email = "" // O portal do discente não mostra o email completo
        val urlAvatar = "${urlBase}${
        document.getElementsByClass("foto")[0].child(0).attr("src").removePrefix("/sigaa")
        }"
        val curso =
            document.getElementsContainingText("Curso:").last()!!.nextElementSibling()!!.text()
                .trim()

        // Disciplinas
        val disciplinasPeriodoAtual = ArrayList<Disciplina>()
        val periodoAtual =
            document.getElementsByClass("periodo-atual")[0].getElementsByTag("strong")[0].text()

        for (element in document.body().getElementsByClass("descricao")) {
            val child = element.child(0).child(1)
            val outerHtmlArgs = child.outerHtml().split("'")

            val id = element.parent()!!.nextElementSibling()!!.child(0).id().replace("linha_", "")
            val nome = child.html()
            val formAcessarTurmaVirtual = outerHtmlArgs[3]
            val formAcessarTurmaVirtualCompleto = outerHtmlArgs[5]
            val frontEndIdTurma = outerHtmlArgs[11]

            disciplinasPeriodoAtual.add(
                Disciplina(
                    id,
                    nome,
                    periodoAtual,
                    formAcessarTurmaVirtual,
                    formAcessarTurmaVirtualCompleto,
                    frontEndIdTurma
                )
            )
        }

        return Usuario(
            login,
            matricula,
            nome,
            email,
            urlAvatar,
            curso,
            USUARIO_DISCENTE,
            disciplinasPeriodoAtual
        )
    }

    fun getDisciplinasTodasAsTurmas(body: String): ArrayList<Disciplina> {
        val document = Jsoup.parse(body)
        val bodyTabela = document.getElementsByClass("listagem")[0].getElementsByTag("tbody")[1]

        var periodoAtual = ""
        val disciplinas = ArrayList<Disciplina>()

        for (linha in bodyTabela.children()) {
            when (linha.className()) {
                "linhaPar", "linhaImpar" -> {
                    // Disciplina
                    val nome = linha.child(0).text()
                    val args = linha.getElementsByTag("a")[0].attr("onclick").split("'")
                    val formAcessarTurmaVirtual = args[3]
                    val formAcessarTurmaVirtualCompleto = args[5]
                    val frontEndIdTurma = args[11]
                    disciplinas.add(
                        Disciplina(
                            null,
                            nome,
                            periodoAtual,
                            formAcessarTurmaVirtual,
                            formAcessarTurmaVirtualCompleto,
                            frontEndIdTurma
                        )
                    )
                }
                "destaque no-hover" -> {
                    // Período
                    periodoAtual = linha.text()
                }
            }
        }

        return disciplinas
    }

    /**
     * Retorna os dois pares de String do botão no menu do portal da disciplina.
     * Ao clicar em um dos botões no menu da esquerda do portal da disciplina, são enviados dois
     * pares de valores que indicam a página que deve ser aberta.
     */
    fun getArgsBotaoPortalDisciplina(body: String, pagina: Int): ArrayList<ArrayList<String>> {
        val document = Jsoup.parse(body)

        val primeiroPar = ArrayList<String>()
        val segundoPar = ArrayList<String>()

        // Texto do botão em questão
        val textoBotao = when (pagina) {
            PAGINA_PRINCIPAL -> "Principal"
            PAGINA_PARTICIPANTES -> "Participantes"
            PAGINA_NOTICIAS -> "Notícias"
            PAGINA_FREQUENCIAS -> "Frequência"
            PAGINA_NOTAS -> "Ver Notas"
            PAGINA_CONTEUDOS -> "Conteúdo/Página web"
            PAGINA_REFERENCIAS -> "Referências"
            PAGINA_ARQUIVOS -> "Arquivos"
            PAGINA_AVALIACOES -> "Avaliações"
            PAGINA_TAREFAS -> "Tarefas"
            PAGINA_QUESTIONARIOS -> "Questionários"
            else -> "Principal"
        }

        // Encontrar o botão na página
        val itensMenu = document.body().getElementsByClass("itemMenu")

        for (item in itensMenu) {
            if (item.text().equals(textoBotao)) {
                // Primeiro par
                primeiroPar.add(
                    item.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!
                        .parent()!!.parent()!!.id()
                )
                primeiroPar.add(
                    item.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!.parent()!!
                        .parent()!!.id()
                )

                // Segundo par
                for (string in item.parent()!!.outerHtml().split("'")) {
                    if (string.startsWith("formMenu:j_id_jsp_")) {
                        segundoPar.add(string)
                    }
                }
            }
        }

        return arrayListOf(primeiroPar, segundoPar)
    }

    /**
     * Retorna o caminho do POST para um botão do menu na esquerda do portal da disciplina.
     * Conforme você navega pelo portal da disciplina através do menu da esquerda, esse caminho
     * se altera.
     */
    fun getCaminhoBotaoPortalDisciplina(body: String): String =
        Jsoup.parse(body).getElementById("form_nee")?.attr("action")?.replace("/sigaa", "") ?: ""

    /**
     * Retorna o caminho do POST para um botão do menu na esquerda do portal da disciplina baseado
     * no número da página.
     */
    fun getCaminhoBotaoPortalDisciplina(pagina: Int): String = when (pagina) {
        PAGINA_PRINCIPAL -> "/ava/index.jsf"
        PAGINA_PARTICIPANTES -> "/ava/participantes.jsf"
        PAGINA_NOTICIAS -> "/ava/NoticiaTurma/listar.jsf"
        PAGINA_FREQUENCIAS -> "/ava/FrequenciaAluno/mapa.jsf"
        PAGINA_NOTAS -> "X" // A página de notas não conta para o javaxViewState e não possui um caminho
        PAGINA_ARQUIVOS -> "/ava/ArquivoTurma/listar_discente.jsf"
        PAGINA_CONTEUDOS -> "/ava/ConteudoTurma/listar.jsf"
        PAGINA_REFERENCIAS -> "/ava/IndicacaoReferencia/listar.jsf"
        PAGINA_AVALIACOES -> "/ava/DataAvaliacao/listar.jsf"
        PAGINA_TAREFAS -> "/ava/TarefaTurma/listar.jsf"
        PAGINA_QUESTIONARIOS -> "/ava/QuestionarioTurma/listarDiscente.jsf"
        else -> "X"
    }

    fun getAulasDisciplina(body: String, disciplina: Disciplina): ArrayList<Aula> {
        val aulas = ArrayList<Aula>()

        val document = Jsoup.parse(body)

        for (topico in document.getElementsByClass("topico-aula")) {
            val titulo = topico.getElementsByClass("titulo")[0]?.text()?.trim() ?: ""
            val elementoConteudo = topico.getElementsByClass("conteudotopico")[0]

            // Obter os anexos
            val anexos = ArrayList<Anexo>()

            for (anexo in elementoConteudo.getElementsByClass("item")) {
                // Identificar o anexo
                for (icone in anexo.getElementsByTag("img")) {
                    val src = icone.attr("src")

                    // Pular o ícone de loading
                    if (src == "/sigaa/img/indicator.gif") {
                        continue
                    }

                    var tipo = ANEXO_DESCONHECIDO
                    var titulo: String
                    var descricao: String
                    var url: String? = null
                    var jIdJspCompleto: String? = null
                    var idObjetoAssociado: String? = null

                    when (src) {
                        "/sigaa/ava/img/forumava.png" -> {
                            // Fórum
                            tipo = ANEXO_FORUM
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            val args = a.attr("onclick").split("'")
                            jIdJspCompleto = args[5]
                        }
                        "/sigaa/img/porta_arquivos/icones/conteudo.png" -> {
                            // Conteúdo
                            tipo = ANEXO_CONTEUDO
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            val args = a.attr("onclick").split("'")
                            jIdJspCompleto = args[5]
                            idObjetoAssociado = args[11]
                        }
                        "/sigaa/img/portal_turma/site_add.png" -> {
                            // Referência (url)
                            tipo = ANEXO_REFERENCIA
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            url = a.attr("href")
                        }
                        "/sigaa/img/portal_turma/video.png" -> {
                            // Vídeo
                            tipo = ANEXO_VIDEO
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()

                            if (anexo.getElementsByTag("iframe").size > 0) {
                                // Visualização no SIGAA
                                titulo = anexo.getElementsByTag("h1")[0].text().trim()
                                url = anexo.getElementsByTag("iframe")[0].attr("src")
                            } else {
                                // Visualização em outro site
                                val a = anexo.getElementsByTag("a")[0]
                                titulo = a.text().trim()
                                url = a.attr("href")
                            }
                        }
                        "/sigaa/img/porta_arquivos/icones/zip.png", "/sigaa/img/porta_arquivos/icones/pdf.png", "/sigaa/img/porta_arquivos/icones/ppt.png", "/sigaa/img/porta_arquivos/icones/doc.png", "/sigaa/img/porta_arquivos/icones/html.png", "/sigaa/img/porta_arquivos/icones/imagem.png", "/sigaa/img/porta_arquivos/icones/txt.png", "/sigaa/img/porta_arquivos/icones/desconhecido.png" -> {
                            // Arquivo
                            tipo = ANEXO_ARQUIVO
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            val args = a.attr("onclick").split("'")
                            jIdJspCompleto = args[5]
                            idObjetoAssociado = args[11]
                        }
                        "/sigaa/img/porta_arquivos/icones/tarefa.png" -> {
                            // Tarefa
                            tipo = ANEXO_TAREFA
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            val args = a.attr("onclick").split("'")
                            jIdJspCompleto = args[5]
                            idObjetoAssociado = args[11]
                        }
                        "/sigaa/ava/img/questionario.png" -> {
                            // Questionário
                            tipo = ANEXO_QUESTIONARIO
                            val a = anexo.getElementsByTag("a")[0]
                            titulo = a.text().trim()
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            val args = a.attr("onclick").split("'")
                            jIdJspCompleto = args[5]
                            idObjetoAssociado = args[11]
                        }
                        else -> {
                            // Não implementado
                            Timber.d("Anexo desconhecido: $src")
                            descricao = anexo.getElementsByClass("descricao-item")[0].text().trim()
                            anexo.getElementsByClass("descricao-item")[0].remove()
                            titulo = anexo.text().trim()
                        }
                    }

                    anexos.add(
                        Anexo(
                            tipo,
                            titulo,
                            descricao,
                            url,
                            jIdJspCompleto,
                            idObjetoAssociado,
                            disciplina
                        )
                    )
                }

                // Remover o anexo do conteúdo
                anexo.parent()!!.remove()
            }

            val htmlConteudo = elementoConteudo.html()

            aulas.add(Aula(titulo, htmlConteudo, anexos, disciplina))
        }

        return aulas
    }

    fun getParticipantesDisciplina(body: String): ArrayList<Usuario> {
        val participantes = ArrayList<Usuario>()

        val document = Jsoup.parse(body)

        for (lista in document.getElementsByClass("participantes")) {
            // A página possui uma lista de docentes e outra lista de participantes
            val tipoUsuario =
                if (lista.previousElementSibling()?.child(0)?.text()?.contains("Docente") == true) {
                    USUARIO_DOCENTE
                } else {
                    USUARIO_DISCENTE
                }

            for (linha in lista.getElementsByTag("tr")) {
                // Cada linha agrupa dois usuários
                for (segundaColuna in linha.getElementsByAttributeValue("valign", "top")) {
                    // A segunda coluna possui a maioria das informações de um usuário, e a
                    // anterior sempre possui o avatar
                    val nome = segundaColuna.getElementsByTag("strong").text().trim()
                    val ems = segundaColuna.getElementsByTag("em")
                    val matricula = if (tipoUsuario == USUARIO_DISCENTE) {
                        ems[1].text().trim().toInt()
                    } else {
                        null
                    }
                    val login = ems[2].text().trim()
                    val email = ems[3].text().trim()
                    val urlAvatar = "${urlBase.replace("/sigaa", "")}${
                    segundaColuna.previousElementSibling()!!
                        .getElementsByTag("img")[0].attr("src")
                    }"
                    val curso = "" // Não tem como identificar o curso dos participantes por aqui

                    participantes.add(
                        Usuario(
                            login,
                            matricula,
                            nome,
                            email,
                            urlAvatar,
                            curso,
                            tipoUsuario,
                            ArrayList<Disciplina>()
                        )
                    )
                }
            }
        }

        return participantes
    }

    /**
     * Retorna as notícias na página de notícias do portal da disciplina.
     * As notícias retornadas aqui não possuem conteúdo!
     * Para obter a notícia com todas as informações, é preciso abrir a página individual da notícia
     * e utilizar o getNoticiaCompletaPaginaNoticia().
     */
    fun getNoticiasDisciplina(body: String, disciplina: Disciplina): ArrayList<Noticia> {
        val noticias = ArrayList<Noticia>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.getElementsByTag("tr")) {
                val titulo = linha.child(0).text().trim()
                val args = linha.child(2).child(0).attr("onclick").split("'")
                val jIdJsp = args[3]
                val jIdJspCompleto = args[5]
                val id = args[11].toInt()

                noticias.add(Noticia(id, titulo, "", Date(), jIdJsp, jIdJspCompleto, disciplina))
            }
        }

        return noticias
    }

    fun getNoticiaCompletaPaginaNoticia(body: String, noticia: Noticia): Noticia {
        val document = Jsoup.parse(body)
        val form = document.getElementsByClass("form").first()

        val data = formatoData.parse(form?.child(1)?.child(1)?.text()?.trim() ?: "01/01/2010 00:00")

        val conteudoNoticia = form?.getElementsByClass("conteudoNoticia")?.first()
        var texto = ""

        if (conteudoNoticia != null) {
            for (linha in conteudoNoticia.child(0).children()) {
                texto += "${linha.text()}\n"
            }
            texto = texto.trim()
        }

        return Noticia(
            noticia.id,
            noticia.titulo,
            texto,
            data,
            noticia.jIdJsp,
            noticia.jIdJspCompleto,
            noticia.disciplina
        )
    }

    fun getFrequenciaDisciplina(body: String, disciplina: Disciplina): Frequencia {
        val document = Jsoup.parse(body)

        val dadosFaltas = document.getElementsByClass("botoes-show").first()?.children()?.first()
        val totalFaltas = dadosFaltas?.childNode(2)?.outerHtml()?.trim()?.toIntOrNull() ?: -1
        val maximoFaltasPermitido =
            dadosFaltas?.childNode(6)?.outerHtml()?.trim()?.toIntOrNull() ?: -1

        val frequencia =
            Frequencia(ArrayList<Presenca>(), totalFaltas, maximoFaltasPermitido, disciplina)

        // Adicionar as presenças
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.children()) {
                val data = formatoData.parse("${linha.child(0).text().trim()} 00:00")
                val isRegistrada = linha.child(1).text().trim() != "Não Registrada"
                val faltas = if (linha.child(1).text().trim().split(" ")[0].toIntOrNull() != null) {
                    linha.child(1).text().trim().split(" ")[0].toInt()
                } else {
                    0
                }
                frequencia.presencas.add(Presenca(data, faltas, isRegistrada))
            }
        }

        return frequencia
    }

    fun getNotasDisciplina(body: String, disciplina: Disciplina): ArrayList<Nota> {
        val notas = ArrayList<Nota>()

        val document = Jsoup.parse(body)
        val tabela = document.getElementsByClass("tabelaRelatorio")[0]
        val linhaDescricaoPeriodos = tabela.getElementsByTag("tr")[0]
        val linhaDadosNotas = tabela.getElementById("trAval")
        val linhaNotas = tabela.getElementsByTag("tbody").first()?.child(0)

        if (linhaNotas != null) {
            // Colunas na linha das notas
            var colunaAtual = 2
            var colunaFimPeriodo = 2

            // Percorrer períodos
            for (i in 2 until linhaDescricaoPeriodos.childrenSize()) {
                // Ignorar as médias parciais, nota necessária para o exame, resultado, faltas e
                // situação
                if (linhaDescricaoPeriodos.child(i).attr("colspan").isEmpty()) {
                    continue
                }

                val periodo = linhaDescricaoPeriodos.child(i).text().trim()

                // Identificar quantas colunas o período ocupa
                val totalColunasPeriodo = linhaDescricaoPeriodos.child(i).attr("colspan").toInt()

                colunaFimPeriodo += totalColunasPeriodo

                // Obter colunas (notas) que fazem parte do período
                while (colunaAtual < colunaFimPeriodo) {
                    val id = linhaDadosNotas?.getElementsByTag("th")?.get(colunaAtual)?.attr("id")
                        ?.replace("aval_", "") ?: ""
                    val nota = if (!linhaNotas.child(colunaAtual).text()
                        .contains("-") and linhaNotas.child(colunaAtual).text().isNotEmpty()
                    ) {
                        linhaNotas.child(colunaAtual).text().replace(",", ".").toFloat()
                    } else {
                        (-1).toFloat() // Nota não inserida
                    }
                    val abreviacao: String
                    val descricao: String
                    val notaMaxima: Float
                    val peso: Float

                    if (id.contains("unid")) {
                        // Nota final do período
                        abreviacao = "Nota"
                        descricao = ""
                        notaMaxima = (10).toFloat()
                        peso = (1).toFloat()
                    } else {
                        // Nota de alguma atividade
                        abreviacao =
                            linhaDadosNotas?.getElementById("abrevAval_$id")?.attr("value")?.trim()
                                ?: ""
                        descricao =
                            linhaDadosNotas?.getElementById("denAval_$id")?.attr("value")?.trim()
                                ?: ""
                        notaMaxima =
                            if (linhaDadosNotas?.getElementById("notaAval_$id")?.attr("value")
                                ?.isNotEmpty() == true
                            ) {
                                linhaDadosNotas.getElementById("notaAval_$id")!!.attr("value")
                                    .toFloat()
                            } else {
                                (-1).toFloat()
                            }
                        peso = if (linhaDadosNotas?.getElementById("pesoAval_$id")?.attr("value")
                            ?.isNotEmpty() == true
                        ) {
                            linhaDadosNotas.getElementById("pesoAval_$id")!!.attr("value").toFloat()
                        } else {
                            (-1).toFloat()
                        }
                    }

                    notas.add(
                        Nota(
                            nota,
                            notaMaxima,
                            peso,
                            abreviacao,
                            descricao,
                            periodo,
                            disciplina
                        )
                    )

                    // Ir para a próxima coluna
                    colunaAtual++
                }
            }
        }

        return notas
    }

    /**
     * Retorna os conteúdos na página de conteúdos do portal da disciplina.
     * Os conteúdos retornados aqui não possuem o conteúdo em si!
     * Para obter o conteúdo em si, é preciso abrir a página individual do conteúdo
     * e utilizar o getConteudoCompletoPaginaConteudo().
     */
    fun getConteudosDisciplina(body: String, disciplina: Disciplina): ArrayList<Conteudo> {
        val conteudos = ArrayList<Conteudo>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.children()) {
                val titulo = linha.child(0).text().trim()
                val data = formatoData.parse("${linha.child(1).text().trim()} 00:00")
                val args = linha.child(2).child(0).attr("onclick").split("'")
                val jIdJsp = args[3]
                val jIdJspCompleto = args[5]
                val id = args[11].toInt()

                conteudos.add(Conteudo(id, titulo, "", data, jIdJsp, jIdJspCompleto, disciplina))
            }
        }

        return conteudos
    }

    fun getConteudoCompletoPaginaConteudo(body: String, conteudo: Conteudo): Conteudo {
        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("formAva").first()?.getElementsByTag("tbody")?.first()

        var textoConteudo = ""
        if (bodyTabela != null) {
            for (linhaConteudo in bodyTabela.child(1).child(1).children()) {
                textoConteudo += "${linhaConteudo.text()}\n"
            }
            textoConteudo = textoConteudo.trim()
        }

        return Conteudo(
            conteudo.id,
            conteudo.titulo,
            textoConteudo,
            conteudo.data,
            conteudo.jIdJsp,
            conteudo.jIdJspCompleto,
            conteudo.disciplina
        )
    }

    fun getReferenciasDisciplina(body: String, disciplina: Disciplina): ArrayList<Referencia> {
        val referencias = ArrayList<Referencia>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.children()) {
                val nome = linha.child(0).text().trim()
                val tipo = when (linha.child(1).text().trim()) {
                    "Site" -> REFERENCIA_SITE
                    "Livro" -> REFERENCIA_LIVRO
                    else -> REFERENCIA_OUTROS
                }
                val url = linha.child(2).text().trim()
                val topicoDeAula = linha.child(3).text().trim()

                referencias.add(Referencia(nome, tipo, url, topicoDeAula, disciplina))
            }
        }

        return referencias
    }

    fun getArquivosDisciplina(body: String, disciplina: Disciplina): ArrayList<Arquivo> {
        val arquivos = ArrayList<Arquivo>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.getElementsByTag("tr")) {
                val colunas = linha.getElementsByTag("td")

                val titulo = colunas[0].text().trim()
                val descricao = colunas[1].text().trim()
                val topicoDeAula = colunas[2].text().trim()
                val args = linha.getElementsByTag("a")[0].attr("onclick").split("'")
                val jIdJsp = args[5]
                val id = args[11]

                arquivos.add(Arquivo(id, titulo, descricao, topicoDeAula, jIdJsp, disciplina))
            }
        }

        return arquivos
    }

    fun getAvaliacoesDisciplina(body: String, disciplina: Disciplina): ArrayList<Avaliacao> {
        val avaliacoes = ArrayList<Avaliacao>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.getElementsByTag("tr")) {
                var id: Long = 0
                var descricao = ""
                var dia = ""
                var hora = ""

                // Pegar valores das colunas
                for (coluna in linha.getElementsByTag("td")) {
                    when (coluna.elementSiblingIndex()) {
                        0 -> dia = coluna.text()
                        1 -> hora = coluna.text()
                        2 -> descricao = coluna.text()
                        3 -> id = coluna.child(0).attr("onclick").split("'")[11].toLong()
                    }
                }

                // O formato do dia é padronizado, mas a hora não é
                // Por causa disso, eu estou definindo a data para
                // a meia noite do dia indicado
                val data = formatoData.parse("$dia 00:00")

                avaliacoes.add(Avaliacao(id, descricao, data, hora, disciplina))
            }
        }

        return avaliacoes
    }

    fun getTarefasDisciplina(body: String, disciplina: Disciplina): ArrayList<Tarefa> {
        val tarefas = ArrayList<Tarefa>()

        val document = Jsoup.parse(body)

        // Na página de tarefas, podem existir duas tabelas: uma para as tarefas individuais e
        // outra para as tarefas em grupo
        val tabelas = document.getElementsByClass("listing")

        for (tabela in tabelas) {
            val isIndividual =
                tabela.previousElementSibling()?.text()?.contains("Tarefas Individuais") ?: true

            val bodyTabela =
                tabela.getElementsByTag("tbody").first()

            if (bodyTabela != null) {
                // Cada tarefa ocupa duas linhas no corpo da tabela
                // A segunda contém somente a descrição
                // A primeira contém as outras informações
                for (i in 0 until bodyTabela.children().size step 2) {
                    val primeiraLinha = bodyTabela.child(i)
                    val segundaLinha = bodyTabela.child(i + 1)

                    // Primeira linha
                    val titulo = primeiraLinha.child(1).text().trim()

                    val stringsData =
                        primeiraLinha.child(2).text().trim().replace("h", ":").split(" ")
                    val dataInicio = formatoData.parse("${stringsData[1]} ${stringsData[3]}")
                    val dataFim = formatoData.parse("${stringsData[5]} ${stringsData[7]}")

                    val envios = primeiraLinha.child(4).text().trim().toInt()
                    val isCorrigida = primeiraLinha.child(0).childrenSize() == 1

                    // IDs
                    var id = ""
                    var jId: String? = null
                    var jIdEnviar: String? = null
                    var jIdVisualizar: String? = null

                    val botaoEnviar = primeiraLinha.child(5).children().first()
                    val botaoVisualizar = primeiraLinha.child(6).children().first()

                    val dataAtual = Date()
                    val isEnviavel =
                        dataAtual.after(dataInicio) and dataAtual.before(dataFim) and (botaoEnviar != null)

                    if (botaoEnviar != null) {
                        val args = botaoEnviar.attr("onclick").split("'")
                        id = args[11]
                        jId = args[3]
                        jIdEnviar = args[5]
                    }

                    val isEnviada = botaoVisualizar != null

                    if (isEnviada) {
                        val args = botaoVisualizar!!.attr("onclick").split("'")
                        id = args[11]
                        jId = args[3]
                        jIdVisualizar = args[5]
                    }

                    // Segunda linha
                    var descricao = ""
                    var urlArquivo: String? = null

                    for (paragrafoDescricao in segundaLinha.child(0).children()) {
                        if (paragrafoDescricao.text().equals("Baixar arquivo")) {
                            // Botão de baixar arquivo
                            val caminho = paragrafoDescricao.attr("href").replace("/sigaa", "")
                            urlArquivo = "${urlBase}$caminho"
                        } else {
                            // Texto escrito pelo professor
                            descricao += "${paragrafoDescricao.text()}\n"
                        }
                    }
                    descricao = descricao.trim()

                    tarefas.add(
                        Tarefa(
                            id,
                            titulo,
                            descricao,
                            urlArquivo,
                            dataInicio,
                            dataFim,
                            envios,
                            isEnviavel,
                            isEnviada,
                            isCorrigida,
                            isIndividual,
                            jId,
                            jIdEnviar,
                            jIdVisualizar,
                            disciplina
                        )
                    )
                }
            }
        }

        return tarefas
    }

    /**
     * Retorna os questionários na página de questionários do portal da disciplina.
     * Esta função retorna todos os questionários com isEnviado definido como false!
     * Para obter o questionário com todas as informações, é preciso abrir a página do questionário
     * e utilizar o getQuestionarioCompletoPaginaQuestionario().
     */
    fun getQuestionariosDisciplina(body: String, disciplina: Disciplina): ArrayList<Questionario> {
        val questionarios = ArrayList<Questionario>()

        val document = Jsoup.parse(body)
        val bodyTabela =
            document.getElementsByClass("listing").first()?.getElementsByTag("tbody")?.first()

        if (bodyTabela != null) {
            for (linha in bodyTabela.getElementsByTag("tr")) {
                val id = linha.child(4).child(0).attr("onclick").split("'")[11].toLong()
                val titulo = linha.child(0).text().trim()
                val dataInicio = formatoData.parse(linha.child(1).text())
                val dataFim = formatoData.parse(linha.child(2).text())

                questionarios.add(Questionario(id, titulo, dataInicio, dataFim, false, disciplina))
            }
        }

        return questionarios
    }

    fun getQuestionarioCompletoPaginaQuestionario(
        body: String,
        questionario: Questionario
    ): Questionario {
        val document = Jsoup.parse(body)

        // Propriedades restantes a conferir
        val isEnviado =
            document.getElementsByAttributeValue("value", "Visualizar Resultado").size > 0

        return Questionario(
            questionario.id,
            questionario.titulo,
            questionario.dataInicio,
            questionario.dataFim,
            isEnviado,
            questionario.disciplina
        )
    }

    fun isLogado(body: String): Boolean {
        val document = Jsoup.parse(body)
        return document.getElementsByClass("usuario").size > 0 || document.getElementById("painelDadosUsuario") != null
    }

    fun isPortalDiscente(body: String): Boolean {
        val document = Jsoup.parse(body)
        return document.getElementsByAttributeValueContaining(
            "action",
            "/sigaa/portais/discente/discente.jsf"
        ).size > 0
    }

    fun isPortalDisciplina(body: String): Boolean = body.contains("id=\"linkNomeTurma\"")

    fun isListaTurmas(body: String): Boolean {
        val document = Jsoup.parse(body)
        return document.getElementsByAttributeValueContaining(
            "action",
            "/sigaa/portais/discente/turmas.jsf"
        ).size > 0
    }
}
