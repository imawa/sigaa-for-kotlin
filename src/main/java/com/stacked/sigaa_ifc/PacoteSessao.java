package com.stacked.sigaa_ifc;

import java.io.Serializable;

public class PacoteSessao implements Serializable {
    private String JSESSIONID;
    private String url_base;
    private Usuario usuario;

    protected PacoteSessao(Sessao sessao) {
        this.JSESSIONID = sessao.getJSESSIONID();
        this.url_base = sessao.getUrlBase();
        this.usuario = sessao.getUsuario();
    }

    protected String getJSESSIONID() {
        return JSESSIONID;
    }

    protected String getUrl_base() {
        return url_base;
    }

    protected Usuario getUsuario() {
        return usuario;
    }
}
