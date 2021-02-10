package com.stacked.sigaa_ifc;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Avaliacao {
    public static SimpleDateFormat formato_data = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    /*
    Avaliações do "VER AVALIAÇÕES" das disciplinas
     */
    private Date data;
    private String descricao;
    private Disciplina disciplina;

    public Avaliacao(Disciplina disciplina, Date data, String descricao) {
        this.disciplina = disciplina;
        this.data = data;
        this.descricao = descricao;
    }

    public Disciplina disciplina() {return disciplina;}
    public Date data() {return data;}
    public String descricao() {return descricao;}
}
