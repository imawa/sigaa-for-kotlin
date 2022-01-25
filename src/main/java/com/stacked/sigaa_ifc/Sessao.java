package com.stacked.sigaa_ifc;

import android.content.Context;
import android.util.Log;

import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import okhttp3.FormBody;

public class Sessao {
    //TODO: Eu posso salvar a ultima página salva para acelerar tudo. Não precisaria ficar pegando a página inicial de uma disciplina toda vez que for ver cada coisa
    public static String TAG = "[SIGAA]";
    private String url_base = "sig.ifc.edu.br";
    private OkHttpClient client;

    private String JSESSIONID = null;
    private Usuario usuarioSalvo = null;

    private String usuario;
    private String senha;

    public Sessao(Context context) {
        //this.url_base = url_base.replace("/", "").replace("https:", "").replace("http:", "");

        client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor(context, this))
                .connectTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    public Sessao(PacoteSessao pacote) {
        this.url_base = pacote.getUrl_base();
        this.JSESSIONID = pacote.getJSESSIONID();
        this.usuarioSalvo = pacote.getUsuario();
        client = new OkHttpClient();
    }

    public PacoteSessao empacotarSessao() {
        return new PacoteSessao(this);
    }

    String getJSESSIONID() {
        return JSESSIONID;
    }

    String getUrlBase() {
        return url_base;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    javaxViewState(Document doc)
    O SIGAA exige um valor chamado javaxViewState em qualquer request.
    Essa função pega um documento parsado pelo jsoup e retorna o ViewState dela.
     */
    static String javaxViewState(Document doc) {
        return doc.body().getElementById("javax.faces.ViewState").attr("value");
    }

    /*
    get(url)
    Usado pra dar os requests de GET.
    */
    private Response get(String caminho) throws IOException {
        Request request = new Request.Builder()
                .url("https://" + url_base + caminho)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=" + JSESSIONID)
                .build();

        return client.newCall(request).execute();
    }

    /*
    post(url, body)
    Usado pra dar os requests de POST.
     */
    private Response post(String caminho, FormBody body) throws IOException {
        Request request = new Request.Builder()
                .url("https://" + url_base + caminho)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=" + JSESSIONID)
                .post(body)
                .build();

        return client.newCall(request).execute();
    }

    /*
    Confere se a resposta eh valida e se o SIGAA nao esta em manutencao
     */
    public static boolean respostaValida(Response r) {
        if (r != null && r.isSuccessful()) {
            if (r.priorResponse() != null && r.priorResponse().isRedirect())
                return !(r.priorResponse().headers().get("Location").contains("manutencao.html"));
            else return true;
        }
        return false;
    }

    //confere a barra que indica o nome do usuario
    private boolean usuarioLogado(Document d) {
        return usuarioLogado(d, true);
    }

    private boolean usuarioLogado(Document d, boolean conferirNome) {
        if (conferirNome) {
            if (d.getElementsByClass("usuario").size() > 0 && (d.getElementsByClass("usuario").get(0).text().equals(usuarioSalvo.getNome()) || d.getElementsByClass("usuario").get(0).text().equals(usuarioSalvo.getNomeAbreviado())))
                return true;
            if (d.getElementById("painelDadosUsuario") != null && (d.getElementById("painelDadosUsuario").text().contains(usuarioSalvo.getNome()) || d.getElementById("painelDadosUsuario").text().contains(usuarioSalvo.getNomeAbreviado())))
                return true;
        } else {
            if (d.getElementsByClass("usuario").size() > 0) return true;
            if (d.getElementById("painelDadosUsuario") != null) return true;
        }
        return false;
    }

    /**
     * Acessa a página inicial e confere se o usuário está logado
     *
     * @return true se logado; false se deslogado ou acontecer alguma exceçaõ
     */
    public boolean conferirUsuarioLogado() {
        try {
            Response r = get("/sigaa/portais/discente/discente.jsf");
            if (respostaValida(r)) {
                return !(r.priorResponse() != null && r.priorResponse().isRedirect()); //Se redirecionou é porque nao ta logado
            } else {
                throw new IOException("conferirUsuarioLogado() não foi possível se conectar com o SIGAA");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Loga a sessão atual
     *
     * @return true se logar; false se não logar
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public boolean login(final String usuario, final String senha) throws IOException {
        //JSESSIONID TODO: Acredito que dá para tirar isso aqui do JSESSIONID, enviar o post de logar direto e pegar o JSESSIONID depois
        JSESSIONID = null;
        Response responsePgLogin = get("/sigaa/verTelaLogin.do");
        if (!respostaValida(responsePgLogin))
            throw new IOException("login() resposta inválida / SIGAA em manutenção");

        JSESSIONID = responsePgLogin.header("Set-Cookie").replace("(", "").replace(")", "").split(";")[0].split("JSESSIONID=")[1];
        System.out.println(TAG + "Obtido um JSESSIONID");

        //Logar JSESSIONID
        //Body
        final FormBody body_login = new FormBody.Builder()
                .add("dispatch", "logOn")
                .add("urlRedirect", "")
                .add("subsistemaRedirect", "")
                .add("acao", "")
                .add("acessibilidade", "")
                .add("user.login", usuario)
                .add("user.senha", senha)
                .build();

        Response responseLogin = post("/sigaa/logar.do", body_login);
        if (!respostaValida(responseLogin))
            throw new IOException("login() resposta inválida / SIGAA em manutenção");

        //Usuario ou senha incorretos (retorna false)
        if (responseLogin.priorResponse() == null) {
            System.out.println(TAG + "login() sem resposta -> usuário ou senha incorretos");
            return false;
        }

        Document docRespostaLogin = Jsoup.parse(responseLogin.body().string());
        String urlRedirecionado = responseLogin.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
        if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
            urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final
        //Pular página de aviso
        while (urlRedirecionado.contains(url_base + "/sigaa/telaAvisoLogon.jsf")) {
            //TODO: Testar. Não tive oportunidade para testar após a limpeza do código
            //TODO 2: Talvez seja possível pular somente solicitando o /sigaa/verPortalDiscente.do
            System.out.println(Sessao.TAG + "login() redirecionado para um aviso");

            FormBody bodyAviso = Parsers.paginaAvisoSkipBody(docRespostaLogin);
            Response responseAviso = post("/sigaa/telaAvisoLogon.jsf", bodyAviso);
            if (!respostaValida(responseAviso))
                throw new IOException("login() resposta inválida / SIGAA em manutenção");
            if (responseAviso.priorResponse() == null)
                throw new IOException("login() não foi possível pular o aviso");

            docRespostaLogin = Jsoup.parse(responseAviso.body().string());
            urlRedirecionado = responseAviso.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
            if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
                urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final
        }
        //Pular página do questionário
        while (urlRedirecionado.contains(url_base + "/sigaa/questionarios.jsf")) {
            System.out.println(TAG + "login() redirecionado para o questionário");

            Response responseQuestionario = get("/sigaa/verPortalDiscente.do");
            if (!respostaValida(responseQuestionario))
                throw new IOException("login() resposta inválida / SIGAA em manutenção");
            if (responseQuestionario.priorResponse() == null)
                throw new IOException("login() não foi possível pular o questionário");

            docRespostaLogin = Jsoup.parse(responseQuestionario.body().string());
            urlRedirecionado = responseQuestionario.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
            if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
                urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final
        }

        //Usuário não é um discente
        if (urlRedirecionado.contains(url_base + "/sigaa/vinculos.jsf") || urlRedirecionado.contains(url_base + "/sigaa/verMenuPrincipal.do")) {
            System.out.println(TAG + "login() usuário não é um discente");
            JSESSIONID = null;
            return false;
        }

        //Conferir se logou
        if (!usuarioLogado(docRespostaLogin, false)) {
            System.out.println(TAG + "login() não foi identificado o login na página redirecionada");
            Response responsePaginaInicial = get("/sigaa/portais/discente/discente.jsf");
            if (!respostaValida(responsePaginaInicial))
                throw new IOException("login() resposta inválida / SIGAA em manutenção");

            docRespostaLogin = Jsoup.parse(responsePaginaInicial.body().string());
            if (!usuarioLogado(docRespostaLogin, false))
                throw new IOException("login() não foi possível logar");
        }

        System.out.println(TAG + "login() sem problemas");
        this.usuario = usuario;
        this.senha = senha;
        usuarioSalvo = Parsers.mainPageDadosUsuario(docRespostaLogin, url_base, usuario);
        return true;
    }

    /*
    Utilizado pelo interceptor para relogar
     */
    protected boolean login() throws IOException {
        return login(this.usuario, this.senha);
    }

    /**
     * Desloga a sessão atual (apaga o cookie e o usuário salvo)
     */
    public void deslogar() {
        JSESSIONID = null;
        usuarioSalvo = null;
    }

    public Usuario getUsuario() {
        return usuarioSalvo;
    }

    String getLogin() {
        return usuario;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Obtém todas as disciplinas que aparecem em [URL]/sigaa/portais/discente/turmas.jsf
     *
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public ArrayList<Disciplina> pegarTodasDisciplinas() throws IOException {
        final Response responsePgTurmas = get("/sigaa/portais/discente/turmas.jsf");
        if (!respostaValida(responsePgTurmas))
            throw new IOException("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

        Document docTurmas = Jsoup.parse(responsePgTurmas.body().string());
        if (!usuarioLogado(docTurmas)) throw new IOException("sessão expirada");

        return Parsers.todasTurmasVirtuais(docTurmas);
    }

    /**
     * Retorna os identificadores dos períodos que podem ser utilizados no pegarBodyBoletim(periodo)
     *
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public ArrayList<String> pegarListaPeriodosBoletim() throws IOException {
        ArrayList<String> periodos = new ArrayList<>();

        Response pgPrincipal = get("/sigaa/portais/discente/discente.jsf");
        if (!respostaValida(pgPrincipal))
            throw new IOException("pegarListaPeriodosBoletim() resposta inválida / SIGAA em manutenção");

        Document docPrincipal = Jsoup.parse(pgPrincipal.body().string());
        if (!usuarioLogado(docPrincipal)) throw new IOException("sessão expirada");

        Element menu_discente = docPrincipal.getElementById("menu:form_menu_discente");
        String jscook_action = menu_discente.getElementsByTag("div").get(0).id();
        jscook_action += ":A]#{ portalDiscente.emitirBoletim }";

        final FormBody body_listaBoletins = new FormBody.Builder()
                .add("menu:form_menu_discente", "menu:form_menu_discente")
                .add("jscook_action", jscook_action)
                .add("javax.faces.ViewState", javaxViewState(docPrincipal))
                .build();

        Response responseBoletins = post("/sigaa/portais/discente/discente.jsf", body_listaBoletins);
        if (!respostaValida(pgPrincipal))
            throw new IOException("pegarListaPeriodosBoletim() resposta inválida / SIGAA em manutenção");

        Document docBoletins = Jsoup.parse(responseBoletins.body().string());
        if (!usuarioLogado(docBoletins)) throw new IOException("sessão expirada");

        //TODO: caso ele vá direto para a página do boletim?
        Element bodyTabela = docBoletins.getElementsByClass("listagem").get(0).getElementsByTag("tbody").get(0);
        for (Element c : bodyTabela.children()) {
            periodos.add(c.child(0).text());
        }

        return periodos;
    }

    //TODO: Não cheguei a testar pra quando só tem 1 boletim

    /**
     * Retorna o body (HTML) do boletim do período inserido
     *
     * @param periodo representa a identificação do período. A lista de períodos pode ser obtida com pegarListaPeriodosBoletim()
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public String pegarBodyBoletim(String periodo) throws IOException {
        //Página principal
        Response pgPrincipal = get("/sigaa/portais/discente/discente.jsf");
        if (!respostaValida(pgPrincipal))
            throw new IOException("pegarBodyBoletim() resposta inválida / SIGAA em manutenção");

        Document docPrincipal = Jsoup.parse(pgPrincipal.body().string());
        if (!usuarioLogado(docPrincipal)) throw new IOException("sessão expirada");

        //Página de boletins
        Element menu_discente = docPrincipal.getElementById("menu:form_menu_discente");
        String jscook_action = menu_discente.getElementsByTag("div").get(0).id();
        jscook_action += ":A]#{ portalDiscente.emitirBoletim }";

        final FormBody body_listaBoletins = new FormBody.Builder()
                .add("menu:form_menu_discente", "menu:form_menu_discente")
                .add("jscook_action", jscook_action)
                .add("javax.faces.ViewState", javaxViewState(docPrincipal))
                .build();

        Response responseBoletins = post("/sigaa/portais/discente/discente.jsf", body_listaBoletins);
        if (!respostaValida(pgPrincipal))
            throw new IOException("pegarBodyBoletim() resposta inválida / SIGAA em manutenção");

        Document docBoletins = Jsoup.parse(responseBoletins.body().string());
        if (!usuarioLogado(docBoletins)) throw new IOException("sessão expirada");

        if (docBoletins.getElementsByAttributeValue("title", "Selecionar Ano Escolar").size() > 0) {
            //Abriu a página para seleção de boletim (quando tem mais de 1)
            Log.d(TAG, "pegarBodyBoletim: lista de boletins");
            //Página do boletim em questão
            String onClickBotaoBoletim = docBoletins.getElementsByClass("listagem").get(0).getElementsByTag("tbody").get(0).getElementsContainingText(periodo).get(1).getElementsByTag("a").get(0).attr("onclick");

            final FormBody body_boletim = new FormBody.Builder()
                    .add("form", "form")
                    .add("javax.faces.ViewState", javaxViewState(docBoletins))
                    .add(onClickBotaoBoletim.split("'")[5], onClickBotaoBoletim.split("'")[7])
                    .add("anoEscolar", onClickBotaoBoletim.split("'")[11])
                    .build();

            Response responseBoletim = post("/sigaa/ensino/tecnico_integrado/boletim/selecao.jsf", body_boletim);
            if (!respostaValida(responseBoletim))
                throw new IOException("pegarBodyBoletim() resposta inválida / SIGAA em manutenção");

            return responseBoletim.body().string();
        } else {
            //Foi direto para a página do boletim (quando só tem 1)
            Log.d(TAG, "pegarBodyBoletim: boletim único");
            return responseBoletins.body().string();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //O método para acessar uma turma da página principal é diferente de uma da página com todas as turmas
    private Document acessarPaginaTurmaVirtual(Disciplina d) throws IOException {
        if (d == null) return null;

        String caminhoGet = (!d.isRetiradoDaPaginaTodasTurmasVirtuais()) ? "/sigaa/portais/discente/discente.jsf" : "/sigaa/portais/discente/turmas.jsf", caminhoPost = (!d.isRetiradoDaPaginaTodasTurmasVirtuais()) ? "/sigaa/portais/discente/discente.jsf#" : "/sigaa/portais/discente/turmas.jsf";

        final Response G = get(caminhoGet);
        if (!respostaValida(G))
            throw new IOException("acessarPaginaTurmaVirtual() resposta invalida / SIGAA em manutenção");

        //Pegar página inicial da disciplina
        Document D = Jsoup.parse(G.body().string());
        if (!usuarioLogado(D)) throw new IOException("sessão expirada");

        FormBody body_disciplina = (!d.isRetiradoDaPaginaTodasTurmasVirtuais()) ? new FormBody.Builder()
                .add(d.getPostArgs()[0], d.getPostArgs()[0])
                .add("javax.faces.ViewState", javaxViewState(D))
                .add(d.getPostArgs()[1], d.getPostArgs()[1])
                .add("frontEndIdTurma", d.getPostArgs()[2])
                .build()
                :
                new FormBody.Builder()
                        .add(d.getPostArgs()[0], d.getPostArgs()[0])
                        .add("javax.faces.ViewState", javaxViewState(D))
                        .add(d.getPostArgs()[1], d.getPostArgs()[1])
                        .add("frontEndIdTurma", d.getPostArgs()[2])
                        .add("inciadoPelaBusca", "true")
                        .add("paginaListaTurmasOrigem", "/portais/discente/turmas.jsp")
                        .build();

        Response R = post(caminhoPost, body_disciplina);
        if (!respostaValida(R))
            throw new IOException("acessarPaginaTurmaVirtual() resposta invalida / SIGAA em manutenção");

        Document pgDisciplina = Jsoup.parse(R.body().string());
        if (!usuarioLogado(pgDisciplina)) throw new IOException("sessão expirada");

        return pgDisciplina;
    }

    //Usado para pegar algum dos botoes no usuario salvo. Se nao encontrar nele, procura na pagina
    private BotaoDocumento getBotao(idBotaoDocumento id, Document d) {
        if (usuarioSalvo.botao(id) != null) return usuarioSalvo.botao(id);

        ArrayList<BotaoDocumento> botoes = Parsers.paginaDisciplinaBotoes(d);
        for (BotaoDocumento b : botoes) {
            usuarioSalvo.adicionarBotao(b);
        }
        return usuarioSalvo.botao(id);
    }

    private Document disciplinaAcessarBotaoMenu(Disciplina d, idBotaoDocumento bt) throws IOException {
        return disciplinaAcessarBotaoMenu(d, bt, true);
    }

    private Document disciplinaAcessarBotaoMenu(Disciplina d, idBotaoDocumento bt, boolean conferirSessao) throws IOException {
        if (d == null) return null;

        Document pgDisciplina = acessarPaginaTurmaVirtual(d);

        //Pegar os dados do botão em questão
        BotaoDocumento BOTAO = getBotao(bt, pgDisciplina);
        if (BOTAO == null) {
            //TODO: Arrumar esse negocio horrivel aqui (botão não estava salvo e página do SIGAA retornou somente com a mensagem de "carregando")
            pgDisciplina = acessarPaginaTurmaVirtual(d);
            BOTAO = getBotao(bt, pgDisciplina);
            if (BOTAO == null)
                throw new IOException("disciplinaAcessarBotaoMenu() página da turma virtual não carregou");
        }

        //Body
        FormBody bodyBotao = new FormBody.Builder()
                .add("formMenu", "formMenu")
                .add(BOTAO.j_id_jsp()[0][0], BOTAO.j_id_jsp()[0][1])
                .add("javax.faces.ViewState", javaxViewState(pgDisciplina))
                .add(BOTAO.j_id_jsp()[1][0], BOTAO.j_id_jsp()[1][1])
                .build();

        Response responseBotao = post("/sigaa/ava/index.jsf", bodyBotao);
        if (!respostaValida(responseBotao))
            throw new IOException("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

        Document docPgBotao = Jsoup.parse(responseBotao.body().string());
        if (conferirSessao && !usuarioLogado(docPgBotao))
            throw new IOException("disciplinaAcessarBotaoMenu() sessão expirada");

        return docPgBotao;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Aula> disciplinaPegarAulas(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document pgDisciplina = acessarPaginaTurmaVirtual(d);
        return Parsers.paginaDisciplinaAulas(pgDisciplina, d);
    }

    public ArrayList<Participante> disciplinaPegarParticipantes(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docParticipantes = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_PARTICIPANTES);
        return Parsers.paginaDisciplinaParticipantes(docParticipantes, url_base);
    }

    public ArrayList<Nota> disciplinaPegarNotas(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docNotas = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_NOTAS, false);
        return Parsers.paginaNotasDisciplinaNotas(docNotas, d);
    }

    public ArrayList<InfoArquivo> disciplinaPegarListaArquivos(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docArquivos = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_ARQUIVOS);
        return Parsers.paginaDisciplinaArquivos(docArquivos, d);
    }

    public ArrayList<Avaliacao> disciplinaPegarAvaliacoes(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docAvaliacoes = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_AVALIACOES);
        return Parsers.paginaDisciplinaAvaliacoes(docAvaliacoes, d);
    }

    public ArrayList<Questionario> disciplinaPegarQuestionarios(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docQuestionarios = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_QUESTIONARIOS);
        ArrayList<Questionario> questionarios = Parsers.paginaDisciplinaQuestionarios(docQuestionarios, d);

        Date dataAtual = new Date();
        for (int i = 0; i < questionarios.size(); i++) {
            if (questionarios.get(i).getDataInicio().before(dataAtual) && questionarios.get(i).getDataFim().after(dataAtual)) {
                Response responseQuestionario = disciplinaAbrirEnvioQuestionario(questionarios.get(i), d);
                if (!respostaValida(responseQuestionario)) {
                    throw new IOException("disciplinaPegarQuestionarios() resposta inválida / SIGAA em manutenção");
                }

                Document docQuestionario = Jsoup.parse(responseQuestionario.body().string());
                if (!usuarioLogado(docQuestionario)) {
                    throw new IOException("disciplinaPegarQuestionarios() sessão expirada");
                }

                if (Parsers.paginaQuestionarioIsRespondido(docQuestionario)) {
                    questionarios.get(i).setEnviado(true);
                }
            }
        }

        return questionarios;
    }

    public ArrayList<Tarefa> disciplinaPegarTarefas(Disciplina d) throws IOException {
        if (d == null) return new ArrayList<>();

        Document docTarefas = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_TAREFAS);
        return Parsers.paginaTarefasDisciplinaTarefas(docTarefas, url_base, d);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private Response disciplinaAbrirEnvioTarefa(Tarefa tarefa) throws IOException {
        if (tarefa == null || !tarefa.isEnviavel()) return null;

        if (tarefa.getDisciplina().getId() != null) {
            // [1] Método rápido (usado pras disciplinas visíveis na página principal): página principal -> página tarefa

            final Response responsePgDiscente = get("/sigaa/portais/discente/discente.jsf");
            if (!respostaValida(responsePgDiscente))
                throw new IOException("disciplinaAbrirEnvioTarefa() resposta inválida / SIGAA em manutenção");

            Document docPgDiscente = Jsoup.parse(responsePgDiscente.body().string());
            if (!usuarioLogado(docPgDiscente))
                throw new IOException("disciplinaAbrirEnvioTarefa() sessão expirada");

            FormBody bodyTarefa = new FormBody.Builder()
                    .add("formAtividades", "formAtividades")
                    .add("javax.faces.ViewState", javaxViewState(docPgDiscente))
                    .add("formAtividades:visualizarTarefaTurmaVirtual", "formAtividades:visualizarTarefaTurmaVirtual")
                    .add("id", tarefa.getId())
                    .add("idTurma", tarefa.getDisciplina().getId())
                    .build();

            return post("/sigaa/portais/discente/discente.jsf#", bodyTarefa);
        } else {
            // [2] Método lento (usado pras disciplinas não visíveis na principal): página principal -> página turma virtual -> página tarefas -> página tarefas
            //TODO: Quando tiver oportunidade, conferir se isso realmente funciona
            Document docTarefas = disciplinaAcessarBotaoMenu(tarefa.getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);

            FormBody bodyTarefa = new FormBody.Builder()
                    .add(tarefa.getPostArgsEnviar()[0], tarefa.getPostArgsEnviar()[0])
                    .add("javax.faces.ViewState", javaxViewState(docTarefas))
                    .add(tarefa.getPostArgsEnviar()[1], tarefa.getPostArgsEnviar()[1])
                    .add("id", tarefa.getId())
                    .build();

            return post("/sigaa/portais/discente/discente.jsf#", bodyTarefa);
        }
    }

    private Response disciplinaAbrirEnvioQuestionario(Questionario questionario, Disciplina disciplina) throws IOException {
        if (questionario == null || disciplina == null) {
            return null;
        }

        final Response responsePgDiscente = get("/sigaa/portais/discente/discente.jsf");
        if (!respostaValida(responsePgDiscente)) {
            throw new IOException("disciplinaAbrirEnvioQuestionario() resposta inválida / SIGAA em manutenção");
        }

        Document docPgDiscente = Jsoup.parse(responsePgDiscente.body().string());
        if (!usuarioLogado(docPgDiscente)) {
            throw new IOException("disciplinaAbrirEnvioTarefa() sessão expirada");
        }

        FormBody bodyQuestionario = new FormBody.Builder()
                .add("formAtividades", "formAtividades")
                .add("javax.faces.ViewState", javaxViewState(docPgDiscente))
                .add("formAtividades:visualizarQuestionarioTurmaVirtual", "formAtividades:visualizarQuestionarioTurmaVirtual")
                .add("id", Long.toString(questionario.getId()))
                .add("idTurma", (disciplina.isRetiradoDaPaginaTodasTurmasVirtuais()) ? disciplina.getId() : "31464") //Por enquanto, não tem problema utilizar o id de uma turma pública, mas acho que é mais garantido futuramente só pegar as disciplinas da página com todas
                .build();

        return post("/sigaa/portais/discente/discente.jsf#", bodyQuestionario);
    }

    //Obtem o form de uma tarefa, que é usado pra enviar a tarefa
    public FormTarefa disciplinaObterFormTarefa(Tarefa tarefa) throws IOException {
        if (tarefa == null || !tarefa.isEnviavel()) return null;

        Response respostaPgTarefa = disciplinaAbrirEnvioTarefa(tarefa);
        if (!respostaValida(respostaPgTarefa))
            throw new IOException("disciplinaObterFormTarefa() resposta inválida / SIGAA em manutenção");

        Document docTarefa = Jsoup.parse(respostaPgTarefa.body().string());
        if (!usuarioLogado(docTarefa))
            throw new IOException("disciplinaObterFormTarefa() sessão expirada");
        ////////////////////////////////////////////////////////
        //Conferir o que a tarefa aceita & requer
        boolean input_arquivo = false, input_comentarios = false, input_arquivo_obrigatorio = false;
        if (docTarefa.getElementsByClass("form").size() > 0 && docTarefa.getElementsByClass("form").first().getElementsByTag("label").size() > 0) {
            Element eForm = docTarefa.getElementsByClass("form").first();
            if (eForm.getElementsByAttributeValueContaining("name", "idComentarios").size() == 1)
                input_arquivo = true;
            if (eForm.getElementsByAttributeValueContaining("name", "idArquivo").size() == 1)
                input_comentarios = true;
            // Algumas tarefas requerem o campo de arquivo
            if (input_arquivo) {
                if (eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").size() > 0) {
                    input_arquivo_obrigatorio = (eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").first().getElementsByAttributeValueContaining("class", "required").size() == 1);
                }
            }
            //TODO: Conferir campo de resposta e sua obrigatoriedade quando haver algum
            //TODO: Conferir se campo de resposta não é obrigatório quando haver algum
        } else
            throw new IOException("disciplinaObterFormTarefa() página da tarefa acessada não possui um form");
        ////////////////////////////////////////////////////////
        //Pegar os inputs do form escondido da página
        String j_id_jsp = "";
        ArrayList<String> infoEscondidaForm = new ArrayList<>();
        for (Element i : docTarefa.getElementsByClass("responderTarefa").get(0).getElementsByTag("input")) {
            if (i.attr("type").equals("hidden")) {
                //j_id_jsp
                if (j_id_jsp.equals("")) {
                    j_id_jsp = i.attr("name").split(":")[0];
                }
                //informacoes
                infoEscondidaForm.add(i.attr("name"));
                infoEscondidaForm.add(i.attr("value"));
            }
        }
        ////////////////////////////////////////////////////////
        return new FormTarefa(tarefa, j_id_jsp, infoEscondidaForm, input_comentarios, input_arquivo, input_arquivo_obrigatorio, false, false); //TODO: campo de resposta
    }

    //TODO: Testar. Não cheguei a testar essa função após a limpeza do código
    public boolean disciplinaEnviarTarefa(FormTarefa formTarefa) throws IOException {
        if (formTarefa.getTarefa() == null || !formTarefa.getTarefa().isEnviavel()) return false;

        Response responsePgTarefa = disciplinaAbrirEnvioTarefa(formTarefa.getTarefa());
        if (!respostaValida(responsePgTarefa))
            throw new IOException("disciplinaEnviarTarefa() resposta inválida / SIGAA em manutenção");

        Document docTarefa = Jsoup.parse(responsePgTarefa.body().string());
        if (!usuarioLogado(docTarefa))
            throw new IOException("disciplinaEnviarTarefa() sessão expirada");

        ////Conferir se preenche a tarefa corretamente
        //Requer arquivo, mas não há arquivo
        if (formTarefa.arquivoObrigatorio() && (formTarefa.getArquivo() == null || formTarefa.getNomeArquivo() == null || formTarefa.getNomeArquivo() == ""))
            throw new IOException("disciplinaEnviarTarefa() formulário preenchido incorretamente");
        //Não aceita arquivo, mas há algum arquivo
        if (!formTarefa.aceitaArquivo() && (formTarefa.getArquivo() != null || formTarefa.getNomeArquivo() != null))
            throw new IOException("disciplinaEnviarTarefa() formulário preenchido incorretamente");
        //TODO: Não aceita comentário
        if (!formTarefa.aceitaComentarios())
            throw new IOException("disciplinaEnviarTarefa() formulário não aceita comentário (?)");
        //Requer resposta, mas não tem resposta
        if (formTarefa.respostaObrigatoria() && (formTarefa.getResposta() == null || formTarefa.getResposta() == ""))
            throw new IOException("disciplinaEnviarTarefa() formulário preenchido incorretamente");
        //Não aceita resposta, mas tem resposta
        if (!formTarefa.aceitaResposta() && (formTarefa.getResposta() != "" && formTarefa.getResposta() != null))
            throw new IOException("disciplinaEnviarTarefa() formulário preenchido incorretamente");

        // MultipartBody pro envio da tarefa
        //TODO: Body que envia campo de resposta
        //TODO 2: Body pra quando não precisa de arquivo
        RequestBody body_envioTarefa = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(formTarefa.j_id_jsp(), formTarefa.j_id_jsp())
                .addFormDataPart(formTarefa.infoEscondidaForm().get(0), formTarefa.infoEscondidaForm().get(1))
                .addFormDataPart(formTarefa.infoEscondidaForm().get(2), formTarefa.infoEscondidaForm().get(3))
                .addFormDataPart(formTarefa.infoEscondidaForm().get(4), formTarefa.infoEscondidaForm().get(5))
                .addFormDataPart(formTarefa.infoEscondidaForm().get(6), formTarefa.infoEscondidaForm().get(7))
                .addFormDataPart(formTarefa.infoEscondidaForm().get(8), formTarefa.infoEscondidaForm().get(9))
                .addFormDataPart(formTarefa.infoEscondidaForm().get(10), formTarefa.infoEscondidaForm().get(11))
                .addFormDataPart(formTarefa.j_id_jsp() + ":idArquivo", formTarefa.getNomeArquivo(), RequestBody.create(MediaType.parse("application/octet-stream"), formTarefa.getArquivo()))
                .addFormDataPart(formTarefa.j_id_jsp() + ":idComentarios", formTarefa.getComentarios())
                .addFormDataPart(formTarefa.j_id_jsp() + ":idEnviar", "Enviar")
                .addFormDataPart("javax.faces.ViewState", javaxViewState(docTarefa))
                .build();

        Request requestEnvioTarefa = new Request.Builder()
                .url("https://" + url_base + "/sigaa/ava/TarefaTurma/enviarTarefa.jsf")
                .header("Content-Type", "multipart/form-data")
                .header("Cookie", "JSESSIONID=" + JSESSIONID)
                .post(body_envioTarefa)
                .build();

        Response responseEnvioTarefa = client.newCall(requestEnvioTarefa).execute();
        if (!respostaValida(responseEnvioTarefa))
            throw new IOException("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

        //Confirmação de envio
        Document docEnvio = Jsoup.parse(responseEnvioTarefa.body().string());
        if (!usuarioLogado(docEnvio))
            throw new IOException("disciplinaAcessarBotaoMenu() sessão expirada");

        if (docEnvio.getElementsByClass("info").size() > 0 && docEnvio.getElementsByClass("info").first().children().size() > 0 && docEnvio.getElementsByClass("info").first().children().first().text().equals("Operação realizada com sucesso!")) {
            System.out.println(TAG + "disciplinaEnviarTarefa() Identificou confirmação de envio. Tarefa enviada com sucesso");
            return true;
        } else {
            System.out.println(TAG + "disciplinaEnviarTarefa() Não identificou o pop-up de confirmação de envio");
            return false;
        }
    }

    public EnvioTarefa disciplinaPegarEnvioTarefa(Tarefa t) throws IOException {
        //Acessar página de tarefas
        Document docTarefas = disciplinaAcessarBotaoMenu(t.getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);
        if (!usuarioLogado(docTarefas))
            throw new IOException("disciplinaPegarEnvioTarefa() sessão expirada");

        //Body para ver o envio
        FormBody body_envioTarefa = new FormBody.Builder()
                .add(t.getPostArgsVisualizar()[0], t.getPostArgsVisualizar()[0])
                .add("javax.faces.ViewState", javaxViewState(docTarefas))
                .add(t.getPostArgsVisualizar()[1], t.getPostArgsVisualizar()[1])
                .add("id", t.getPostArgsVisualizar()[2])
                .build();

        Response responseEnvioTarefa = post("/sigaa/ava/TarefaTurma/listar.jsf", body_envioTarefa);
        if (!respostaValida(responseEnvioTarefa))
            throw new IOException("disciplinaPegarEnvioTarefa() resposta inválida / SIGAA em manutenção");

        Document docEnvioTarefa = Jsoup.parse(responseEnvioTarefa.body().string());
        if (!usuarioLogado(docEnvioTarefa))
            throw new IOException("disciplinaPegarEnvioTarefa() sessão expirada");

        return Parsers.paginaTarefaEnvioTarefa(docEnvioTarefa, t, url_base);
    }

    //TODO  Transforma o AnexoTarefa em Tarefa. Isso aqui tá horrivel. Atualmente essa foi a solução mais fácil pra não precisar modificar o resto do código. A solução que envolve mudar o código provavelmente vai ser armazenando somente o id da tarefa e fazendo o processo a partir somente dele
    //      2: acho que eu poderia deixar essa mesmo caso a api guarde e ultilize da ultima pagina pra acelerar as coisas. aí não perderia muito tempo no final
    public Tarefa disciplinaPegarTarefa(AnexoTarefa tarefa) throws IOException {
        Document docTarefas = disciplinaAcessarBotaoMenu(tarefa.getAula().getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);

        ArrayList<Tarefa> tarefas = Parsers.paginaTarefasDisciplinaTarefas(docTarefas, url_base, tarefa.getAula().getDisciplina());

        if (tarefas.size() > 0) {
            //Conferir pelo id
            for (Tarefa t : tarefas) {
                if (t.getId().equals(tarefa.getId())) return t;
            }

            //Conferir pelo titulo (quando a tarefa não é enviável). Há chance de não ser a mesma, mas a tarefa já não é enviável de qualquer maneira aqui
            for (Tarefa t : tarefas) {
                if (t.getTitulo().equals(tarefa.getTitulo())) return t;
            }
        }

        throw new IOException("disciplinaPegarTarefa() Tarefa inexistente");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Utilizado para baixar algum arquivo do SIGAA (retorna uma classe com o nome e bytes do arquivo)
     *
     * @param a pode ser obtido ao consultar a lista de arquivos de uma disciplina
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public Arquivo disciplinaBaixarArquivo(InfoArquivo a) throws IOException {
        if (a == null) return null;

        Document docArquivos = disciplinaAcessarBotaoMenu(a.getDisciplina(), idBotaoDocumento.DISC_ARQUIVOS);

        FormBody bodyAq = new FormBody.Builder()
                .add("formAva", "formAva")
                .add("javax.faces.ViewState", javaxViewState(docArquivos))
                .add(a.getJ_id_jsp(), a.getJ_id_jsp())
                .add("id", a.getId())
                .build();

        Response responseArquivo = post("/sigaa/ava/ArquivoTurma/listar_discente.jsf", bodyAq);

        String nomeArquivo = responseArquivo.header("Content-Disposition").split("filename=\"")[1];
        nomeArquivo = nomeArquivo.substring(0, nomeArquivo.length() - 1);
        byte[] bytes = IOUtils.toByteArray(responseArquivo.body().byteStream());

        return new Arquivo(nomeArquivo, bytes);
    }

    /**
     * Utilizado para baixar algum arquivo do SIGAA (retorna uma classe com o nome e bytes do arquivo)
     *
     * @param a é o anexo de alguma aula, que pode ser obtida ao consultar as aulas de alguma disciplina
     * @throws IOException se acontecer algum problema por parte da api ou a internet não estiver disponível
     */
    public Arquivo disciplinaBaixarArquivo(AnexoInfoArquivo a) throws IOException {
        if (a == null) return null;

        Document pgDisciplina = acessarPaginaTurmaVirtual(a.getAula().getDisciplina());

        FormBody bodyAq = new FormBody.Builder()
                .add("formAva", "formAva")
                .add("formAva:idTopicoSelecionado", pgDisciplina.getElementById("formAva:idTopicoSelecionado").attr("value"))
                .add("javax.faces.ViewState", javaxViewState(pgDisciplina))
                .add(a.getJ_id_jsp(), a.getJ_id_jsp())
                .add("id", a.getId())
                .build();

        Response responseArquivo = post("/sigaa/ava/index.jsf", bodyAq);

        String nomeArquivo = responseArquivo.header("Content-Disposition").split("filename=\"")[1];
        nomeArquivo = nomeArquivo.substring(0, nomeArquivo.length() - 1);
        byte[] bytes = IOUtils.toByteArray(responseArquivo.body().byteStream());

        return new Arquivo(nomeArquivo, bytes);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //TODO: Parei para pensar aqui: as informações daqui talvez não sejam muito uteis por enquanto para o aplicativo, então vou deixar isso para adicionar algum outro dia se precisar
    /*
    usuarioCompleto()
    Obtém as informações do usuário da página principal, meus dados, dos botões do SIGAA (meus dados e disciplinas) e retorna
     *//*
    public usuario usuarioCompleto() {
        ////**1** PEGAR MAIN PAGE
        try {
            Response G = get("/sigaa/portais/discente/discente.jsf");
            if (respostaValida(G)) {
                Document d = Jsoup.parse(G.body().string());
                ////INFORMACOES DA MAIN PAGE
                //Disciplinas
                final disciplina[] disciplinas = parsers.mainPageDisciplinas(d);
                //Outras informacoes
                final String nome = d.body().getElementsByClass("nome").get(0).text();
                final String campus = d.body().getElementsByClass("unidade").get(0).text();
                //TODO: AVATAR
                //Botao "meus dados"
                final String j_id_jsp_meusDados = d.body().getElementsByAttributeValueContaining("id", "meusDadosPessoais").get(0).attr("id").split(":")[0];
                final botaoDocumento meusDados = new botaoDocumento(idBotaoDocumento.MEUS_DADOS, new String[][]{{j_id_jsp_meusDados, j_id_jsp_meusDados}, {j_id_jsp_meusDados + ":meusDadosPessoais", j_id_jsp_meusDados + ":meusDadosPessoais"}});

                ////**2** PEGAR A PAGINA DO "MEUS DADOS"
                FormBody body_dados = new FormBody.Builder()
                        .add(meusDados.j_id_jsp()[0][0], meusDados.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(d))
                        .add(meusDados.j_id_jsp()[1][0], meusDados.j_id_jsp()[1][0])
                        .build();

                Response P = post("/sigaa/portais/discente/discente.jsf", body_dados);
                if (respostaValida(P)) {
                    Document m = Jsoup.parse(P.body().string());
                    //Informação restante nos "meus dados"
                    final int matricula = Integer.parseInt(m.body().getElementsContainingText("Matrícula:").last().lastElementSibling().text());
                    final String email = m.body().getElementById("formDiscente:txtEmail").attr("value");

                    //**3** Solicitar uma disciplina se houver e salvar os botoes
                    if (disciplinas.length > 0) {
                        final Response mainDisciplina = disciplinaPaginaDisciplina(disciplinas[0]);
                        if (mainDisciplina != null && respostaValida(mainDisciplina)) {
                            Document DI = Jsoup.parse(mainDisciplina.body().string());
                            //Juntar o botaoDocumento meus Dados + botoesDisciplina
                            ArrayList<botaoDocumento> botoesUsuario = parsers.paginaDisciplinaBotoes(DI);
                            botoesUsuario.add(meusDados);
                            //botaoDocumento[] botoesUsuario = Arrays.copyOf(botoesDisciplina, botoesDisciplina.length + 1);
                            //botoesUsuario[0] = meusDados;
                            //System.arraycopy(botoesDisciplina, 0, botoesUsuario, 1, botoesDisciplina.length);

                            usuarioSalvo = new usuario(nome, campus, email, matricula, disciplinas, botoesUsuario);
                            return usuarioSalvo;
                        } else {
                            System.out.println(TAG + "inicializarUsuario() POST disciplina sem sucesso");
                            return null;
                        }
                    } else {
                        //todo: usuario sem disciplinas
                        //Usuario = new usuario(nome, campus, email, matricula, new botaoDocumento[] {meusDados});
                        System.out.println(TAG + logado());
                        ArrayList<botaoDocumento> botoesUsuario = new ArrayList<botaoDocumento>();
                        botoesUsuario.add(meusDados);
                        usuarioSalvo = new usuario(nome, campus, email, matricula, botoesUsuario);
                        return usuarioSalvo;
                    }
                } else {
                   System.out.println(TAG + "inicializarUsuario() POST meus dados sem sucesso");
                    return null;
                }
            } else {
                System.out.println(TAG + "inicializarUsuario() GET main page sem sucesso");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(TAG + "inicializarUsuario() " + e);
            return null;
        }
    }*/
}