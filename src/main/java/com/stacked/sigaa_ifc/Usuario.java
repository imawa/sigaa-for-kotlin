package com.stacked.sigaa_ifc;

import java.util.ArrayList;

public class Usuario {
    private String nome, nomeAbreviado, campus = "", email = "", urlAvatar, login;
    private int matricula = 0;
    private ArrayList<Disciplina> disciplinasAtuais = new ArrayList<>();
    private ArrayList<BotaoDocumento> botoes = new ArrayList<>();

    Usuario(String nome, String login, String campus, int matricula, ArrayList<Disciplina> disciplinasAtuais, String urlAvatar) {
        this.nome = nome;
        this.login = login;
        this.campus = campus;
        this.matricula = matricula;
        this.disciplinasAtuais = disciplinasAtuais;
        this.urlAvatar = urlAvatar;
    }

    public Integer getMatricula() {
        return this.matricula;
    }
    public String getNome() {return this.nome;}
    public String getUrlAvatar() {return this.urlAvatar;}
    public String getCampus() {return this.campus;}
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
