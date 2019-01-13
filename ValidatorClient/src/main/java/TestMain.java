import blockchain.BlockChainSecurityHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.Configuration;
import general.security.SecurityHelper;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.symmetric.SEED;
import org.glassfish.jersey.message.internal.EntityInputStream;
import pojo.LocationPojo;
import pojo.PatientInfoContentPojo;
import pojo.PatientInfoPojo;
import pojo.PatientShortInfoPojo;
import general.utility.Utility;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Entity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.SimpleDateFormat;

//for testing purpose
public class TestMain {

    public static void main(String[] args) throws Exception {
//        InetAddress inetAddress = InetAddress.getByName("25.44.56.7");
//        ValidatorRestClient validatorRestClient = new ValidatorRestClient(inetAddress, general.security.SecurityHelper.getX509FromDER(new File("auth2.cer")));
//
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//
//        validatorRestClient.login("testroot","1234".toCharArray());
//
//
////        KeyPair keyPair=general.security.SecurityHelper.generateECKeyPair();
////        SecretKey secretKey = general.security.SecurityHelper.generateAESKey();
////
////        testRegister(validatorRestClient,keyPair,secretKey, "Test patient info");
//
//        PatientShortInfoPojo[] patientShortInfoPojos =testGetPatientShortInfo(validatorRestClient);
//        System.out.println(patientShortInfoPojos.length);
//        System.out.println(Utility.bytesToStringHex(patientShortInfoPojos[0].getLocationPojo().getTargetIdentifier()));
//        System.out.println(Utility.bytesToStringHex(patientShortInfoPojos[0].getLocationPojo().getBlockHash()));
//        System.out.println(patientShortInfoPojos[0].getTimestamp());
//
//        PatientInfoContentPojo[] patientInfoContentPojos =testGetPatientInfoContent(validatorRestClient,new LocationPojo[]{patientShortInfoPojos[0].getLocationPojo()});
//
//        System.out.println(patientInfoContentPojos.length);
//
//
//        byte[] zeroIV = new byte[16];
//        FileInputStream fis = new FileInputStream(new File("testPatientInfoEncryptionKey"));
//        byte[] encoded =fis.readAllBytes();
//        fis.close();
//        SecretKey secretKey = new SecretKeySpec(encoded, "AES");
//
//
//        byte[] result =general.security.SecurityHelper.decryptAES(patientInfoContentPojos[0].getEncryptedInfo(),secretKey,zeroIV);
//
//        System.out.println(new String(result));

//        InetAddress inetAddress = InetAddress.getByName("25.44.56.7");
//        ValidatorRestClient validatorRestClient = new ValidatorRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("auth2.cer")));
//
//        validatorRestClient.login("root", "1234".toCharArray());
//        System.out.println(testGetPatientShortInfo(validatorRestClient).length);
//        System.out.println(validatorRestClient.getOverallAuthorityShortInfoList().length);
        //testAuthorize(validatorRestClient);
//
//        testRegister2(validatorRestClient,"patient1/testPatientPublicKey.pem","patient1/testPatientPrivateKey.pem","patient1/testPatientInfoEncryptionKey","Test patient1 info");
//        testRegister2(validatorRestClient,"patient2/testPatientPublicKey.pem","patient2/testPatientPrivateKey.pem","patient2/testPatientInfoEncryptionKey","Test patient2 info");


        //  testRegister(validatorRestClient,SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE),SecurityHelper.generateAESKey(),"Test patient2 info");

        PatientInfoPojo patientInfoPojo = new PatientInfoPojo();
        patientInfoPojo.setTimestamp(System.currentTimeMillis());
        patientInfoPojo.setSignature(new byte[5]);
        patientInfoPojo.setEncryptedInfo(new byte[5]);
        patientInfoPojo.getEcPublicKey();
        patientInfoPojo.setKeyDEREncoded(true);
        patientInfoPojo.setSignatureDEREncoded(true);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(patientInfoPojo));

        PublicKey publicKey =SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(publicKey.getEncoded()));
        ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) subjectPublicKeyInfo.getAlgorithm().getParameters();
        System.out.println(oid.equals(SECObjectIdentifiers.secp256k1));

    }


    private static PatientInfoContentPojo[] testGetPatientInfoContent(ValidatorRestClient validatorRestClient, LocationPojo[] locationPojo) throws Exception {

        return validatorRestClient.getPatientContentList(locationPojo);
    }

    private static PatientShortInfoPojo[] testGetPatientShortInfo(ValidatorRestClient validatorRestClient) throws Exception {

        ECPublicKey publicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");

        System.out.println(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey).length);
        return validatorRestClient.getPatientShortInfoList(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey));
    }

    private static void testAuthorize(ValidatorRestClient validatorRestClient) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        X509Certificate certificate = validatorRestClient.authorizeMedicalOrg("med0", (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("spb0.pem", "EC"), format.parse("2037-7-7"));
        SecurityHelper.writeX509ToDER(certificate, new File("med0.cer"));
    }

    private static void testRegister2(ValidatorRestClient validatorRestClient, String publicKey, String privateKey, String encryptionKey, String content) throws Exception {
        //just for saving key
        ECPublicKey ecPublicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM(publicKey, "EC");
        ECPrivateKey ecPrivateKey = (ECPrivateKey) SecurityHelper.getPrivateFromPEM(privateKey, "EC");
        SecretKey secretKey = SecurityHelper.getAESKeyFromFile(encryptionKey);

        byte[] zeroIV = new byte[16];//use 0 iv for encryption as encryption key will change every time
        byte[] patientInfo = SecurityHelper.encryptAES(content.getBytes(), secretKey, zeroIV);
        long timestamp = System.currentTimeMillis()-2000;
        byte[] timestampBytes = Utility.longToBytes(timestamp);

        // signature would be done on hash(timestamp + encrypted patient info)
        byte[] signatureCoverage = Utility.mergeByteArrays(timestampBytes, patientInfo);
        byte[] signature = SecurityHelper.createECDSASignatureWithContent(ecPrivateKey, signatureCoverage, Configuration.BLOCKCHAIN_HASH_ALGORITHM);


        validatorRestClient.registerPatientInfo(timestamp, ecPublicKey, patientInfo, signature,false);


    }

    private static void testRegister(ValidatorRestClient validatorRestClient, KeyPair testPatientKeyPair, SecretKey testPatientInfoEncryptionKey, String content) throws Exception {
        //just for saving key
        SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");
        SecurityHelper.writePrivateKeyToPEM(testPatientKeyPair.getPrivate(), "testPatientPrivateKey.pem", "EC");
        FileOutputStream fos = new FileOutputStream(new File("testPatientInfoEncryptionKey"));
        fos.write(testPatientInfoEncryptionKey.getEncoded());
        fos.close();


//        FileInputStream fis = new FileInputStream(new File("testPatientInfoEncryptionKey"));
//        byte[] encoded = fis.readAllBytes();

        byte[] encoded = Files.readAllBytes(new File("testPatientInfoEncryptionKey").toPath());
//        fis.close();
        SecretKey compareKey = new SecretKeySpec(encoded, "AES");

        if (compareKey.equals(testPatientInfoEncryptionKey)) {
            System.out.println("correctly saved");

            byte[] zeroIV = new byte[16];//use 0 iv for encryption as encryption key will change every time
            byte[] patientInfo = SecurityHelper.encryptAES(content.getBytes(), testPatientInfoEncryptionKey, zeroIV);
            long timestamp = System.currentTimeMillis();
            byte[] timestampBytes = Utility.longToBytes(timestamp);

            // signature would be done on hash(timestamp + encrypted patient info)
            byte[] signatureCoverage = Utility.mergeByteArrays(timestampBytes, patientInfo);
            byte[] signature = SecurityHelper.createECDSASignatureWithContent((ECPrivateKey) testPatientKeyPair.getPrivate(), signatureCoverage, Configuration.BLOCKCHAIN_HASH_ALGORITHM);


            validatorRestClient.registerPatientInfo(timestamp, (ECPublicKey) testPatientKeyPair.getPublic(), patientInfo, signature,false);

        } else {
            System.out.println("not correctly saved");
        }
    }
}
