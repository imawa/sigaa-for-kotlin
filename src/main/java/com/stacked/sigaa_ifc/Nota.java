package com.stacked.sigaa_ifc;

public class Nota {
    /*
        Notas do botão de Ver Notas de cada disciplina
         */
    private String periodo, abrev, descricao;
    private float nota, notaMax, peso;
    private Disciplina disciplina;

    Nota(Disciplina disciplina, String abrev, String periodo, float nota, float notaMax, float peso, String descricao) {
        this.disciplina = disciplina;
        this.abrev = abrev;
        this.periodo = periodo;
        this.nota = nota;
        this.notaMax = notaMax;
        this.peso = peso;
        this.descricao = descricao;
    }

    public float getNota() {
        return nota;
    }
    public float getNotaMax() { return notaMax; }
    public Disciplina getDisciplina() {
        return disciplina;
    }
    public String getAbrev() { return abrev; }
    public String getPeriodo() {
        return periodo;
    }
    public String getDescricao() {
        return descricao;
    }
    public float getPeso() {
        return peso;
    }
}
