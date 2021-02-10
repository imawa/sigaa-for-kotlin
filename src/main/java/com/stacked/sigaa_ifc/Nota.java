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

    public float nota() {
        return nota;
    }
    public float notaMax() { return notaMax; }
    public Disciplina disciplina() {
        return disciplina;
    }
    public String abrev() { return abrev; }
    public String periodo() {
        return periodo;
    }
    public String descricao() {
        return descricao;
    }
    public float peso() {
        return peso;
    }
}
