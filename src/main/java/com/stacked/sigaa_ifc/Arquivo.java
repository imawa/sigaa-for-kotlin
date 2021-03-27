package com.stacked.sigaa_ifc;

import java.io.InputStream;

public class Arquivo {
    private String nome;
    private InputStream byteStream;

    Arquivo(String nome, InputStream byteStream) {
        this.nome = nome;
        this.byteStream = byteStream;
    }

    public String getNome() {return nome;}
    public InputStream getByteStream() {return byteStream;}
}
