package com.stacked.sigaa_ifc;

public class InfoArquivo {
    private String titulo, j_id_jsp, id;
    private Disciplina disciplina;

    InfoArquivo(Disciplina disciplina, String titulo, String j_id_jsp, String id) {
        this.disciplina = disciplina;
        this.titulo = titulo;
        this.j_id_jsp = j_id_jsp;
        this.id = id;
    }

    public Disciplina getDisciplina() {return disciplina;}
    String getJ_id_jsp() { return j_id_jsp; }
    String getId() { return id; }
}