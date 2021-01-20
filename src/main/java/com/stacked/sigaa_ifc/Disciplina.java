package com.stacked.sigaa_ifc;

public class Disciplina {
    private String nome, form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma; //Esses são os 4 obtidos da página principal

    Disciplina(String nome, String form_acessarTurmaVirtual, String form_acessarTurmaVirtual_full, String frontEndIdTurma) {
        this.nome = nome;
        this.form_acessarTurmaVirtual = form_acessarTurmaVirtual;
        this.form_acessarTurmaVirtual_full = form_acessarTurmaVirtual_full;
        this.frontEndIdTurma = frontEndIdTurma;
    }

    public String nome() {
        return nome;
    }

    String[] postArgs() {
        return new String[]{form_acessarTurmaVirtual, form_acessarTurmaVirtual_full, frontEndIdTurma};
    }
}
