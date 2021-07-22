package com.stacked.sigaa_ifc;

import java.io.IOException;

public class ExcecaoSemInternet extends IOException {
    public ExcecaoSemInternet() {
        super("Internet não disponível");
    }
}
