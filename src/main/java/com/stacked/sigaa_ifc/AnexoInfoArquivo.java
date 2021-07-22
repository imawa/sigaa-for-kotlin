package com.stacked.sigaa_ifc;

public class AnexoInfoArquivo extends Anexo {
    private String j_id_jsp, id;

    AnexoInfoArquivo(Aula aula, String titulo, String j_id_jsp, String id) {
        super(aula, titulo);
        this.j_id_jsp = j_id_jsp;
        this.id = id;
    }

    String getJ_id_jsp() { return j_id_jsp; }
    String getId() { return id; }
}