package com.stacked.sigaa_ifc;

import java.io.Serializable;

public class Disciplina implements Serializable {
    private String id = null; //Somente as atuais possuem isso. É utilizado somente para abrir uma tarefa direto
    private String nome, periodo, form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma;
    private boolean paginaTodasTurmasVirtuais;

    Disciplina(boolean paginaTodasTurmasVirtuais, String periodo, String nome, String form_acessarTurmaVirtual, String form_acessarTurmaVirtual_full, String frontEndIdTurma) {
        this.paginaTodasTurmasVirtuais = paginaTodasTurmasVirtuais;
        this.periodo = periodo;
        this.nome = nome;
        this.form_acessarTurmaVirtual = form_acessarTurmaVirtual;
        this.form_acessarTurmaVirtual_full = form_acessarTurmaVirtual_full;
        this.frontEndIdTurma = frontEndIdTurma;
    }

    public String getNome() {
        return nome;
    }
    public String getPeriodo() {
        return periodo;
    }

    String[] postArgs() {
        return new String[]{form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma};
    }

    boolean paginaTodasTurmasVirtuais() {
        return paginaTodasTurmasVirtuais;
    }

    String id() {
        return id;
    }
    void definirId(String id) {
        this.id = id;
    }
}
