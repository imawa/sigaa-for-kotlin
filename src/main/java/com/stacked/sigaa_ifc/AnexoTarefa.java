package com.stacked.sigaa_ifc;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AnexoTarefa extends Anexo implements Serializable {
    public static SimpleDateFormat formato_data = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private Date inicio, fim;
    private boolean enviavel;

    AnexoTarefa(Aula aula, String titulo, String id, Date inicio, Date fim, boolean enviavel) {
        super(aula, titulo);
        this.id = id;
        this.inicio = inicio;
        this.fim = fim;
        this.enviavel = enviavel;
    }

    public Date getInicio() { return inicio; }
    public Date getFim() { return fim; }
    public boolean enviavel() { return enviavel; }

    private String id;
    String getId() { return id; }
}
