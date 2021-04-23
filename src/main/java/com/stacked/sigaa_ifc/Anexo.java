package com.stacked.sigaa_ifc;

import java.io.Serializable;

public abstract class Anexo implements Serializable {
    private String titulo = "";
    private Aula aula;

    Anexo(Aula aula, String titulo) {
        this.aula = aula;
        this.titulo = titulo;
    }

    public String getTitulo() { return titulo; }

    Aula getAula() {return aula;}
}

