package com.stacked.sigaa_ifc;

public class Participante {
    int tipo; //0 = discente; 1 = docente
    int matricula;
    String nome, login, urlAvatar, email;

    Participante(int tipo, String nome, String login, String urlAvatar, String email) {
        this.tipo = tipo;
        this.nome = nome;
        this.login = login;
        this.urlAvatar = urlAvatar;
        this.email = email;
    }

    public boolean discente() {return (tipo == 0);}
    public boolean docente() {return (tipo == 1);}

    void definirMatricula(int matricula) { //Professores não é informado
        this.matricula = matricula;
    }
}
