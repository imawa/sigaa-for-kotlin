package com.stacked.sigaa_ifc;

public class EnvioTarefa {
    private Tarefa t;

    boolean contem_arquivo, contem_comentarios_discente, contem_resposta, contem_nota, contem_comentarios_docente;
    String url_arquivo = "", resposta = "", comentarios_discente = "", comentarios_docente = "";
    float nota;

    EnvioTarefa(Tarefa t) {
        this.t = t;
    }

    public boolean contemArquivo() { return contem_arquivo; }
    public String getUrlArquivo() { return url_arquivo; }
    public boolean contemComentariosDiscente() { return contem_comentarios_discente; }
    public String getComentariosDiscente() { return comentarios_discente; }
    public boolean contemComentariosDocente() { return contem_comentarios_docente; }
    public String getComentariosDocente() { return comentarios_docente; }
    public boolean contemResposta() { return contem_resposta; }
    public String getResposta() { return resposta; }
    public boolean contemNota() { return contem_nota; }
    public float getNota() { return nota; }


    void setUrlArquivo(String url_arquivo) {
        this.url_arquivo = url_arquivo;
        contem_arquivo = true;
    }
    void setResposta(String resposta) {
        this.resposta = resposta;
        contem_resposta = true;
    }
    void setComentariosDiscente(String comentarios_discente) {
        this.comentarios_discente = comentarios_discente;
        contem_comentarios_discente = true;
    }
    void setNota(float nota) {
        this.nota = nota;
        contem_nota = true;
    }
    void setComentariosDocente(String comentarios_docente) {
        this.comentarios_docente = comentarios_docente;
        contem_comentarios_docente = true;
    }
}
