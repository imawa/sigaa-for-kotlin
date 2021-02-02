package com.stacked.sigaa_ifc;

import java.util.ArrayList;

public class Usuario {//TODO: AVATAR, arrumar o construtor. substituir [] por list
    private String nome, campus, email, urlAvatar;
    private int matricula;
    private ArrayList<Disciplina> disciplinasAtuais;
    private ArrayList<BotaoDocumento> botoes = new ArrayList<>();

    //Usuario do login
    Usuario(String nome, String campus, int matricula, ArrayList<Disciplina> disciplinasAtuais, String urlAvatar) {
        this.nome = nome;
        this.campus = campus;
        this.matricula = matricula;
        this.disciplinasAtuais = disciplinasAtuais;
        this.urlAvatar = urlAvatar;
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

    public Integer matricula() {
        return this.matricula;
    }
    public String nome() {return this.nome;}
    public String urlAvatar() {return this.urlAvatar;}
    public String campus() {return this.campus;}
    //public String email() {return this.email;}

    public ArrayList<Disciplina> disciplinasAtuais() {
        return this.disciplinasAtuais;
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
}
