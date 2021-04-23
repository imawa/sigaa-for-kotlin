package com.stacked.sigaa_ifc;

import java.io.Serializable;

public class AnexoSite extends Anexo implements Serializable {
    private String url;

    AnexoSite(Aula aula, String titulo, String url) {
        super(aula, titulo);
        this.url = url;
    }

    public String getUrl() { return url; }
}