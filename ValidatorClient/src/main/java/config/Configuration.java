package config;

import java.io.File;

public class Configuration {




    //blockchain setting
    public static final String ELIPTIC_CURVE="secp256k1";
    public static final String ELIPTIC_CURVE_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String BLOCKCHAIN_HASH_ALGORITHM ="SHA256";



    public static final int MAX_NAME_LENGTH= Byte.MAX_VALUE;
    public static final int HASH_LENGTH =32;
    public static final int SIGNATURE_LENGTH=64;
    public static final int IDENTIFIER_LENGTH=20;
    public static final int RAW_PUBLICKEY_LENGTH=33;

}
