package config;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;

import java.io.File;

public class Configuration {

    public static final File AUTHENTICATION_FOLDER =new File("auth");

    public static final File SIGNING_KEYSTORE_FILE = new File(AUTHENTICATION_FOLDER,"signingKeyStore.keystore"); // for validating blocks and issuing other certs
    public static final String SIGNING_CERTIFICATE_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String SIGNING_KEYSTORE_ALIAS ="DIP1-SIGNING";


    public static final File CONNECTION_KEYSTORE_FILE = new File(AUTHENTICATION_FOLDER,"connectionKeystore.keystore"); // for tls connection in blockchain
    public static final String CONNECTION_KEYSTORE_ALIAS ="DIP1-CONNECTION";
    public static final String CONNECTION_CERTIFICATE_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String[] CONNECTION_TLS_CIPHER_SUITE =new String[]{"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
            ,"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256","TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
            ,"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};


    public static final File API_KEYSTORE_FILE = new File(AUTHENTICATION_FOLDER,"apiKeystore.keystore"); // for tls in api
    public static final String API_KEYSTORE_ALIAS ="DIP1-API";
    public static final String API_CERTIFICATE_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String[] API_TLS_CIPHER_SUITE =new String[]{"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
            ,"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256","TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
            ,"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};
    public static final String PASSWORD_HASH_ALGORITHM ="PBKDF2WithHmacSHA512";
    public static final int PASSWORD_HASH_LENGTH =64;
    public static final int PASSWORD_SALT_LENGTH =16;
    public static final byte ROOT_USER_LEVEL =0;
    public static final byte USER_LEVEL =1;


    public static final File API_DATA_FOLDER= new File("apiData");
    public static final File USERINFO_FOLDER = new File(API_DATA_FOLDER,"userInfo");
    public static final File SESSION_FOLDER = new File(API_DATA_FOLDER,"session");
    public static final File ISSUED_CERT_FOLDER = new File(API_DATA_FOLDER,"issuedCert");
    public static final String API_LOG_FILENAME ="APILogFile.log";
    public static final int API_SERVER_PORT = 443;


    public static final File BLOCKCHAIN_DATA_FOLDER= new File("blockChainData");
    public static final File BLOCK_FOLDER= new File(BLOCKCHAIN_DATA_FOLDER,"block");
    public static final File AUTHORITY_FOLDER= new File(BLOCKCHAIN_DATA_FOLDER,"authority");
    public static final File BEST_CHAIN_FILE= new File(BLOCKCHAIN_DATA_FOLDER,"bestChain");
    public static final File MEDICAL_ORGANIZATION_FOLDER = new File(BLOCKCHAIN_DATA_FOLDER,"medicalOrg");
    public static final File AUTHORIZATION_AND_REVOCATION_RECORD_FILE= new File(MEDICAL_ORGANIZATION_FOLDER,"record");
    public static final File DROPPED_VOTE_FOLDER = new File(BLOCKCHAIN_DATA_FOLDER,"droppedVotes");
    public static final File PATIENT_FOLDER = new File(BLOCKCHAIN_DATA_FOLDER,"patient");
    public static final File GENESISBLOCK_FILE = new File("genesisBlock");
    public static final String BLOCKCHAIN_LOG_FILENAME ="BlockChainLogFile.log";


    //blockchain setting
    public static final int BLOCK_PERIOD = 30000;
    public static final int SYNC_PERIOD = 2* BLOCK_PERIOD;
    public static final int MAXIMUM_RESPONSE_WAITING_TIME=15000;
    public static final int MIN_OUT_ORDER_BLOCK_PERIOD = 35000;
    public static final int MAX_BLOCK_ON_MEMEORY = 20;
    public static final byte IN_ORDER = 2;
    public static final byte OUT_ORDER = 1;
    public static final String ELIPTIC_CURVE="secp256k1";
    public static final ASN1ObjectIdentifier ELIPTIC_CURVE_OID= SECObjectIdentifiers.secp256k1;
    public static final String BLOCKCHAIN_SIGNATURE_ALGORITHM ="SHA256withECDSA";
    public static final String BLOCKCHAIN_HASH_ALGORITHM ="SHA-256";
    public static final int ELIPTIC_CURVE_COORDINATE_LENGTH=32;
    public static final int CHECK_POINT_BLOCK_INTERVAL =30000;
    ;
    //block setting
    public static final int MAX_RECORD = 5000;
    public static final int MAX_AUTHORIZATION = 10;
    public static final int MAX_REVOCATION = 10;
    public static final int MAX_PATIENT_INFO = 50;
    public static final int MAX_UPDATE = 50;

    public static final int MAX_NAME_LENGTH= Byte.MAX_VALUE;
    public static final int HASH_LENGTH =32;
    public static final int SIGNATURE_LENGTH=64;
    public static final int IDENTIFIER_LENGTH=20;
    public static final int RAW_PUBLICKEY_LENGTH=33;

    public static final byte INITIAL_AUTHORITIES_BIT_POSITION=0;
    public static final byte VOTE_BIT_POSITION=1;
    public static final byte AUTHORIZATION_BIT_POSITION=2;
    public static final byte REVOCATION_BIT_POSITION=3;
    public static final byte PATIENT_REGISTRATION_BIT_POSITION=4;
    public static final byte TRANSACTION_BIT_POSITION=5;

    //node setting
    public static final int MAX_OUT_BOUND_CONNECTION= 5;
    public static final int MAX_IN_BOUND_CONNECTION= 100;
    public static final int MAX_HEADER_NUMBER_PER_REQUEST=1000;

    //message number
    public static final byte MESSAGE_STATUS=0;
    public static final byte MESSAGE_PEER_NODE_REQUEST=1;
    public static final byte MESSAGE_HEADER_REQUEST=2;
    public static final byte MESSAGE_BLOCK_REQUEST=3;
    public static final byte MESSAGE_PEER_NODE_LIST=4;
    public static final byte MESSAGE_HEADER_LIST=5;
    public static final byte MESSAGE_TRANSACTION=6;
    public static final byte MESSAGE_BLOCK=7;

    //currently no limit in size of header, block, transaction
    public static final int MAX_HEADER_SIZE=Integer.MAX_VALUE;
    public static final int MAX_TRANSACTION_SIZE=Integer.MAX_VALUE;
    public static final int MAX_BLOCK_SIZE=Integer.MAX_VALUE;
    public static final int MAX_MESSAGE_SIZE=MAX_HEADER_SIZE*MAX_HEADER_NUMBER_PER_REQUEST>MAX_BLOCK_SIZE?
            MAX_HEADER_SIZE*MAX_HEADER_NUMBER_PER_REQUEST:MAX_BLOCK_SIZE;

    //node setting
    public static final int NODE_SERVER_PORT = 7777;
    public static final int NUM_OF_BLOCK_REQUEST_AT_ONCE= 20;
    public static final int BLOCK_REQUEST_TIME_OUT= 5000;
}
