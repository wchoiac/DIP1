package blockchain.block;

import blockchain.block.transaction.Transaction;
import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException {


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
        byte[] raw =parsedTransaction.getRaw();

        assertEquals(Transaction.parse(raw), parsedTransaction);

    }

    @Test
    void signatureVerificationTestCorrect() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();

        KeyPair testPatientKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeypair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey)testPatientKeypair.getPrivate();

        long timestamp = System.currentTimeMillis();
        byte[] content =new byte[10];
        byte[] signatureCoverage=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp), content,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey));
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);


        Transaction testTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey)
                , timestamp,content, signature
                ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey)
                ,testMedicalOrgPrivateKey);

        assertTrue(testTransaction.verify(testPatientPublicKey, testMedicalOrgPublicKey));

    }


    @Test
    void signatureVerificationTestWrong() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();

        KeyPair testPatientKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeypair.getPublic();
        ECPrivateKey testPatientPrivateKey =(ECPrivateKey)testPatientKeypair.getPrivate();

        long timestamp = System.currentTimeMillis();
        byte[] content =new byte[10];
        byte[] signatureCoverage=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp), content,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey));
        byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
        signature[0]-=1;


        Transaction testTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey)
                , timestamp,content, signature
                ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey)
                ,testMedicalOrgPrivateKey);

        assertFalse(testTransaction.verify(testPatientPublicKey, testMedicalOrgPublicKey));
    }
}
