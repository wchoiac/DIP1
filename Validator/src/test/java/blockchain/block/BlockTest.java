package blockchain.block;

import blockchain.block.transaction.Transaction;
import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockTest {


    //for no content
    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {
        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , null
                , null
                , null
                , null);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }

    //for authorization
    @Test
    void rawAndParseTest2() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        KeyPair testMedicalOrgKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeyPair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey) testMedicalOrgKeyPair.getPrivate();
        MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("test", testMedicalOrgPublicKey);
        MedicalOrgInfo[] authorizationList = new MedicalOrgInfo[]{testMedicalOrgInfo};


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , authorizationList
                , null
                , null
                , null);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }

    //for revocation
    @Test
    void rawAndParseTest3() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        KeyPair testMedicalOrgKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeyPair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey) testMedicalOrgKeyPair.getPrivate();
        MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("test", testMedicalOrgPublicKey);
        byte[][] identifiers = new byte[][]{testMedicalOrgInfo.getIdentifier()};


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , null
                , identifiers
                , null
                , null);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }

    //for patient registration
    @Test
    void rawAndParseTest4() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();
        long timestamp = System.currentTimeMillis();
        byte[] encryptedInfo =new byte[10];
        byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);
        PatientInfo[] registrationList = new PatientInfo[]{testPatientInfo};


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , null
                , null
                , registrationList
                , null);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }


    //for transactions
    @Test
    void rawAndParseTest5() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


        KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();
        KeyPair testPatientKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeypair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey)testPatientKeypair.getPrivate();
        byte[] content =new byte[10];
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, content
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        Transaction parsedTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey)
                , System.currentTimeMillis(),content, signature
                ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey)
                ,testMedicalOrgPrivateKey);
        Transaction[] transactionList = new Transaction[]{parsedTransaction};


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , null
                , null
                , null
                , transactionList);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }

    //for all
    @Test
    void rawAndParseTest6() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


        KeyPair testMedicalOrgKeyPair2 = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey2 =(ECPublicKey) testMedicalOrgKeyPair2.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey2 =(ECPrivateKey) testMedicalOrgKeyPair2.getPrivate();
        MedicalOrgInfo testMedicalOrgInfo2 = new MedicalOrgInfo("test", testMedicalOrgPublicKey2);
        MedicalOrgInfo[] authorizationList = new MedicalOrgInfo[]{testMedicalOrgInfo2};

        KeyPair testMedicalOrgKeyPair3 = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey3 =(ECPublicKey) testMedicalOrgKeyPair3.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey3 =(ECPrivateKey) testMedicalOrgKeyPair3.getPrivate();
        MedicalOrgInfo testMedicalOrgInfo3 = new MedicalOrgInfo("test", testMedicalOrgPublicKey3);
        byte[][] revocationList = new byte[][]{testMedicalOrgInfo2.getIdentifier()};

        KeyPair testPatientKeyPair4 = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey4 =(ECPublicKey) testPatientKeyPair4.getPublic();
        ECPrivateKey testPatientPrivateKey4 =(ECPrivateKey) testPatientKeyPair4.getPrivate();
        long timestamp4 = System.currentTimeMillis();
        byte[] encryptedInfo4 =new byte[10];
        byte[] signatureCoverage4= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp4),encryptedInfo4);
        byte[] signature4 = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey4, signatureCoverage4
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        PatientInfo testPatientInfo4 = new PatientInfo(timestamp4,testPatientPublicKey4,encryptedInfo4,signature4);
        PatientInfo[] registrationList = new PatientInfo[]{testPatientInfo4};


        KeyPair testMedicalOrgKeypair5 = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey5 =(ECPublicKey) testMedicalOrgKeypair5.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey5 =(ECPrivateKey)testMedicalOrgKeypair5.getPrivate();
        KeyPair testPatientKeypair5 = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey5 =(ECPublicKey) testPatientKeypair5.getPublic();
        ECPrivateKey testPatientPrivateKey5 =(ECPrivateKey)testPatientKeypair5.getPrivate();
        byte[] content =new byte[10];
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey5, content
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        Transaction parsedTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey5)
                , System.currentTimeMillis(),content, signature
                ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey5)
                ,testMedicalOrgPrivateKey5);
        Transaction[] transactionList = new Transaction[]{parsedTransaction};


        Block testBlock =new Block(testAuthorityPrivateKey, null, null, 1
                , (byte)0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                , authorizationList
                , revocationList
                , registrationList
                , transactionList);

        byte[] raw =testBlock.getRaw();

        Block parsedBlock =Block.parse(raw);


        assertEquals(parsedBlock,testBlock);
    }

}
