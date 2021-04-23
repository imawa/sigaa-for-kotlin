package com.stacked.sigaa_ifc;

import java.io.Serializable;

public class AnexoDesconhecido extends Anexo implements Serializable {
    AnexoDesconhecido(Aula aula, String titulo) {
        super(aula, titulo);
    }
}
