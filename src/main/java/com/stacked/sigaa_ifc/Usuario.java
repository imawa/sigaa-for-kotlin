package com.stacked.sigaa_ifc;

import java.util.ArrayList;

enum tipoUsuario {
    DISCENTE,
    DOCENTE;
}

public class Usuario {
    private String nome, nomeAbreviado, campus = "", email = "", urlAvatar, login;
    private int matricula = 0;
    private ArrayList<Disciplina> disciplinasAtuais = new ArrayList<>();
    private ArrayList<BotaoDocumento> botoes = new ArrayList<>();
    private tipoUsuario tipo;

    //Usuario do login
    Usuario(tipoUsuario tipo, String nome, String login, String campus, int matricula, ArrayList<Disciplina> disciplinasAtuais, String urlAvatar) {
        this.tipo = tipo;
        this.nome = nome;
        this.login = login;
        this.campus = campus;
        this.matricula = matricula;
        this.disciplinasAtuais = disciplinasAtuais;
        this.urlAvatar = urlAvatar;
    }

    //Usuario do participantes
    Usuario(tipoUsuario tipo, String nome, String login, String urlAvatar, String email) {
        this.tipo = tipo;
        this.nome = nome;
        this.login = login;
        this.urlAvatar = urlAvatar;
        this.email = email;
    }
    void definirMatricula(Integer matricula) { //Professores não é informado
        this.matricula = matricula;
    }

    //Usuario  > +email, +botoes disciplina
        /*usuario(String nome, String campus, int matricula, disciplina[] disciplinas, String urlAvatar) {
            this.nome = nome;
            this.campus = campus;
            this.matricula = matricula;
            this.disciplinas = disciplinas;
            this.urlAvatar = urlAvatar;
        }*/

    //TODO: FAZER 1 SO E ADICIONAR DISCIPLINAS COM VOIDI
    //Usuario sem disciplinas
    // usuario(String nome, String campus, String email, int matricula, ArrayList<botaoDocumento> botoes) {
    //     this.nome = nome;
    //     this.campus = campus;
    //      this.email = email;
    //      this.matricula = matricula;
    //      this.botoes = botoes;
    // }

    public Integer getMatricula() {
        return this.matricula;
    }
    public String getNome() {return this.nome;}
    public String getUrlAvatar() {return this.urlAvatar;}
    public String getCampus() {return this.campus;}
    public boolean discente() {return (tipo == tipoUsuario.DISCENTE);}
    public boolean docente() {return (tipo == tipoUsuario.DOCENTE);}
    public String getEmail() {return this.email;}
    public String getUsuario() {return this.login;}

    public ArrayList<Disciplina> getDisciplinasAtuais() {
        return this.disciplinasAtuais;
    }
    public Disciplina getDisciplina(String nome) {
        for(Disciplina d : disciplinasAtuais) {
            if(d.getNome().equals(nome)) return d;
        }
        return null;
    }

    ArrayList<BotaoDocumento> botoes() {
        return this.botoes;
    }
    BotaoDocumento botao(idBotaoDocumento idBotao) {
        for(BotaoDocumento x : botoes) {
            if(x.id() == idBotao) return x;
        }
        return null;
    }
    void adicionarBotao(BotaoDocumento b) {
        botoes.add(b);
    }

    void setNomeAbreviado(String nomeAbreviado) { this.nomeAbreviado = nomeAbreviado; }
    String getNomeAbreviado() { return nomeAbreviado; }
}
