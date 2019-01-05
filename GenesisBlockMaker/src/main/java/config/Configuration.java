package config;

import java.io.File;

public class Configuration {


    public static final File GENESISBLOCK_FILE = new File("genesisBlock");

    public static final String ELIPTIC_CURVE="secp256k1";
    public static final String BLOCKCHAIN_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String BLOCKCHAIN_HASH_ALGORITHM ="SHA-256";
    public static final int ELIPTIC_CURVE_COORDINATE_LENGTH=32;

    //block setting
    public static final int MAX_NAME_LENGTH= Byte.MAX_VALUE;
    public static final int HASH_LENGTH =32;
    public static final int SIGNATURE_LENGTH=64;
    public static final int IDENTIFIER_LENGTH=20;
    public static final int RAW_PUBLICKEY_LENGTH=33;
        // patient info setting
        public static final byte INITIAL_AUTHORITIES_BIT_POSITION=0;
        public static final byte VOTE_BIT_POSITION=1;
        public static final byte AUTHORIZATION_BIT_POSITION=2;
        public static final byte REVOCATION_BIT_POSITION=3;
        public static final byte PATIENT_REGISTRATION_BIT_POSITION=4;
        public static final byte PATIENT_INFO_UPDATE_BIT_POSITION=5;
        public static final byte TRANSACTION_BIT_POSITION=6;

}
