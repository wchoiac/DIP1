package xyz.medirec.medirec;

import java.io.Serializable;

public class PublicKeyProperties implements Serializable {
    private static final long serialVersionUID = 1234123412341L;
    public byte[] encoded;
    public String format;
    public String algorithm;
}