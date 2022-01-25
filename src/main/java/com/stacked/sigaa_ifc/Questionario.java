package com.stacked.sigaa_ifc;

import java.util.Date;

public class Questionario {
    private long id;
    private String titulo;
    private boolean enviado;
    private Date dataInicio, dataFim;
    private Disciplina disciplina;

    public Questionario(long id, String titulo, boolean enviado, Date dataInicio, Date dataFim, Disciplina disciplina) {
        this.id = id;
        this.titulo = titulo;
        this.enviado = enviado;
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

    public boolean isEnviado() {
        return enviado;
    }

    public void setEnviado(boolean enviado) {
        this.enviado = enviado;
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
