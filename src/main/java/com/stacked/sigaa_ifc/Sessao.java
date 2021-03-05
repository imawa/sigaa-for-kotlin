package com.stacked.sigaa_ifc;

import android.net.Uri;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.FormBody;

public class Sessao {
    public static String logMSG = "Debug API: ";
    private String url_base;
    private OkHttpClient client;

    private String JSESSIONID = null;
    private Usuario usuarioSalvo = null;
    private long dataLogin = 0;

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
    renovarCliente()
    Caso o cliente dê algum problema, etc.
     */
    private void renovarCliente() {
        client = new OkHttpClient();
    }

    /*
    get(url)
    Usado pra dar os requests de GET.
    */
    private Response get(String caminho) throws IOException {
        if (JSESSIONID != null && dataLogin != 0 && sessionTimeout())
            throw new IOException(JSESSIONID + " Session Timeout");
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
        if (JSESSIONID != null && dataLogin != 0 && sessionTimeout())
            throw new IOException(JSESSIONID + " Session Timeout");
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
        } else return false;
    }

    /*
    logado()
    Confere a página inicial do SIGAA
    Retorna boolean indicando se esta logado
     */
    public boolean logado() {
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

    /*
    sessionTimeout()
    Eu to assumindo que a sessão do SIGAA deslogue automaticamente após 1 hora (contador do SIGAA). Essa função apenas confere se esse contador expirou pra sessão atual baseado na data de login
    Retorna true se expirou, false se não.
     */
    public boolean sessionTimeout() {
        long t = 55 * 60 * 1000; //55 minutos para margem de erro
        return (System.currentTimeMillis() - dataLogin > t);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    novoJSESSIONID()
    Obtém um novo JSESSIONID. Retorna se foi possível atualizar o JSESSIONID corretamente.
     */
    private boolean novoJSESSIONID() {
        JSESSIONID = null;
        try {
            Response r = get("/sigaa/verTelaLogin.do");
            if (respostaValida(r)) {
                JSESSIONID = r.header("Set-Cookie").replace("(", "").replace(")", "").split(";")[0].split("JSESSIONID=")[1];
                System.out.println(logMSG + "Obtido um JSESSIONID");
            } else {
                System.out.println(logMSG + "novoJSESSIONID() Resposta falhou");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(logMSG + "novoJSESSIONID() IOException");
            return false;
        }
        return true;
    }

    /*
    login(usuario, senha);
    Loga a sessão e um usuário não completo (com os dados disponíveis na página principal) se logar corretamente. Retorna null se acontecer algum erro
    */
    //TODO Tenho que limpar esse código. Principalmente pular mais de 1 aviso
    public Usuario login(final String usuario, final String senha) {
        //JSESSIONID
        if (!novoJSESSIONID()) return null;

        //Logar JSESSIONID
        //Body para logar
        final FormBody body_login = new FormBody.Builder()
                .add("dispatch", "logOn")
                .add("urlRedirect", "")
                .add("subsistemaRedirect", "")
                .add("acao", "")
                .add("acessibilidade", "")
                .add("user.login", usuario)
                .add("user.senha", senha)
                .build();

        System.out.println(logMSG + "login() Enviando POST login");
        try {
            Response R = post("/sigaa/logar.do", body_login);
            if (respostaValida(R)) {
                if (R.priorResponse() == null) {
                    //Nao redirecionado, nao logou.
                    System.out.println(logMSG + "logar() sem resposta. Usuário ou senha incorretos");
                    return null;
                } else {
                    //Redirecionado
                    String[] urlsLogado = {url_base + "/sigaa/verPortalDiscente.do", url_base + "/sigaa/portais/discente/discente.jsf"};

                    String urlRedirecionado = R.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
                    if ((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/")
                        urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final

                    if (Arrays.asList(urlsLogado).contains(urlRedirecionado)) {

                        //Redirecionado para uma das paginas comuns pos-login
                        System.out.println(logMSG + "logar() sem problemas");
                        dataLogin = System.currentTimeMillis();

                        //Dados
                        Document d = Jsoup.parse(R.body().string());
                        usuarioSalvo = Parsers.mainPageDadosUsuario(d, url_base, usuario);
                        return usuarioSalvo;

                    } else if (urlRedirecionado.contains(url_base + "/sigaa/telaAvisoLogon.jsf")) {

                        //Redirecionado para algum aviso
                        System.out.println(Sessao.logMSG + "Redirecionado para um aviso");

                        Document d = Jsoup.parse(R.body().string());
                        FormBody body_aviso = Parsers.paginaAvisoSkipBody(d);
                        try {
                            Response A = post("/sigaa/telaAvisoLogon.jsf", body_aviso);
                            if (respostaValida(A)) {
                                if (A.priorResponse() == null) {
                                    //Nao redirecionado, nao logou.
                                    System.out.println(logMSG + "Pular aviso sem resposta");
                                } else {
                                    //Redirecionado para a página principal
                                    Document f = Jsoup.parse(A.body().string());
                                    usuarioSalvo = Parsers.mainPageDadosUsuario(f, url_base, usuario);
                                    return usuarioSalvo;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;

                    } else {

                        //Redirecionado para outra pagina
                        System.out.println(logMSG + "logar() POST login redirecionado para a página errada? (" + urlRedirecionado + "). Conferindo logado()");
                        try {
                            Response r = get("/sigaa/portais/discente/discente.jsf");
                            if (respostaValida(r)) {
                                //Se redirecionou é porque nao ta logado
                                if (!(r.priorResponse() != null && r.priorResponse().isRedirect())) {
                                    //Logado
                                    System.out.println(logMSG + "logar() sem problemas");
                                    dataLogin = System.currentTimeMillis();

                                    //Dados
                                    Document d = Jsoup.parse(R.body().string());
                                    usuarioSalvo = Parsers.mainPageDadosUsuario(d, url_base, usuario);
                                    return usuarioSalvo;
                                } else return null;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.print(logMSG + "login() Falha ao conferir login");
                            return null;
                        }
                        return null;

                    }
                }
            } else {
                System.out.print(logMSG + "login() Falha POST login (sem sucesso)");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.print(logMSG + "login() Falha POST login");
            return null;
        }
    }

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

    /*
    Retorna o usuario com os dados salvos através dessa sessão
     */
    /*public Usuario usuarioSalvo() {
        return this.usuarioSalvo;
    }*/
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    disciplinaPaginaDisciplina(disciplina)
    Solicita a página principal de uma disciplina. É necessário fazer isso toda vez que for fazer uma operação em uma disciplina não acessada previamente
    Retorna a resposta da página principal da disciplina.

    A diferença entre esse e o disciplinaFetchPaginaPrincipal(disciplina) é que o segundo parsa a página principal em itens, enquanto esse apenas solicita ela para que comandos como o disciplinaFetchNotas(disciplina) possam funcionar
     */
    private Response disciplinaPaginaDisciplina(Disciplina d) {
        if (d == null) return null;
        try {
            final Response G = get("/sigaa/portais/discente/discente.jsf");
            if (respostaValida(G)) {
                Document D = Jsoup.parse(G.body().string());
                FormBody body_disciplina = new FormBody.Builder()
                        .add(d.postArgs()[0], d.postArgs()[0])
                        .add("javax.faces.ViewState", javaxViewState(D))
                        .add(d.postArgs()[1], d.postArgs()[1])
                        .add("frontEndIdTurma", d.postArgs()[2])
                        .build();

                return post("/sigaa/portais/discente/discente.jsf#", body_disciplina);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(logMSG + "solicitarPaginaDisciplina() nao teve sucesso");
            return null;
        }
    }

    private Response disciplinaAbrirEnvioTarefa(Tarefa tarefa) {
        if (tarefa == null || !tarefa.enviavel()) return null;
        try {
            final Response G = get("/sigaa/portais/discente/discente.jsf");
            if (respostaValida(G)) {
                Document D = Jsoup.parse(G.body().string());
                FormBody body_tarefa = new FormBody.Builder()
                        .add("formAtividades", "formAtividades")
                        .add("javax.faces.ViewState", javaxViewState(D))
                        .add("formAtividades:visualizarTarefaTurmaVirtual", "formAtividades:visualizarTarefaTurmaVirtual")
                        .add("id", tarefa.getId())
                        .add("idTurma", tarefa.getDisciplina().id()) //TODO: arrumar pras que nao tem id
                        .build();

                return post("/sigaa/portais/discente/discente.jsf#", body_tarefa);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(logMSG + "disciplinaAbrirEnvioTarefa() nao teve sucesso");
            return null;
        }
    }

    //Usado para pegar algum dos botoes no usuario salvo. Se nao encontrar nele, procura na pagina
    BotaoDocumento botao(idBotaoDocumento id, Document d) {
        if (usuarioSalvo.botao(id) != null) return usuarioSalvo.botao(id);

        ArrayList<BotaoDocumento> botoes = Parsers.paginaDisciplinaBotoes(d);
        for (BotaoDocumento b : botoes) {
            usuarioSalvo.adicionarBotao(b);
        }
        return usuarioSalvo.botao(id);
    }

    /*
    disciplinaPegarNotas(disciplina)
    Pega a página de notas de uma disciplina, parsa as notas, salva no objeto da disciplina e as retorna.
     */
    //TODO: Terminar esse. parser
    //TODO 2: Dá pra juntar muita coisa dessas funções aqui abaixo, pois são bem iguais
    public ArrayList<Nota> disciplinaPegarNotas(Disciplina d) {
        if (d == null) return new ArrayList<>();
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if (respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                //Body pra pedir as notas
                BotaoDocumento VER_NOTAS = botao(idBotaoDocumento.DISC_VER_NOTAS, doc_p);
                if (VER_NOTAS == null) {
                    //TODO: nao encontrou botao (nao salvo e pagina nao carregou)
                    return new ArrayList<Nota>();
                }

                FormBody body_notas = new FormBody.Builder()
                        .add("formMenu", "formMenu")
                        .add(VER_NOTAS.j_id_jsp()[0][0], VER_NOTAS.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(doc_p))
                        .add(VER_NOTAS.j_id_jsp()[1][0], VER_NOTAS.j_id_jsp()[1][1])
                        .build();

                Response N = post("/sigaa/ava/index.jsf", body_notas);
                if (respostaValida(N)) {
                    Document doc_notas = Jsoup.parse(N.body().string());
                    //System.out.println(doc_notas.wholeText());//////
                    return Parsers.paginaNotasDisciplinaNotas(doc_notas, d);
                } else {
                    System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página de notas");
                    return null;
                }
            } else {
                System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página principal da disciplina");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //return null;
    }

    public ArrayList<Tarefa> disciplinaPegarTarefas(Disciplina d) {
        if (d == null) return new ArrayList<>();
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if (respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                //Body pra pedir as tarefas
                BotaoDocumento VER_TAREFAS = botao(idBotaoDocumento.DISC_VER_TAREFAS, doc_p);
                if (VER_TAREFAS == null) {
                    //TODO: nao encontrou botao (nao salvo e pagina nao carregou)
                    return new ArrayList<Tarefa>();
                }

                FormBody body_notas = new FormBody.Builder()
                        .add("formMenu", "formMenu")
                        .add(VER_TAREFAS.j_id_jsp()[0][0], VER_TAREFAS.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(doc_p))
                        .add(VER_TAREFAS.j_id_jsp()[1][0], VER_TAREFAS.j_id_jsp()[1][1])
                        .build();

                Response N = post("/sigaa/ava/index.jsf", body_notas);
                if (respostaValida(N)) {
                    Document doc_tarefas = Jsoup.parse(N.body().string());
                    return Parsers.paginaTarefasDisciplinaTarefas(doc_tarefas, url_base, d);
                } else {
                    System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página de tarefas");
                    return null;
                }
            } else {
                System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página principal da disciplina");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Usuario> disciplinaPegarParticipantes(Disciplina d) {
        if (d == null) return new ArrayList<>();
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if (respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                //Body pra pedir as tarefas
                BotaoDocumento VER_PARTICIPANTES = botao(idBotaoDocumento.DISC_PARTICIPANTES, doc_p);
                if (VER_PARTICIPANTES == null) {
                    //TODO: nao encontrou botao (nao salvo e pagina nao carregou)
                    return new ArrayList<Usuario>();
                }

                FormBody body_participantes = new FormBody.Builder()
                        .add("formMenu", "formMenu")
                        .add(VER_PARTICIPANTES.j_id_jsp()[0][0], VER_PARTICIPANTES.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(doc_p))
                        .add(VER_PARTICIPANTES.j_id_jsp()[1][0], VER_PARTICIPANTES.j_id_jsp()[1][1])
                        .build();

                Response N = post("/sigaa/ava/index.jsf", body_participantes);
                if (respostaValida(N)) {
                    Document doc_participantes = Jsoup.parse(N.body().string());
                    //System.out.println(doc_participantes.wholeText());
                    return Parsers.paginaDisciplinaParticipantes(doc_participantes, url_base);
                } else {
                    System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página de participantes");
                    return null;
                }
            } else {
                System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página principal da disciplina");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Avaliacao> disciplinaPegarAvaliacoes(Disciplina d) {
        if (d == null) return new ArrayList<>();
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if (respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                // System.out.println(logMSG + doc_p.getElementById("conteudo").className());
                //   for(Element e : doc_p.getElementsByClass("titulo")) {
                //      System.out.println(logMSG + e.text());
                //   }
                //   System.out.println(doc_p.wholeText());

                //Body pra pedir as avaliacoes
                BotaoDocumento VER_AVALIACOES = botao(idBotaoDocumento.DISC_VER_AVALIACOES, doc_p);
                if (VER_AVALIACOES == null) {
                    //TODO: nao encontrou botao (nao salvo e pagina nao carregou)
                    return new ArrayList<Avaliacao>();
                }

                FormBody body_avaliacoes = new FormBody.Builder()
                        .add("formMenu", "formMenu")
                        .add(VER_AVALIACOES.j_id_jsp()[0][0], VER_AVALIACOES.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(doc_p))
                        .add(VER_AVALIACOES.j_id_jsp()[1][0], VER_AVALIACOES.j_id_jsp()[1][1])
                        .build();

                Response N = post("/sigaa/ava/index.jsf", body_avaliacoes);
                if (respostaValida(N)) {
                    Document doc_avaliacoes = Jsoup.parse(N.body().string());
                    //System.out.println(doc_avaliacoes.wholeText());
                    return Parsers.paginaDisciplinaAvaliacoes(doc_avaliacoes, d);
                } else {
                    System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página de avaliações");
                    return null;
                }
            } else {
                System.out.println(logMSG + "disciplinaFetchNotas() Erro solicitar página principal da disciplina");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Obtem o form de uma tarefa, que é usado pra enviar a tarefa
    //TODO: Testar se dá pra enviar mesmo assim. Não tive oportunidade para testar ainda
    public FormTarefa disciplinaObterFormTarefa(Tarefa tarefa) {
        if (tarefa == null || !tarefa.enviavel()) return null;

        try {
            Response respostaPgTarefa = disciplinaAbrirEnvioTarefa(tarefa);
            if (respostaValida(respostaPgTarefa)) {
                Document docTarefa = Jsoup.parse(respostaPgTarefa.body().string());
                ////////////////////////////////////////////////////////
                //Conferir o que a tarefa requer
                boolean input_arquivo = false, input_comentarios = false, input_arquivo_obrigatorio = false;
                if(docTarefa.getElementsByClass("form").size() > 0 && docTarefa.getElementsByClass("form").first().getElementsByTag("label").size() > 0) {
                    Element eForm = docTarefa.getElementsByClass("form").first();
                    if(eForm.getElementsByAttributeValueContaining("name", "idComentarios").size() == 1) input_arquivo = true;
                    if(eForm.getElementsByAttributeValueContaining("name", "idArquivo").size() == 1) input_comentarios = true;
                    // Algumas tarefas requerem o campo de arquivo
                    if(input_arquivo) {
                        if(eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").size() > 0) {
                            input_arquivo_obrigatorio = (eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").first().getElementsByAttributeValueContaining("class", "required").size() == 1);
                        }
                    }
                    //TODO: Conferir campo de resposta e sua obrigatoriedade quando haver algum
                    //TODO: Conferir se campo de resposta não é obrigatório quando haver algum
                } else {
                    System.out.println(logMSG + "disciplinaObterFormTarefa() Página acessada não possui um form");
                    return null;
                }
                ////////////////////////////////////////////////////////
                //Pegar os inputs do form escondido da pagina
                String j_id_jsp = "";
                ArrayList<String> infoEscondidaForm = new ArrayList<>();
                for (Element i : docTarefa.getElementsByClass("responderTarefa").get(0).getElementsByTag("input")) {
                    if (i.attr("type").equals("hidden")) {
                        //j_id_jsp
                        if(j_id_jsp.equals("")) {
                            j_id_jsp = i.attr("name").split(":")[0];
                        }
                        //informacoes
                        infoEscondidaForm.add(i.attr("name"));
                        infoEscondidaForm.add(i.attr("value"));
                    }
                }
                ////////////////////////////////////////////////////////
                FormTarefa form_tarefa = new FormTarefa(tarefa, j_id_jsp, infoEscondidaForm, input_comentarios, input_arquivo, input_arquivo_obrigatorio, false, false);//TODO: resposta
                return form_tarefa;
                } else {
                System.out.println(logMSG + "disciplinaObterFormTarefa() Erro solicitar página tarefa");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(logMSG + "disciplinaObterFormTarefa() Ocorreu algum erro");
        }
        return null;
    }

    //TODO 0: Trocar pelo form
    public boolean disciplinaEnviarTarefa(FormTarefa formTarefa) {
        if (formTarefa.getTarefa() == null || !formTarefa.getTarefa().enviavel()) return false;

        try {
            Response T = disciplinaAbrirEnvioTarefa(formTarefa.getTarefa());
            if (respostaValida(T)) {
                Document doc_tarefa = Jsoup.parse(T.body().string());
                ////////////////////////////////////////////////////////
                //Conferir se a tarefa aceita o que foi inserido
                boolean input_arquivo = false, input_comentarios = false, input_arquivo_obrigatorio = false;

                if(doc_tarefa.getElementsByClass("form").size() > 0 && doc_tarefa.getElementsByClass("form").first().getElementsByTag("label").size() > 0) {
                    Element eForm = doc_tarefa.getElementsByClass("form").first();
                    if(eForm.getElementsByAttributeValueContaining("name", "idComentarios").size() == 1) input_arquivo = true;
                    if(eForm.getElementsByAttributeValueContaining("name", "idArquivo").size() == 1) input_comentarios = true;

                    // Algumas tarefas requerem o campo de arquivo
                    if(input_arquivo) {
                        if(eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").size() > 0) {
                            input_arquivo_obrigatorio = (eForm.getElementsByAttributeValueContaining("name", "idArquivo").first().parent().getElementsByTag("label").first().getElementsByAttributeValueContaining("class", "required").size() == 1);
                        }
                    }

                    //TODO: Conferir campo de resposta e sua obrigatoriedade quando haver algum
                    //TODO: Conferir se campo de resposta não é obrigatório quando haver algum
                } else {
                    System.out.println(logMSG + "disciplinaEnviarTarefa() Página acessada não possui um form");
                    return false;
                }

                //Requer arquivo, mas não há arquivo
                if(input_arquivo_obrigatorio && (formTarefa.getArquivo() == null || formTarefa.getNomeArquivo() == null || formTarefa.getNomeArquivo() == "")) {
                    System.out.println(logMSG + "disciplinaEnviarTarefa() Tarefa requer um arquivo, mas não foi inserido um arquivo");
                    return false;
                }
                //Não aceita arquivo, mas há algum arquivo
                if(!input_arquivo && (formTarefa.getArquivo() != null || formTarefa.getNomeArquivo() != null)) {
                    System.out.println(logMSG + "disciplinaEnviarTarefa() Tarefa não aceita arquivo, mas foi inserido um arquivo");
                    return false;
                }
                //Não aceita comentário
                if(!input_comentarios) {
                    System.out.println(logMSG + "disciplinaEnviarTarefa() Tarefa não aceita envio de comentários. Eu não sabia que isso era possível...");
                    return false; //Tarefa não aceita algum dos dois
                }
                //TODO: Aceita resposta, sem resposta & Com resposta, não aceita resposta
                ////////////////////////////////////////////////////////
                //Pegar os inputs do form invisível da pagina
                /*
                String j_id_jsp = "";
                ArrayList<String> form = new ArrayList<>();
                for (Element i : doc_tarefa.getElementsByClass("responderTarefa").get(0).getElementsByTag("input")) {
                    if (i.attr("type").equals("hidden")) {
                        if(j_id_jsp.equals("")) {
                            j_id_jsp = i.attr("name").split(":")[0];
                        }

                        System.out.println(logMSG + "disciplinaEnviarTarefa() " + i.attr("name") + " " + i.attr("value"));

                        form.add(i.attr("name"));
                        form.add(i.attr("value"));
                    }
                }*/

                // o sig envia um multi part form body no envio de tarefa
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
                        .addFormDataPart("javax.faces.ViewState", javaxViewState(doc_tarefa))
                        .build();
                //https://sig.ifc.edu.br/sigaa/ava/TarefaTurma/enviarTarefa.jsf

                //POSTAR O BODY
                if (JSESSIONID != null && dataLogin != 0 && sessionTimeout())
                    throw new IOException(JSESSIONID + " Session Timeout");
                Request request = new Request.Builder()
                        .url("https://" + url_base + "/sigaa/ava/TarefaTurma/enviarTarefa.jsf")
                        .header("Content-Type", "multipart/form-data")
                        .header("Cookie", "JSESSIONID=" + JSESSIONID)
                        .post(body_envioTarefa)
                        .build();

                //TODO: preciso .addInterceptor() pra retentar as que falham, tipo disciplina e conexao lenta
                Response N = client.newCall(request).execute();
                //System.out.println(logMSG + "d0");
                if(respostaValida(N)) {
                    Document docEnvio = Jsoup.parse(N.body().string());

                    System.out.println(docEnvio.wholeText());

                    //System.out.println(logMSG + "d1");
                    if(docEnvio.getElementsByClass("info").size() > 0 && docEnvio.getElementsByClass("info").first().children().size() > 0) {
                        if(docEnvio.getElementsByClass("info").first().children().first().text().equals("Operação realizada com sucesso!")) {
                            System.out.println(logMSG + "disciplinaEnviarTarefa() Identificou confirmação de envio. Tarefa enviada com sucesso");
                            //System.out.println(logMSG + "d2");
                            return true;
                        } else {
                            System.out.println(logMSG + "disciplinaEnviarTarefa() Não identificou confirmação de envio. Algum erro ocorreu");
                            //System.out.println(logMSG + "d3");
                            return false;
                        }
                    } else {
                        System.out.println(logMSG + "disciplinaEnviarTarefa() Não identificou o pop-up de confirmação de envio");
                        //System.out.println(logMSG + "d4");
                        return false;
                    }
                }
                //System.out.println(logMSG + "d5");
            } else {
                System.out.println(logMSG + "disciplinaEnviarTarefa() Erro solicitar página tarefa");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
         //   System.out.println(logMSG + "d6");
            return false;
        }
     //   System.out.println(logMSG + "d7");
        return false;
    }

    public EnvioTarefa disciplinaPegarEnvioTarefa(Tarefa t) {
        if (t == null) return null;

        try {
            Response responsePgDisciplina = disciplinaPaginaDisciplina(t.getDisciplina());
            if (respostaValida(responsePgDisciplina)) {
                Document docPgDisciplina = Jsoup.parse(responsePgDisciplina.body().string());

                //Body pra pedir as tarefas
                BotaoDocumento VER_TAREFAS = botao(idBotaoDocumento.DISC_VER_TAREFAS, docPgDisciplina);
                if (VER_TAREFAS == null) {
                    //TODO: nao encontrou botao (nao salvo e pagina nao carregou)
                    return null;
                }

                FormBody body_tarefas = new FormBody.Builder()
                        .add("formMenu", "formMenu")
                        .add(VER_TAREFAS.j_id_jsp()[0][0], VER_TAREFAS.j_id_jsp()[0][1])
                        .add("javax.faces.ViewState", javaxViewState(docPgDisciplina))
                        .add(VER_TAREFAS.j_id_jsp()[1][0], VER_TAREFAS.j_id_jsp()[1][1])
                        .build();

                Response responsePgTarefas = post("/sigaa/ava/index.jsf", body_tarefas);
                if (respostaValida(responsePgTarefas)) {
                    Document docTarefas = Jsoup.parse(responsePgTarefas.body().string());

                    FormBody body_envioTarefa = new FormBody.Builder()
                            .add(t.postArgsVisualizar()[0], t.postArgsVisualizar()[0])
                            .add("javax.faces.ViewState", javaxViewState(docTarefas))
                            .add(t.postArgsVisualizar()[1], t.postArgsVisualizar()[1])
                            .add("id", t.postArgsVisualizar()[2])
                            .build();

                    Response responseEnvioTarefa = post("/sigaa/ava/TarefaTurma/listar.jsf", body_envioTarefa);
                    if(respostaValida(responseEnvioTarefa)) {
                        Document docEnvioTarefa = Jsoup.parse(responseEnvioTarefa.body().string());
                        return Parsers.paginaTarefaEnvioTarefa(docEnvioTarefa, t, url_base);
                    } else {
                        System.out.println(logMSG + "disciplinaPegarEnvioTarefa() Erro solicitar página do envio");
                    }
                } else {
                    System.out.println(logMSG + "disciplinaPegarEnvioTarefa() Erro solicitar página de tarefas");
                }
            } else {
                System.out.println(logMSG + "disciplinaPegarEnvioTarefa() Erro solicitar página principal da disciplina");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
