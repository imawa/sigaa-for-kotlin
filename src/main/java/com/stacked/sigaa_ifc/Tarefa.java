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

    Tarefa(String titulo, String descricao, Date inicio, Date fim, int envios, boolean enviavel, boolean enviada, boolean corrigida) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.envios = envios;
        this.enviavel = enviavel;
        this.enviada = enviada;
        this.corrigida = corrigida;
    }

    public String titulo() { return titulo; }
    public String descricao() { return descricao; }
    public Date inicio() { return inicio; }
    public Date fim() { return fim; }
    public int envios() { return envios; }
    public boolean enviavel() { return enviavel; }
    public boolean enviada() { return enviada; }
    public boolean corrigida() { return corrigida; }
    public boolean contemArquivo() { return (urlDownload != ""); }
    public String urlArquivo() { return urlDownload; }

    void definirIds(String id, String j_id) {
        this.id = id;
        this.j_id = j_id;
    }
    void definirIdEnvio(String id) { j_idEnviar = id; }
    void definirIdVisualizacao(String id) { j_idVisualizar = id; }
    void definirUrlArquivo(String dir) { urlDownload = dir; }

    private String id, j_id, j_idEnviar, j_idVisualizar;
    String[] postArgsEnviar() { return new String[]{j_id, (j_id + ":" + j_idEnviar), id}; }
    String[] postArgsVisualizar() { return new String[]{j_id, (j_id + ":" + j_idVisualizar), id}; }
}
