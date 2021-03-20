package com.stacked.sigaa_ifc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.FormBody;

public class Parsers {
    //**0** ETC
    static protected FormBody paginaAvisoSkipBody(Document d) {
        Element btContinuar = null;
        for(Element e : d.getElementsByTag("input")) {
            if(e.attr("value").equals("Continuar >>")) {
                btContinuar = e;
            }
        }

        if(btContinuar != null) {
            final FormBody body_aviso = new FormBody.Builder()
                    .add(btContinuar.attr("name").split(":")[0], btContinuar.attr("name").split(":")[0])
                    .add(btContinuar.attr("name"), "Continuar >>")
                    .add("javax.faces.ViewState", Sessao.javaxViewState(d))
                    .build();
            return body_aviso;
        } else {
            System.out.println(Sessao.logMSG + "botão para pular o aviso não encontrado");
        }
        return null;
    }

    //**1** MAIN PAGE
    static protected ArrayList<Disciplina> mainPageDisciplinas(Document d) {
        ArrayList<Disciplina> disciplinas = new ArrayList<Disciplina>();

        int n = 0;
        for(Element e : d.body().getElementsByClass("descricao")) {
            Element _e = e.child(0).child(1);
            String periodo = d.getElementsByClass("periodo-atual").get(0).child(0).text(); //TODO: Isso funciona?

            Disciplina _d = new Disciplina(false, periodo, _e.html(), _e.outerHtml().split("'")[3], _e.outerHtml().split("'")[5], _e.outerHtml().split("'")[11]);
            String _id = e.parent().nextElementSibling().child(0).id().replace("linha_", "");
            _d.definirId(_id);

            disciplinas.add(_d);
        }

        return disciplinas;
    }

    static protected Usuario mainPageDadosUsuario(Document d, String url_base, String login) {
        final ArrayList<Disciplina> disciplinasAtuais = Parsers.mainPageDisciplinas(d);

        final String nome = d.body().getElementsByClass("nome").get(0).text();
        final String campus = d.body().getElementsByClass("unidade").get(0).text();
        final int matricula = Integer.parseInt(d.body().getElementById("perfil-docente").child(4).child(1).child(0).child(0).child(1).text());
        final String urlAvatar = ((!url_base.startsWith("http://") && !url_base.startsWith("https://")) ? "http://" : "") + url_base + d.body().getElementsByClass("foto").get(0).child(0).attr("src");

        final String j_id_jsp_meusDados = d.body().getElementsByAttributeValueContaining("id", "meusDadosPessoais").get(0).attr("id").split(":")[0];
        final BotaoDocumento meusDados = new BotaoDocumento(idBotaoDocumento.MAIN_MEUS_DADOS, new String[][]{{j_id_jsp_meusDados, j_id_jsp_meusDados}, {j_id_jsp_meusDados + ":meusDadosPessoais", j_id_jsp_meusDados + ":meusDadosPessoais"}});

        Usuario u = new Usuario(tipoUsuario.DISCENTE, nome, login, campus, matricula, disciplinasAtuais, urlAvatar);
        u.adicionarBotao(meusDados);
        return u;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //**2** DISCIPLINAS
    //Pagina principal
    static protected ArrayList<BotaoDocumento> paginaDisciplinaBotoes(Document d) {
        ArrayList<BotaoDocumento> botoesDisciplina = new ArrayList<BotaoDocumento>();

        Elements l = d.body().getElementsByClass("itemMenu");
        for (int i = 0; i < l.size(); i++) {
            //Percorrer todos os botoes encontrados na pagina da disciplina
            switch (l.get(i).text()) {
                //TODO: Parsar outros valores aqui (participantes, foruns, noticias, frequencia, avaliaccoes, tarefas, questionarios)
                //TODO 2: da pra  fazer em uma so funcao pra todos e so conferir o nome. Arrumar isso
                case "Ver Notas":
                    String[] v = {"", ""};
                    for (String a : l.get(i).parent().outerHtml().split("'")) {
                        if (a.startsWith("formMenu:j_id_jsp_")) {
                            if (v[0] == "") v[0] = a;
                            else v[1] = a;
                        }
                    }
                    String[][] vl = new String[][]{{l.get(i).parent().parent().parent().parent().parent().parent().parent().parent().id(), l.get(i).parent().parent().parent().parent().parent().parent().parent().id()}, v};
                    botoesDisciplina.add(new BotaoDocumento(idBotaoDocumento.DISC_VER_NOTAS, vl));
                    break;

                case "Tarefas":
                    String[] t = {"", ""};
                    for (String a : l.get(i).parent().outerHtml().split("'")) {
                        if (a.startsWith("formMenu:j_id_jsp_")) {
                            if (t[0] == "") t[0] = a;
                            else t[1] = a;
                        }
                    }
                    String[][] tl = new String[][]{{l.get(i).parent().parent().parent().parent().parent().parent().parent().parent().id(), l.get(i).parent().parent().parent().parent().parent().parent().parent().id()}, t};
                    botoesDisciplina.add(new BotaoDocumento(idBotaoDocumento.DISC_VER_TAREFAS, tl));
                    break;

                case "Participantes":
                    String[] p = {"", ""};
                    for (String a : l.get(i).parent().outerHtml().split("'")) {
                        if (a.startsWith("formMenu:j_id_jsp_")) {
                            if (p[0] == "") p[0] = a;
                            else p[1] = a;
                        }
                    }
                    String[][] pl = new String[][]{{l.get(i).parent().parent().parent().parent().parent().parent().parent().parent().id(), l.get(i).parent().parent().parent().parent().parent().parent().parent().id()}, p};
                    botoesDisciplina.add(new BotaoDocumento(idBotaoDocumento.DISC_PARTICIPANTES, pl));
                    //System.out.println(Sessao.logMSG + pl[0][0] + " " + pl[0][1] + " // " + pl[1][0] + " " + pl[1][1]);
                    break;

                case "Avaliações":
                    String[] av = {"", ""};
                    for (String a : l.get(i).parent().outerHtml().split("'")) {
                        if (a.startsWith("formMenu:j_id_jsp_")) {
                            if (av[0] == "") av[0] = a;
                            else av[1] = a;
                        }
                    }
                    String[][] avl = new String[][]{{l.get(i).parent().parent().parent().parent().parent().parent().parent().parent().id(), l.get(i).parent().parent().parent().parent().parent().parent().parent().id()}, av};
                    botoesDisciplina.add(new BotaoDocumento(idBotaoDocumento.DISC_VER_AVALIACOES, avl));
                    break;
            }
        }
        return botoesDisciplina;
    }

    //Participantes
    static protected ArrayList<Usuario> paginaDisciplinaParticipantes(Document d, String url_base) {
        ArrayList<Usuario> usuarios = new ArrayList<>();
        //fieldset em cima indicando docente ou discente (0 docente) (1 ddisente)
        //document.getElementsByClassName("participantes")
        /*
        dentro do primeiro do participantes ta os usuarios

        tem um tr por linha

      valign=top > dados usuario
      em cima do valign top = imagem do usuario
         */
        Elements listaParticipantes = d.getElementsByClass("participantes");

        for(int listaP = 0; listaP < listaParticipantes.size(); listaP++) {
            tipoUsuario tipo = (listaParticipantes.get(listaP).previousElementSibling().child(0).text().contains("Docente")) ? tipoUsuario.DOCENTE : tipoUsuario.DISCENTE;

            Element listaUsuarios = listaParticipantes.get(listaP).getElementsByTag("tbody").get(0);
            //Linhas (1-2 usuarios)
            for(Element linha : listaUsuarios.getElementsByTag("tr")) {
                //System.out.println(Sessao.logMSG + linha);
                //System.out.println(Sessao.logMSG + listaUsuarios.getElementsByTag("tr").size());

                for(Element elementosUsuariosLinha : linha.getAllElements()) {
                    //valign="top" -> elemento com informações do usuário
                    //em cima do valign="top" -> avatar
                    //em baixo do valign="top" -> botão de mensagem
                    if(elementosUsuariosLinha.attr("valign").equals("top")) {
                        Element eAvatar = elementosUsuariosLinha.previousElementSibling();
                        Element eInformacoes = elementosUsuariosLinha;

                        String url_avatar = ((!url_base.startsWith("http://") && !url_base.startsWith("https://")) ? "http://" : "") + url_base + eAvatar.getElementsByTag("img").get(0).attr("src");
                        String nome = eInformacoes.getElementsByTag("strong").text();
                        String usuario = "", email = "";
                        Integer matricula = 0;
                        for(Element e : eInformacoes.getElementsByTag("em")) {
                            if(e.previousSibling().outerHtml().contains("Usuário")) {
                                usuario = e.text();
                            } else if(e.previousSibling().outerHtml().toLowerCase().contains("e-mail")) { //Docente é E-[M]ail, o de discente é E-[m]ail. SIGAA, oi????
                                email = e.text();
                            } else if(e.previousSibling().outerHtml().contains("Matrícula")) {
                                matricula = Integer.parseInt(e.text());
                            }
                        }
                        //System.out.println(Sessao.logMSG + tipo +" " + nome + " " + usuario + " " + email + " " + matricula);
                        Usuario _u = new Usuario(tipo, nome, usuario, url_avatar, email);
                        if(matricula != 0) _u.definirMatricula(matricula); //Só discentes possuem matrícula aqui
                        usuarios.add(_u);
                    }
                }
            }
        }
        return usuarios;
    }

    //Pagina notas
    //todo: arrumar isso aqui e pegar pela linha e nao classe linha par ou impar
    static protected ArrayList<Nota> paginaNotasDisciplinaNotas(Document d, Disciplina disc) {
        ArrayList<Nota> notas = new ArrayList<Nota>();

        if(d.body().getElementById("trAval") == null || (d.body().getElementsByClass("linhaPar").size() == 0 && d.body().getElementsByClass("linhaImpar").size() == 0)) return notas; //Notas vazias (provavelmente nem foi pra página certa)

      //  System.out.println("1");
        final Element linhaInfoAval = d.body().getElementById("trAval");
        final Element linhaNotaAval = (d.body().getElementsByClass("linhaPar").size() > 0) ? d.body().getElementsByClass("linhaPar").get(0) : d.body().getElementsByClass("linhaImpar").get(0);
        final Element linhaInfoPeriodo = linhaInfoAval.previousElementSibling();

        int index_periodo = 0, index_nota = 0; //pepriodo -> Trimestre, bimestre, etc... Esse período separa "Trimestre 1" de "Trimestre 1 - Reavaliação"
        //OBS: Talvez ter conferido por cada numero no linhaNotaAval teria sido melhor, mas acho que o próprio software pode calcular as médias parciais.... enfim, se for provado necessário eu corrijo isso
        //TODO: isso ta organizado ruim demais e eu me envergonho disso, pessoa que esta vendo meu codigo espaguete. tenho q dar uma ajeitada depois. o importante é que ta funcionando normalmente
       // System.out.println("2");
        for(Element e : linhaInfoAval.getAllElements()) {
            String periodo = "", abrev = "", descricao = "";
            float nota = 0, notaMax = 0, peso = 0;
            //  System.out.println(index_nota + " " + e.id());///
            if(e.id().startsWith("aval_")) {
               // System.out.println("3");
                //Nota de algum trabalho, avaliação...
                String id = e.id().split("_")[1];
                //System.out.println("Debug API" + id);

                ///INFORMAÇÕES DA NOTA
                //  System.out.println(index_nota);////
                Element _e = e;
                for (int i = 0; i < 3; i++) {
                    Element x = _e.nextElementSibling();
                    if (x.id().contains(id)) {
                    //    System.out.println("5");
                        if (x.id().startsWith("abrevAval_")) {
                         //   System.out.println("4");
                            //Abreviacao
                            //   System.out.println("abrev");
                            abrev = (x.val() != null) ? x.val() : "";
                        } else if (x.id().startsWith("denAval_")) {
                         //   System.out.println("6");
                            //Descricao
                            //      System.out.println("desc");
                            descricao = (x.val() != null) ? x.val() : "";
                            //   System.out.println(descricao);
                        } else if (x.id().startsWith("notaAval_")) {
                     //       System.out.println("7");
                            //    System.out.println(x.val() + (x.val() != ""));
                            //Nota maxima
                            notaMax = (x.val() != "") ? Float.parseFloat(x.val()) : -1 ;
                        } else if (x.id().startsWith("pesoAval_")) {
                       //     System.out.println("8");
                            //         System.out.println("peso");
                            //Peso
                            peso = (x.val() != "") ? Float.parseFloat(x.val()) : -1;
                        }
                    }
               //     System.out.println("9");
                    _e = x;
                }

                //NOTA EM SI
                //   System.out.println(index_nota);
                //    System.out.println(linhaInfoPeriodo.child(index_periodo+2).text());
              //  System.out.println("10");
                periodo = linhaInfoPeriodo.child(index_periodo+2).text();
                //        System.out.println(linhaNotaAval.child(index_nota+2).text());
            //    System.out.println("11");
                //  System.out.println(linhaNotaAval.child(index_nota+2).text().length() > 0);
                if(!(linhaNotaAval.child(index_nota+2).text().length() > 0) || linhaNotaAval.child(index_nota+2).text() == "") {
                    nota = -1;
                } else {
                    if(!linhaNotaAval.child(index_nota+2).text().contains("-")) {
                        nota = Float.parseFloat(linhaNotaAval.child(index_nota+2).text().replace(",", "."));
                    } else {
                        nota = -1;
                    }
                }
            //    System.out.println("12");
                notas.add(new Nota(disc, abrev, periodo, nota, notaMax, peso, descricao));
             //   System.out.println("13");
                index_nota++;
            } else if(e.id().contains("unid")) {
                //Nota final do periodo
                //desculpa por acidentalmente ter feito o codigo assim, serio. algum dia eu arrumo
                //        System.out.println(index_nota);
                //      System.out.println(linhaInfoPeriodo.child(index_periodo+2).text());
               // System.out.println("14");
                //TODO: eh melhor eu conferir se isso nao eh null de uma vez pra n crashar
                periodo = linhaInfoPeriodo.child(index_periodo+2).text();
                //      System.out.println(linhaNotaAval.child(index_nota+2).text());
               // System.out.println("15");
                //todo: arrumar isso ali em cima e aqui dps
                if(!(linhaNotaAval.child(index_nota+2).text().length() > 0) || linhaNotaAval.child(index_nota+2).text() == "") {
                    nota = -1;
                } else {
                    if(!linhaNotaAval.child(index_nota+2).text().contains("-")) {
                        nota = Float.parseFloat(linhaNotaAval.child(index_nota+2).text().replace(",", "."));
                    } else {
                        nota = -1;
                    }
                }

                //System.out.println(periodo + " " + nota + " " + descricao);
              //  System.out.println("16");
                notas.add(new Nota(disc, "Nota", periodo, nota, 10, 1, "Nota " + periodo)); //Me pergunto se 10 sempre é o maximo aqui....
                index_nota++;
                index_periodo++;
            }
        }
        return notas;
    }

    static protected ArrayList<Avaliacao> paginaDisciplinaAvaliacoes(Document d, Disciplina disc) {
        //td
        //0=dia
        //1=hora
        //2=descricao
        ArrayList<Avaliacao> avaliacoes = new ArrayList<>();

        Element itensTabela = null;
        if(d.getElementsByClass("listing").size() > 0 && d.getElementsByClass("listing").first().getElementsByTag("tbody").size() > 0) {
            itensTabela = d.getElementsByClass("listing").first().getElementsByTag("tbody").first();
        } else return new ArrayList<>();

        for(Element avaliacao : itensTabela.getElementsByTag("tr")) {
            String descricao = "";
            String dia = "", hora = "";
            for(Element dado : avaliacao.getElementsByTag("td")) {
                switch(dado.elementSiblingIndex()) {
                    case 0:
                        dia = dado.text();
                        break;

                    case 1:
                        hora = dado.text().toLowerCase().replace("h", ":");
                        if(hora.toLowerCase().charAt(hora.length() - 1) == ':') {hora += "00";}
                        break;

                    case 2:
                        descricao = dado.text();
                        break;
                }
            }

            //TODO: conferir quando tiver oportunidade se eu nao ferrei a data disso aqui
            if(hora.length() != 5 || hora.charAt(3) != ':') {
                hora = "00:00";
            }
            Date data = new Date();
            try {
                data = Avaliacao.formato_data.parse(dia + " " + hora);
            } catch (ParseException e) {e.printStackTrace();}
            Avaliacao _a = new Avaliacao(disc, data, descricao);
            avaliacoes.add(_a);
        }
        //document.getElementsByClassName("listing")[0].getElementsByTagName("tbody")[0].getElementsByTagName("tr")[0].getElementsByTagName("td")
        return avaliacoes;
    }
    //Pagina tarefas
    static protected ArrayList<Tarefa> paginaTarefasDisciplinaTarefas(Document d, String url_base, Disciplina disc) {
        ArrayList<Tarefa> tarefas = new ArrayList<Tarefa>();

        if(d.getElementsByClass("listing").size() == 0) return tarefas; //Sem tabela -> sem tarefas/página errada

        for(int i = 0; i < d.getElementsByClass("listing").get(0).children().size(); i++) {
            if(d.getElementsByClass("listing").get(0).children().get(i).tagName() == "tbody") {
                Element corpoTabela = d.getElementsByClass("listing").get(0).children().get(i);

                //Cada tarefa ocupa 2 child do corpo. O primeiro é informações, o segundo é somente a descrição
                //0o = 0,1. 1o = 2,3. 2o = 4,5.
                int quantidadeTarefas = (corpoTabela.children().size())/2;
                for(int j = 0; j < quantidadeTarefas; j++) {
                    if(corpoTabela.children().get(j*2) == null || corpoTabela.children().get(j*2+1) == null) {
                        System.out.println(Sessao.logMSG + "Não foi possível parsar uma tarefa");
                        continue;
                    }

                    Tarefa t;
                    Element linhaInformacao = corpoTabela.children().get(j*2);
                    Element linhaDescricao = corpoTabela.children().get(j*2+1);

                    //Informacoes
                    String titulo = linhaInformacao.children().get(1).text();
                    int envios = Integer.parseInt(linhaInformacao.children().get(4).text());

                    String[] datas = linhaInformacao.children().get(2).text().replace("h", ":").split(" ");
                    Date inicio = new Date();
                    Date fim = new Date();
                    try {
                        inicio = Tarefa.formato_data.parse(datas[1] + " " + datas[3]);
                        fim = Tarefa.formato_data.parse(datas[5] + " " + datas[7]);
                    } catch (ParseException e) {e.printStackTrace();}

                    String id = "", j_id = "", j_idEnviar = "", j_idVisualizar = "";
                    boolean enviavel = (inicio.compareTo(new Date()) * new Date().compareTo(fim) > 0) && (linhaInformacao.children().get(5).children().size() == 1);
                    if(enviavel) {
                        System.out.println("\n");
                        String[] x = linhaInformacao.children().get(5).children().get(0).attr("onclick").split("'");

                        id = x[11];
                        j_id = x[3];
                        j_idEnviar = x[5].replace(j_id + ":", "");
                    }
                    boolean enviada = (linhaInformacao.children().get(6).children().size() == 1);
                    if(enviada) {
                        System.out.println("\n");
                        String[] x = linhaInformacao.children().get(6).children().get(0).attr("onclick").split("'");

                        id = x[11]; //Acredito que esses dois sempre são os mesmos para uma mesma tarefa, então não tem problema fazer isso
                        j_id = x[3]; // ^^
                        j_idVisualizar = x[5].replace(j_id + ":", "");
                    }
                    boolean corrigida = (linhaInformacao.children().get(0).children().size() == 1);

                    //Descricao e arquivo
                    //System.out.println(">>> " + corrigida + " " + titulo + " " + envios + " " + enviavel + " " + enviada + " " + tarefa.formato_data.format(inicio) + " " + tarefa.formato_data.format(fim));
                    String descricao = "", url_arquivo = "";
                    for(Element paragrafo : linhaDescricao.children().get(0).children()) {
                        if(paragrafo.text().equals("Baixar arquivo")) {
                            //Arquivo
                            url_arquivo = ((!url_base.startsWith("http://") && !url_base.startsWith("https://")) ? "http://" : "") + url_base + paragrafo.attr("href");
                        } else {
                            //Escrito pelo professor
                            descricao += paragrafo.text() + "\n";
                        }
                    }

                    t = new Tarefa(disc, titulo, descricao, inicio, fim, envios, enviavel, enviada, corrigida);
                    if(url_arquivo != "") t.definirUrlArquivo(url_arquivo);
                    if(enviavel || enviada) t.definirIds(id, j_id);
                    if(enviavel) t.definirIdEnvio(j_idEnviar);
                    if(enviada) t.definirIdVisualizacao(j_idVisualizar);

                    tarefas.add(t);
                }
            }
        }
        return tarefas;
    }

    static protected EnvioTarefa paginaTarefaEnvioTarefa(Document d, Tarefa t, String url_base) {
        Elements campos = d.getElementsByClass("campo");
        if(campos.size() > 0) {
            EnvioTarefa e = new EnvioTarefa(t);
         for(Element c : campos) {
             if(c.previousElementSibling().text().equals("Arquivo:")) {
                 //Arquivo enviado pelo discente
                 String urlArquivo = ((!url_base.startsWith("http://") && !url_base.startsWith("https://")) ? "http://" : "") + url_base + c.child(0).attr("href");
                 e.setUrlArquivo(urlArquivo);
             //    System.out.println(Sessao.logMSG + urlArquivo);
             } else if(c.previousElementSibling().text().equals("Resposta:")) {
                 //Resposta do discente
                 String resposta = formatarTextoDoElemento(c);
                 e.setResposta(resposta);
               //  System.out.println(Sessao.logMSG + resposta);
             } else if(c.previousElementSibling().text().equals("Comentários:")) {
                 //Comentários do discente ou docente
                 Element legenda = c.parent().parent().parent().getElementsByTag("legend").first();
                 if(legenda.text().equals("Resposta enviada")) {
                    //Discente
                     String comentariosDiscente = formatarTextoDoElemento(c);
                     e.setComentariosDiscente(comentariosDiscente);
                   //  System.out.println(Sessao.logMSG + comentariosDiscente);
                 } else {
                     //Docente
                     String comentariosDocente = formatarTextoDoElemento(c);
                     e.setComentariosDocente(comentariosDocente);
                  //   System.out.println(Sessao.logMSG + comentariosDocente);
                 }
             } else if(c.previousElementSibling().text().equals("Nota:")) {
                 //Nota
                 float nota = Float.parseFloat((c.text()).replace(",", "."));
                 e.setNota(nota);
              //   System.out.println(Sessao.logMSG + nota);
             }
            }
            return e;
        }
        return null;
    }

    //TODO: Essa é provavelmente a pior maneira pra fazer isso, o importante é que funciona por enquanto. Tenho que algum dia encontrar uma maneira melhor para formatar corretamente o texto
    static protected String formatarTextoDoElemento(Element e) {
        String texto = "";

        for(int i = 0; i < e.childNodes().size(); i++) {
            Document d = Jsoup.parse(e.childNodes().get(i).toString());

            texto += d.text();

            if(i+1 < e.childNodes().size() && (d.getElementsByTag("p").size() > 0 || d.getElementsByTag("br").size() > 0)) {
                texto += "\n";
            }
        }

        return texto;
    }

    static protected ArrayList<Disciplina> todasTurmasVirtuais(Document d) {
        ArrayList<Disciplina> turmasVirtuais = new ArrayList<>();

        Element bodyTabela = d.getElementsByClass("listagem").get(0).getElementsByTag("tbody").get(1);

        String periodoAtual = "";
        for(Element e : bodyTabela.children()) {
            if(e.className().equals("destaque no-hover")) {
                periodoAtual = e.text();
            } else if(e.className().equals("linhaPar") || e.className().equals("linhaImpar")) {
                String nome = e.child(0).text();
                String botaoAcessoOnClick = e.getElementsByTag("a").get(0).attr("onclick");

                turmasVirtuais.add(new Disciplina(true, periodoAtual, nome, botaoAcessoOnClick.split("'")[3], botaoAcessoOnClick.split("'")[5], botaoAcessoOnClick.split("'")[11]));
            }
        }
        return turmasVirtuais;
    }
}