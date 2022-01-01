package com.stacked.sigaa_ifc;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Tarefa {
    public static SimpleDateFormat formato_data = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    /*
        jid1
        viewstate
        jid1:jidEnviar //botao de enviar
        jid1:jidVer //botao de ver
        id
         */

    private String titulo, descricao, urlDownload = "";
    private Date inicio, fim;
    private int envios;
    private boolean enviavel, enviada, corrigida;
    private Disciplina disciplina;

    public Tarefa(Disciplina disciplina, String titulo, String descricao, Date inicio, Date fim, int envios, boolean enviavel, boolean enviada, boolean corrigida) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.envios = envios;
        this.enviavel = enviavel;
        this.enviada = enviada;
        this.corrigida = corrigida;
        this.disciplina = disciplina;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Date getInicio() {
        return inicio;
    }

    public Date getFim() {
        return fim;
    }

    public int getEnvios() {
        return envios;
    }

    public boolean isEnviavel() {
        return enviavel;
    }

    public boolean isEnviada() {
        return enviada;
    }

    public boolean isCorrigida() {
        return corrigida;
    }

    public boolean contemArquivo() {
        return (urlDownload != "");
    }

    public String getUrlArquivo() {
        return urlDownload;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public void definirIds(String id, String j_id) {
        this.id = id;
        this.j_id = j_id;
    }

    public void setIdEnvio(String id) {
        j_idEnviar = id;
    }

    public void setIdVisualizacao(String id) {
        j_idVisualizar = id;
    }

    public void setUrlArquivo(String dir) {
        urlDownload = dir;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String id, j_id, j_idEnviar, j_idVisualizar;

    public String getId() {
        return id;
    }

    public String getJ_Id() {
        return j_id;
    }

    public void setJ_Id(String j_id) {
        this.j_id = j_id;
    }

    public String getJ_IdEnviar() {
        return j_idEnviar;
    }

    public String getJ_idVisualizar() {
        return j_idVisualizar;
    }

    public String[] getPostArgsEnviar() {
        return new String[]{j_id, (j_id + ":" + j_idEnviar), id};
    }

    public String[] getPostArgsVisualizar() {
        return new String[]{j_id, (j_id + ":" + j_idVisualizar), id};
    }
}
