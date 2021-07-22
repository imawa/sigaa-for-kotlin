package com.stacked.sigaa_ifc;

public class Arquivo {
    private String nome;
    private byte[] bytes;

    Arquivo(String nome, byte[] bytes) {
        this.nome = nome;
        this.bytes = bytes;
    }

    public String getNome() {return nome;}
    public byte[] getBytes() {return bytes;}
}
