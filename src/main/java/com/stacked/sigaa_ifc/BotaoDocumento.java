package com.stacked.sigaa_ifc;

enum idBotaoDocumento {
    MEUS_DADOS,
    VER_NOTAS,
    VER_TAREFAS;
}

public class BotaoDocumento {
    /*
        Em suma: cada botão que voce clica no SIGAA requer 4 valores únicos (2 pares) pra cada pessoa chamados de j_id_jsp_{numero}. O botão "Ver Notas", "Participantes" etc. de cada disciplina possui um valor diferente pra cada usuario
        Eu to usando essa classe pra armazenar os botoes de cada disciplina com um id pra identificar o que faz

        Ignorar isso aqui. Vou identificar pelo enum ali de cima
        *==CLASSIFICACAO POR ID==*
        --0 = meus dados (main page)
        --1 = notas; (disciplina)
        2 = participantes; (disciplina)
        3 = fóruns; (disciplina)
        4 = notícias; (disciplina)
        5 = frequencia; (disciplina)
        --6 = tarefas; (disciplina)
        7 = questionários; (disciplina)
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
