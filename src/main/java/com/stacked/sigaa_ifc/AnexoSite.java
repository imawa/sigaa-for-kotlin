package com.stacked.sigaa_ifc;

public class AnexoSite extends Anexo {
    private String url;

    AnexoSite(Aula aula, String titulo, String url) {
        super(aula, titulo);
        this.url = url;
    }

    public String getUrl() { return url; }
}