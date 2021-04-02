package com.stacked.sigaa_ifc;

import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import okhttp3.FormBody;

public class Sessao {
    public static String logMSG = "Debug API: ";
    private String url_base;
    private OkHttpClient client;

    private String JSESSIONID = null;
    private Usuario usuarioSalvo = null;

    public Sessao(String url_base) {
        this.url_base = url_base.replace("/", "").replace("https:", "").replace("http:", "");
        client = new OkHttpClient();
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

        //TODO: preciso .addInterceptor() pra retentar as que falham, tipo disciplina e conexao lenta

        return client.newCall(request).execute();
    }

    /*
    Confere se a resposta eh valida e se o SIGAA nao esta em manutencao
     */
    private boolean respostaValida(Response r) {
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
            if (d.getElementsByClass("usuario").size() > 0 && d.getElementsByClass("usuario").get(0).text().equals(usuarioSalvo.getNome()))
                return true;
            if (d.getElementById("painelDadosUsuario") != null && d.getElementById("painelDadosUsuario").text().contains(usuarioSalvo.getNome()))
                return true;
        } else {
            if (d.getElementsByClass("usuario").size() > 0) return true;
            if (d.getElementById("painelDadosUsuario") != null) return true;
        }
        return false;
    }

    //confere a pagina inicial
    public boolean usuarioLogado() {
        try {
            Response r = get("/sigaa/portais/discente/discente.jsf");
            if (respostaValida(r)) {
                return !(r.priorResponse() != null && r.priorResponse().isRedirect()); //Se redirecionou é porque nao ta logado
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    login(usuario, senha);
    Loga a sessão e um usuário não completo (com os dados disponíveis na página principal) se logar corretamente. Retorna null se acontecer algum erro
    */
    //TODO Tenho que limpar esse código. Principalmente pular mais de 1 aviso
    public Usuario login(final String usuario, final String senha) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        try {
            //JSESSIONID
            JSESSIONID = null;
            Response responsePgLogin = get("/sigaa/verTelaLogin.do");
            if (!respostaValida(responsePgLogin))
                throw new ExcecaoSIGAA("login() resposta inválida / SIGAA em manutenção");

            JSESSIONID = responsePgLogin.header("Set-Cookie").replace("(", "").replace(")", "").split(";")[0].split("JSESSIONID=")[1];
            System.out.println(logMSG + "Obtido um JSESSIONID");

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
                throw new ExcecaoSIGAA("login() resposta inválida / SIGAA em manutenção");

            //Usuario ou senha incorretos (retorna null)
            if (responseLogin.priorResponse() == null) {
                System.out.println(logMSG + "login() sem resposta -> usuário ou senha incorretos");
                return null;
            }


            Document docRespostaLogin = Jsoup.parse(responseLogin.body().string());
            String urlRedirecionado = responseLogin.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
            if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
                urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final
            //Pular página de aviso
            while (urlRedirecionado.contains(url_base + "/sigaa/telaAvisoLogon.jsf")) {
                //TODO: Testar. Não tive oportunidade para testar após a limpeza do código
                System.out.println(Sessao.logMSG + "login() redirecionado para um aviso");

                FormBody bodyAviso = Parsers.paginaAvisoSkipBody(docRespostaLogin);
                Response responseAviso = post("/sigaa/telaAvisoLogon.jsf", bodyAviso);
                if (!respostaValida(responseAviso))
                    throw new ExcecaoSIGAA("login() resposta inválida / SIGAA em manutenção");
                if (responseAviso.priorResponse() == null)
                    throw new ExcecaoSIGAA("login() não foi possível pular o aviso");

                docRespostaLogin = Jsoup.parse(responseAviso.body().string());
                urlRedirecionado = responseAviso.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
                if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
                    urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final
            }

            //Conferir se logou
            if (!usuarioLogado(docRespostaLogin, false)) {
                System.out.println(logMSG + "login() não foi identificado o login na página redirecionada");
                Response responsePaginaInicial = get("/sigaa/portais/discente/discente.jsf");
                if (!respostaValida(responsePaginaInicial))
                    throw new ExcecaoSIGAA("login() resposta inválida / SIGAA em manutenção");

                Document docPgDiscente = Jsoup.parse(responsePaginaInicial.body().string());
                if (!usuarioLogado(docPgDiscente, false))
                    throw new ExcecaoSessaoExpirada("login() não foi possível logar");
            }

            System.out.println(logMSG + "login() sem problemas");
            usuarioSalvo = Parsers.mainPageDadosUsuario(docRespostaLogin, url_base, usuario);
            return usuarioSalvo;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("login() IOException");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Disciplina> pegarTodasDisciplinas() throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        try {
            final Response responsePgTurmas = get("/sigaa/portais/discente/turmas.jsf");
            if (!respostaValida(responsePgTurmas))
                throw new ExcecaoSIGAA("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

            Document docTurmas = Jsoup.parse(responsePgTurmas.body().string());
            if (!usuarioLogado(docTurmas)) throw new ExcecaoSessaoExpirada("sessão expirada");

            return Parsers.todasTurmasVirtuais(docTurmas);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("obterTodasDisciplinas() IOException");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //O método para acessar uma turma da página principal é diferente de uma da página com todas as turmas
    private Document acessarPaginaTurmaVirtual(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return null;

        try {
            String caminhoGet = (!d.paginaTodasTurmasVirtuais()) ? "/sigaa/portais/discente/discente.jsf" : "/sigaa/portais/discente/turmas.jsf", caminhoPost = (!d.paginaTodasTurmasVirtuais()) ? "/sigaa/portais/discente/discente.jsf#" : "/sigaa/portais/discente/turmas.jsf";

            final Response G = get(caminhoGet);
            if (!respostaValida(G))
                throw new ExcecaoSIGAA("acessarPaginaTurmaVirtual() resposta invalida / SIGAA em manutenção");

            //Pegar página inicial da disciplina
            Document D = Jsoup.parse(G.body().string());
            if (!usuarioLogado(D)) throw new ExcecaoSessaoExpirada("sessão expirada");

            FormBody body_disciplina = (!d.paginaTodasTurmasVirtuais()) ? new FormBody.Builder()
                    .add(d.postArgs()[0], d.postArgs()[0])
                    .add("javax.faces.ViewState", javaxViewState(D))
                    .add(d.postArgs()[1], d.postArgs()[1])
                    .add("frontEndIdTurma", d.postArgs()[2])
                    .build()
                    :
                    new FormBody.Builder()
                            .add(d.postArgs()[0], d.postArgs()[0])
                            .add("javax.faces.ViewState", javaxViewState(D))
                            .add(d.postArgs()[1], d.postArgs()[1])
                            .add("frontEndIdTurma", d.postArgs()[2])
                            .add("inciadoPelaBusca", "true")
                            .add("paginaListaTurmasOrigem", "/portais/discente/turmas.jsp")
                            .build();

            Response R = post(caminhoPost, body_disciplina);
            if (!respostaValida(R))
                throw new ExcecaoSIGAA("acessarPaginaTurmaVirtual() resposta invalida / SIGAA em manutenção");

            Document pgDisciplina = Jsoup.parse(R.body().string());
            if (!usuarioLogado(pgDisciplina)) throw new ExcecaoSessaoExpirada("sessão expirada");

            return pgDisciplina;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("acessarPaginaTurmaVirtual() IOException");
        }
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

    private Document disciplinaAcessarBotaoMenu(Disciplina d, idBotaoDocumento bt) throws ExcecaoAPI, ExcecaoSIGAA, ExcecaoSessaoExpirada {
        return disciplinaAcessarBotaoMenu(d, bt, true);
    }

    private Document disciplinaAcessarBotaoMenu(Disciplina d, idBotaoDocumento bt, boolean conferirSessao) throws ExcecaoAPI, ExcecaoSIGAA, ExcecaoSessaoExpirada {
        if (d == null) return null;

        Document pgDisciplina = acessarPaginaTurmaVirtual(d);

        //Pegar os dados do botão em questão
        BotaoDocumento BOTAO = getBotao(bt, pgDisciplina);
        if (BOTAO == null) {
            //TODO: Arrumar esse negocio horrivel aqui (botão não estava salvo e página do SIGAA retornou somente com a mensagem de "carregando")
            pgDisciplina = acessarPaginaTurmaVirtual(d);
            BOTAO = getBotao(bt, pgDisciplina);
            if (BOTAO == null)
                throw new ExcecaoSIGAA("disciplinaAcessarBotaoMenu() página da turma virtual não carregou");
        }

        //Body
        FormBody bodyBotao = new FormBody.Builder()
                .add("formMenu", "formMenu")
                .add(BOTAO.j_id_jsp()[0][0], BOTAO.j_id_jsp()[0][1])
                .add("javax.faces.ViewState", javaxViewState(pgDisciplina))
                .add(BOTAO.j_id_jsp()[1][0], BOTAO.j_id_jsp()[1][1])
                .build();

        try {
            Response responseBotao = post("/sigaa/ava/index.jsf", bodyBotao);
            if (!respostaValida(responseBotao))
                throw new ExcecaoSIGAA("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

            Document docPgBotao = Jsoup.parse(responseBotao.body().string());
            if (conferirSessao && !usuarioLogado(docPgBotao))
                throw new ExcecaoSessaoExpirada("disciplinaAcessarBotaoMenu() sessão expirada");

            return docPgBotao;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaAcessarBotaoMenu() IOException");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Aula> disciplinaPegarAulas(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document pgDisciplina = acessarPaginaTurmaVirtual(d);
        return Parsers.paginaDisciplinaAulas(pgDisciplina, d);
    }

    public ArrayList<Usuario> disciplinaPegarParticipantes(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document docParticipantes = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_PARTICIPANTES);
        return Parsers.paginaDisciplinaParticipantes(docParticipantes, url_base);
    }

    public ArrayList<Nota> disciplinaPegarNotas(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document docNotas = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_NOTAS, false);
        return Parsers.paginaNotasDisciplinaNotas(docNotas, d);
    }

    public ArrayList<InfoArquivo> disciplinaPegarListaArquivos(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document docArquivos = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_ARQUIVOS);
        return Parsers.paginaDisciplinaArquivos(docArquivos, d);
    }

    public ArrayList<Avaliacao> disciplinaPegarAvaliacoes(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document docAvaliacoes = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_AVALIACOES);
        return Parsers.paginaDisciplinaAvaliacoes(docAvaliacoes, d);
    }

    public ArrayList<Tarefa> disciplinaPegarTarefas(Disciplina d) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (d == null) return new ArrayList<>();

        Document docTarefas = disciplinaAcessarBotaoMenu(d, idBotaoDocumento.DISC_VER_TAREFAS);
        return Parsers.paginaTarefasDisciplinaTarefas(docTarefas, url_base, d);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private Response disciplinaAbrirEnvioTarefa(Tarefa tarefa) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (tarefa == null || !tarefa.enviavel()) return null;

        try {
            if (tarefa.getDisciplina().id() != null) {
                // [1] Método rápido (usado pras disciplinas visíveis na página principal): página principal -> página tarefa

                final Response responsePgDiscente = get("/sigaa/portais/discente/discente.jsf");
                if (!respostaValida(responsePgDiscente))
                    throw new ExcecaoSIGAA("disciplinaAbrirEnvioTarefa() resposta inválida / SIGAA em manutenção");

                Document docPgDiscente = Jsoup.parse(responsePgDiscente.body().string());
                if (!usuarioLogado(docPgDiscente))
                    throw new ExcecaoSessaoExpirada("disciplinaAbrirEnvioTarefa() sessão expirada");

                FormBody bodyTarefa = new FormBody.Builder()
                        .add("formAtividades", "formAtividades")
                        .add("javax.faces.ViewState", javaxViewState(docPgDiscente))
                        .add("formAtividades:visualizarTarefaTurmaVirtual", "formAtividades:visualizarTarefaTurmaVirtual")
                        .add("id", tarefa.getId())
                        .add("idTurma", tarefa.getDisciplina().id())
                        .build();

                return post("/sigaa/portais/discente/discente.jsf#", bodyTarefa);
            } else {
                // [2] Método lento (usado pras disciplinas não visíveis na principal): página principal -> página turma virtual -> página tarefas -> página tarefas
                //TODO: Quando tiver oportunidade, conferir se isso realmente funciona
                Document docTarefas = disciplinaAcessarBotaoMenu(tarefa.getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);

                FormBody bodyTarefa = new FormBody.Builder()
                        .add(tarefa.postArgsEnviar()[0], tarefa.postArgsEnviar()[0])
                        .add("javax.faces.ViewState", javaxViewState(docTarefas))
                        .add(tarefa.postArgsEnviar()[1], tarefa.postArgsEnviar()[1])
                        .add("id", tarefa.getId())
                        .build();

                return post("/sigaa/portais/discente/discente.jsf#", bodyTarefa);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaAcessarBotaoMenu() IOException");
        }
    }

    //Obtem o form de uma tarefa, que é usado pra enviar a tarefa
    public FormTarefa disciplinaObterFormTarefa(Tarefa tarefa) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (tarefa == null || !tarefa.enviavel()) return null;

        Response respostaPgTarefa = disciplinaAbrirEnvioTarefa(tarefa);
        if (!respostaValida(respostaPgTarefa))
            throw new ExcecaoSIGAA("disciplinaObterFormTarefa() resposta inválida / SIGAA em manutenção");

        try {
            Document docTarefa = Jsoup.parse(respostaPgTarefa.body().string());
            if (!usuarioLogado(docTarefa))
                throw new ExcecaoSessaoExpirada("disciplinaObterFormTarefa() sessão expirada");
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
                throw new ExcecaoSIGAA("disciplinaObterFormTarefa() página da tarefa acessada não possui um form");
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
        } catch (IOException e) {
            throw new ExcecaoAPI("disciplinaObterFormTarefa() IOException ");
        }
    }

    //TODO: Testar. Não cheguei a testar essa função após a limpeza do código
    public boolean disciplinaEnviarTarefa(FormTarefa formTarefa) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (formTarefa.getTarefa() == null || !formTarefa.getTarefa().enviavel()) return false;

        Response responsePgTarefa = disciplinaAbrirEnvioTarefa(formTarefa.getTarefa());
        if (!respostaValida(responsePgTarefa))
            throw new ExcecaoSIGAA("disciplinaEnviarTarefa() resposta inválida / SIGAA em manutenção");

        try {
            Document docTarefa = Jsoup.parse(responsePgTarefa.body().string());
            if (!usuarioLogado(docTarefa))
                throw new ExcecaoSessaoExpirada("disciplinaEnviarTarefa() sessão expirada");

            ////Conferir se preenche a tarefa corretamente
            //Requer arquivo, mas não há arquivo
            if (formTarefa.arquivoObrigatorio() && (formTarefa.getArquivo() == null || formTarefa.getNomeArquivo() == null || formTarefa.getNomeArquivo() == ""))
                throw new ExcecaoSIGAA("disciplinaEnviarTarefa() formulário preenchido incorretamente");
            //Não aceita arquivo, mas há algum arquivo
            if (!formTarefa.aceitaArquivo() && (formTarefa.getArquivo() != null || formTarefa.getNomeArquivo() != null))
                throw new ExcecaoSIGAA("disciplinaEnviarTarefa() formulário preenchido incorretamente");
            //TODO: Não aceita comentário
            if (!formTarefa.aceitaComentarios())
                throw new ExcecaoSIGAA("disciplinaEnviarTarefa() formulário não aceita comentário (?)");
            //Requer resposta, mas não tem resposta
            if (formTarefa.respostaObrigatoria() && (formTarefa.getResposta() == null || formTarefa.getResposta() == ""))
                throw new ExcecaoSIGAA("disciplinaEnviarTarefa() formulário preenchido incorretamente");
            //Não aceita resposta, mas tem resposta
            if (!formTarefa.aceitaResposta() && (formTarefa.getResposta() != "" && formTarefa.getResposta() != null))
                throw new ExcecaoSIGAA("disciplinaEnviarTarefa() formulário preenchido incorretamente");

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
            //TODO: preciso .addInterceptor() pra retentar as que falham, tipo disciplina e conexao lenta
            Response responseEnvioTarefa = client.newCall(requestEnvioTarefa).execute();
            if (!respostaValida(responseEnvioTarefa))
                throw new ExcecaoSIGAA("disciplinaAcessarBotaoMenu() resposta inválida / SIGAA em manutenção");

            //Confirmação de envio
            Document docEnvio = Jsoup.parse(responseEnvioTarefa.body().string());
            if (!usuarioLogado(docEnvio))
                throw new ExcecaoSessaoExpirada("disciplinaAcessarBotaoMenu() sessão expirada");

            if (docEnvio.getElementsByClass("info").size() > 0 && docEnvio.getElementsByClass("info").first().children().size() > 0 && docEnvio.getElementsByClass("info").first().children().first().text().equals("Operação realizada com sucesso!")) {
                System.out.println(logMSG + "disciplinaEnviarTarefa() Identificou confirmação de envio. Tarefa enviada com sucesso");
                return true;
            } else {
                System.out.println(logMSG + "disciplinaEnviarTarefa() Não identificou o pop-up de confirmação de envio");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaEnviarTarefa() IOException");
        }
    }

    public EnvioTarefa disciplinaPegarEnvioTarefa(Tarefa t) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        //Acessar página de tarefas
        Document docTarefas = disciplinaAcessarBotaoMenu(t.getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);
        if (!usuarioLogado(docTarefas))
            throw new ExcecaoSessaoExpirada("disciplinaPegarEnvioTarefa() sessão expirada");

        //Body para ver o envio
        FormBody body_envioTarefa = new FormBody.Builder()
                .add(t.postArgsVisualizar()[0], t.postArgsVisualizar()[0])
                .add("javax.faces.ViewState", javaxViewState(docTarefas))
                .add(t.postArgsVisualizar()[1], t.postArgsVisualizar()[1])
                .add("id", t.postArgsVisualizar()[2])
                .build();

        try {
            Response responseEnvioTarefa = post("/sigaa/ava/TarefaTurma/listar.jsf", body_envioTarefa);
            if (!respostaValida(responseEnvioTarefa))
                throw new ExcecaoSIGAA("disciplinaPegarEnvioTarefa() resposta inválida / SIGAA em manutenção");

            Document docEnvioTarefa = Jsoup.parse(responseEnvioTarefa.body().string());
            if (!usuarioLogado(docEnvioTarefa))
                throw new ExcecaoSessaoExpirada("disciplinaPegarEnvioTarefa() sessão expirada");

            return Parsers.paginaTarefaEnvioTarefa(docEnvioTarefa, t, url_base);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaPegarEnvioTarefa() IOException");
        }
    }

    //TODO  Transforma o AnexoTarefa em Tarefa. Isso aqui tá horrivel. Atualmente essa foi a solução mais fácil pra não precisar modificar o resto do código. A solução que envolve mudar o código provavelmente vai ser armazenando somente o id da tarefa e fazendo o processo a partir somente dele
    //      2: acho que eu poderia deixar essa mesmo caso a api guarde e ultilize da ultima pagina pra acelerar as coisas. aí não perderia muito tempo no final
    public Tarefa disciplinaPegarTarefa(AnexoTarefa tarefa) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        Document docTarefas = disciplinaAcessarBotaoMenu(tarefa.getAula().getDisciplina(), idBotaoDocumento.DISC_VER_TAREFAS);

        ArrayList<Tarefa> tarefas = Parsers.paginaTarefasDisciplinaTarefas(docTarefas, url_base, tarefa.getAula().getDisciplina());

        if(tarefas.size() > 0) {
            //Conferir pelo id
            for(Tarefa t : tarefas) {
                if(t.getId().equals(tarefa.getId())) return t;
            }

            //Conferir pelo titulo (quando a tarefa não é enviável). Há chance de não ser a mesma, mas a tarefa já não é enviável de qualquer maneira aqui
            for(Tarefa t : tarefas) {
                if(t.getTitulo().equals(tarefa.getTitulo())) return t;
            }
        }

        throw new ExcecaoSIGAA("disciplinaPegarTarefa() Tarefa inexistente");
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Arquivo disciplinaBaixarArquivo(InfoArquivo a) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (a == null) return null;

        Document docArquivos = disciplinaAcessarBotaoMenu(a.getDisciplina(), idBotaoDocumento.DISC_ARQUIVOS);

        FormBody bodyAq = new FormBody.Builder()
                .add("formAva", "formAva")
                .add("javax.faces.ViewState", javaxViewState(docArquivos))
                .add(a.getJ_id_jsp(), a.getJ_id_jsp())
                .add("id", a.getId())
                .build();

        try {
            Response responseArquivo = post("/sigaa/ava/ArquivoTurma/listar_discente.jsf", bodyAq);

            String nomeArquivo = responseArquivo.header("Content-Disposition").split("filename=\"")[1];
            nomeArquivo = nomeArquivo.substring(0, nomeArquivo.length()-1);
            byte[] bytes = IOUtils.toByteArray(responseArquivo.body().byteStream());

            return new Arquivo(nomeArquivo, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaBaixarArquivo() IOException");
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Funções de tarefa, arquivo, etc. pra anexos
    public Arquivo disciplinaBaixarArquivo(AnexoInfoArquivo a) throws ExcecaoSIGAA, ExcecaoAPI, ExcecaoSessaoExpirada {
        if (a == null) return null;

        Document pgDisciplina = acessarPaginaTurmaVirtual(a.getAula().getDisciplina());

        FormBody bodyAq = new FormBody.Builder()
                .add("formAva", "formAva")
                .add("formAva:idTopicoSelecionado", pgDisciplina.getElementById("formAva:idTopicoSelecionado").attr("value"))
                .add("javax.faces.ViewState", javaxViewState(pgDisciplina))
                .add(a.getJ_id_jsp(), a.getJ_id_jsp())
                .add("id", a.getId())
                .build();

        try {
            Response responseArquivo = post("/sigaa/ava/index.jsf", bodyAq);

            String nomeArquivo = responseArquivo.header("Content-Disposition").split("filename=\"")[1];
            nomeArquivo = nomeArquivo.substring(0, nomeArquivo.length()-1);
            byte[] bytes = IOUtils.toByteArray(responseArquivo.body().byteStream());

            return new Arquivo(nomeArquivo, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExcecaoAPI("disciplinaBaixarArquivo() IOException");
        }
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
                            System.out.println(logMSG + "inicializarUsuario() POST disciplina sem sucesso");
                            return null;
                        }
                    } else {
                        //todo: usuario sem disciplinas
                        //Usuario = new usuario(nome, campus, email, matricula, new botaoDocumento[] {meusDados});
                        System.out.println(logMSG + logado());
                        ArrayList<botaoDocumento> botoesUsuario = new ArrayList<botaoDocumento>();
                        botoesUsuario.add(meusDados);
                        usuarioSalvo = new usuario(nome, campus, email, matricula, botoesUsuario);
                        return usuarioSalvo;
                    }
                } else {
                   System.out.println(logMSG + "inicializarUsuario() POST meus dados sem sucesso");
                    return null;
                }
            } else {
                System.out.println(logMSG + "inicializarUsuario() GET main page sem sucesso");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(logMSG + "inicializarUsuario() " + e);
            return null;
        }
    }*/
}
