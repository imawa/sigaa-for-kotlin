package com.stacked.sigaa_ifc;

import java.io.Serializable;

enum idBotaoDocumento {
    MAIN_MEUS_DADOS,
    DISC_PARTICIPANTES,
    DISC_VER_NOTAS,
    DISC_ARQUIVOS,
    DISC_VER_AVALIACOES,
    DISC_VER_TAREFAS;
}

public class BotaoDocumento implements Serializable {
    /*
        Em suma: cada botão que voce clica no SIGAA requer 4 valores únicos (2 pares) pra cada pessoa chamados de j_id_jsp_{numero}. O botão "Ver Notas", "Participantes" etc. de cada disciplina possui um valor diferente pra cada usuario
        Eu to usando essa classe pra armazenar os botoes de cada disciplina com um id pra identificar o que faz
         */
    private String[][] j_id_jsp;
    private idBotaoDocumento id;

    BotaoDocumento(idBotaoDocumento id, String[][] j_id_jsp) {
        this.id = id;
        this.j_id_jsp = j_id_jsp;
    }
    idBotaoDocumento id() {
        return id;
    }
    String[][] j_id_jsp() {
        return j_id_jsp;
    }
}
