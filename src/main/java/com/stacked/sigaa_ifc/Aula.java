package com.stacked.sigaa_ifc;

import java.util.ArrayList;

public class Aula {
    private String titulo, html;
    private ArrayList<Anexo> anexos = new ArrayList<>();
    private Disciplina disciplina;

    Aula(Disciplina disciplina, String titulo, String html) {
        this.disciplina = disciplina;
        this.titulo = titulo;
        this.html = html;
    }

    //anexosSIAGA

    public String getTitulo() { return titulo; }
    public String getHtml() { return html; }
    Disciplina getDisciplina() {return disciplina;}

    void adicionarAnexo(Anexo a) { anexos.add(a); }
    public ArrayList<Anexo> getAnexos() { return anexos; }

}
