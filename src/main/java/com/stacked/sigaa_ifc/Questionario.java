package com.stacked.sigaa_ifc;

import java.util.Date;

public class Questionario {
    private long id;
    private String titulo;
    private Date dataInicio, dataFim;
    private Disciplina disciplina;

    public Questionario(long id, String titulo, Date dataInicio, Date dataFim, Disciplina disciplina) {
        this.id = id;
        this.titulo = titulo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.disciplina = disciplina;
    }

    public long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public Date getDataInicio() {
        return dataInicio;
    }

    public Date getDataFim() {
        return dataFim;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }
}
