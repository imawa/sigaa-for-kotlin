package com.stacked.sigaa_ifc;

import java.util.ArrayList;

public class FormTarefa {
    private Tarefa t;
    private String j_id_jsp;
    private ArrayList<String> infoEscondidaForm;
    private boolean input_comentarios, input_arquivo, input_arquivo_obrigatorio, input_resposta, input_resposta_obrigatorio;

    FormTarefa(Tarefa t, String j_id_jsp, ArrayList<String> infoEscondidaForm, boolean input_comentarios, boolean input_arquivo, boolean input_arquivo_obrigatorio, boolean input_resposta, boolean input_resposta_obrigatorio) {
            this.t = t;
            this.j_id_jsp = j_id_jsp;
            this.infoEscondidaForm = infoEscondidaForm;
            this.input_comentarios = input_comentarios;
            this.input_arquivo = input_arquivo;
            this.input_arquivo_obrigatorio = input_arquivo_obrigatorio;
            this.input_resposta = input_resposta;
            this.input_resposta_obrigatorio = input_resposta_obrigatorio;
    }

    public Tarefa getTarefa() { return t; }
    public boolean aceitaComentarios() { return input_comentarios; }
    public boolean aceitaArquivo() { return input_arquivo; }
    public boolean aceitaResposta() { return input_resposta; }
    public boolean arquivoObrigatorio() { return input_arquivo_obrigatorio; }
    public boolean respostaObrigatoria() { return input_resposta_obrigatorio; }

    String j_id_jsp() { return j_id_jsp; }
    ArrayList<String> infoEscondidaForm() { return infoEscondidaForm; }

    private String comentarios = "", nomeArquivo = null;
    private byte[] arquivo = null;
    public void setArquivo(String nomeArquivo, byte[] arquivo) {
        if(!aceitaArquivo()) return;
        this.nomeArquivo = nomeArquivo;
        this.arquivo = arquivo;
    }
    public void setComentarios(String comentarios) {
        if(!aceitaComentarios()) return;
        this.comentarios = comentarios;
    }
    public String getNomeArquivo() { return nomeArquivo; }
    public byte[] getArquivo() { return arquivo; }
    public String getComentarios() { return comentarios; }

}
