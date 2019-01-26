package blockchain.block;

import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatientInfoTest {


    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {
        KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

        long timestamp = System.currentTimeMillis();
        byte[] encryptedInfo =new byte[10];
        byte[] signatureCoverage=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

        PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);

        byte[] raw =testPatientInfo.getRaw();

        PatientInfo parsedPatientInfo = PatientInfo.parse(raw);
        assertEquals(parsedPatientInfo,testPatientInfo);

    }


    @Test
    void signatureVerificationTestCorrect() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {
        KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

        long timestamp = System.currentTimeMillis();
        byte[] encryptedInfo =new byte[10];
        byte[] signatureCoverage=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

        PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);

        assertTrue(testPatientInfo.verify());

    }

    @Test
    void signatureVerificationTestWrong() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {
        KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

        long timestamp = System.currentTimeMillis();
        byte[] encryptedInfo =new byte[10];
        byte[] signatureCoverage=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        signature[0]-=1;

        PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);

        assertFalse(testPatientInfo.verify());

    }
}
