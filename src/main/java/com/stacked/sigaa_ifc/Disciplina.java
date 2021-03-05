package com.stacked.sigaa_ifc;

public class Disciplina {
    private String id = null; //Somente as atuais possuem isso. É utilizado somente para abrir uma tarefa direto
    private String nome, form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma; //Esses são os 4 obtidos da página principal

    Disciplina(String nome, String form_acessarTurmaVirtual, String form_acessarTurmaVirtual_full, String frontEndIdTurma) {
        this.nome = nome;
        this.form_acessarTurmaVirtual = form_acessarTurmaVirtual;
        this.form_acessarTurmaVirtual_full = form_acessarTurmaVirtual_full;
        this.frontEndIdTurma = frontEndIdTurma;
    }

    public String getNome() {
        return nome;
    }

    String[] postArgs() {
        return new String[]{form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma};
    }

    String id() {
        return id;
    }
    void definirId(String id) {
        this.id = id;
    }
}
