package com.stacked.sigaa_ifc;

import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
        if(JSESSIONID != null && dataLogin != 0 && sessionTimeout()) throw new IOException(JSESSIONID + " Session Timeout");
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
        if(JSESSIONID != null && dataLogin != 0 && sessionTimeout()) throw new IOException(JSESSIONID + " Session Timeout");
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
        if(r.isSuccessful()) {
            if(r.priorResponse() != null && r.priorResponse().isRedirect()) return !(r.priorResponse().headers().get("Location").contains("manutencao.html")); else return true;
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
        long t = 55*60*1000; //55 minutos para margem de erro
        return (System.currentTimeMillis()-dataLogin > t);
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
            if(respostaValida(r)) {
                JSESSIONID = r.header("Set-Cookie").replace("(", "").replace(")", "").split(";")[0].split("JSESSIONID=")[1];
                System.out.println(logMSG + "JSESSIONID = " + JSESSIONID);
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
        if(!novoJSESSIONID()) return null;

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
        try
        {
            Response R = post("/sigaa/logar.do", body_login);
            if(respostaValida(R)) {
                if(R.priorResponse() == null) {
                    //Nao redirecionado, nao logou.
                    System.out.println(logMSG + "logar() sem resposta. Usuário ou senha incorretos");
                    return null;
                } else {
                    //Redirecionado
                    String[] urlsLogado = {url_base + "/sigaa/verPortalDiscente.do", url_base + "/sigaa/portais/discente/discente.jsf"};

                    String urlRedirecionado = R.priorResponse().headers().get("Location").replace("https://", "").replace("http://", "");
                    if((urlRedirecionado.substring(urlRedirecionado.length() - 1)) == "/") urlRedirecionado = urlRedirecionado.substring(0, urlRedirecionado.length() - 1); //Remover / final

                    if (Arrays.asList(urlsLogado).contains(urlRedirecionado)) {

                        //Redirecionado para uma das paginas comuns pos-login
                        System.out.println(logMSG + "logar() sem problemas");
                        dataLogin = System.currentTimeMillis();

                        //Dados
                        Document d = Jsoup.parse(R.body().string());
                        usuarioSalvo = Parsers.mainPageDadosUsuario(d, url_base);
                        return usuarioSalvo;

                    } else if(urlRedirecionado.contains(url_base + "/sigaa/telaAvisoLogon.jsf")) {

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
                                    usuarioSalvo = Parsers.mainPageDadosUsuario(f, url_base);
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
                                    usuarioSalvo = Parsers.mainPageDadosUsuario(d, url_base);
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
    public Usuario usuarioSalvo() {
        return this.usuarioSalvo;
    }
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
        try {
            final Response G = get("/sigaa/portais/discente/discente.jsf");
            if(respostaValida(G)) {
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

    //Usado para pegar algum dos botoes no usuario salvo. Se nao encontrar nele, procura na pagina
    BotaoDocumento botao (idBotaoDocumento id, Document d) {
        if(usuarioSalvo.botao(id) != null) return usuarioSalvo.botao(id);

        ArrayList<BotaoDocumento> botoes = Parsers.paginaDisciplinaBotoes(d);
        for(BotaoDocumento b : botoes) {
            usuarioSalvo.adicionarBotao(b);
        }
        return usuarioSalvo.botao(id);
    }

    /*
    disciplinaPegarNotas(disciplina)
    Pega a página de notas de uma disciplina, parsa as notas, salva no objeto da disciplina e as retorna.
     */
    //TODO: Terminar esse. parser
    public ArrayList<Nota> disciplinaPegarNotas(Disciplina d) {
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if(respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                //Body pra pedir as notas
                BotaoDocumento VER_NOTAS = botao(idBotaoDocumento.VER_NOTAS, doc_p);
                if(VER_NOTAS == null) {
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
                if(respostaValida(N)) {
                    Document doc_notas = Jsoup.parse(N.body().string());
                    //System.out.println(doc_notas.wholeText());//////
                    return Parsers.paginaNotasDisciplinaNotas(doc_notas);
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
        try {
            Response P = disciplinaPaginaDisciplina(d);
            if(respostaValida(P)) {
                Document doc_p = Jsoup.parse(P.body().string());

                //Body pra pedir as tarefas
                BotaoDocumento VER_TAREFAS = botao(idBotaoDocumento.VER_TAREFAS, doc_p);
                if(VER_TAREFAS == null) {
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
                if(respostaValida(N)) {
                    Document doc_tarefas = Jsoup.parse(N.body().string());
                    return Parsers.paginaTarefasDisciplinaTarefas(doc_tarefas, url_base);
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
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
