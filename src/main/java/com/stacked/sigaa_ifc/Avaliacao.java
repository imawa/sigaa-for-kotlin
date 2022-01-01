package com.stacked.sigaa_ifc;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Avaliacao {
    public static SimpleDateFormat formato_data = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    /*
    Avaliações do "VER AVALIAÇÕES" das disciplinas
     */
    private long id;
    private Date data;
    private String descricao;
    private Disciplina disciplina;

    public Avaliacao(long id, Disciplina disciplina, Date data, String descricao) {
        this.id = id;
        this.disciplina = disciplina;
        this.data = data;
        this.descricao = descricao;
    }

    public long getId() {
        return id;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public Date getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }
}
