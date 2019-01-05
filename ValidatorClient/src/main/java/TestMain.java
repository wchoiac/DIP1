import config.Configuration;
import general.security.SecurityHelper;
import pojo.LocationPojo;
import pojo.PatientInfoContentPojo;
import pojo.PatientShortInfoPojo;
import general.utility.Utility;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.SimpleDateFormat;

//for testing purpose
public class TestMain {

    public static void main(String[] args) throws Exception {
//        InetAddress inetAddress = InetAddress.getByName("25.44.56.7");
//        ValidatorRestClient validatorRestClient = new ValidatorRestClient(inetAddress, general.security.SecurityHelper.getX509FromDER(new File("dip1_signing.cer")));
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

        InetAddress inetAddress = InetAddress.getByName("25.44.56.7");
        ValidatorRestClient validatorRestClient = new ValidatorRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("dip1_signing.cer")));

        validatorRestClient.login("root", "1234".toCharArray());

        //testAuthorize(validatorRestClient);

        testRegister2(validatorRestClient,"patient1/testPatientPublicKey.pem","patient1/testPatientPrivateKey.pem","patient1/testPatientInfoEncryptionKey","Test patient1 info");
        testRegister2(validatorRestClient,"patient2/testPatientPublicKey.pem","patient2/testPatientPrivateKey.pem","patient2/testPatientInfoEncryptionKey","Test patient2 info");


        //  testRegister(validatorRestClient,SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE),SecurityHelper.generateAESKey(),"Test patient2 info");

    }


    private static PatientInfoContentPojo[] testGetPatientInfoContent(ValidatorRestClient validatorRestClient, LocationPojo[] locationPojo) throws Exception {

        return validatorRestClient.getPatientContentList(locationPojo);
    }

    private static PatientShortInfoPojo[] testGetPatientShortInfo(ValidatorRestClient validatorRestClient) throws Exception {

        ECPublicKey publicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");

        return validatorRestClient.getPatientShortInfoList(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey));
    }

    private static void testAuthorize(ValidatorRestClient validatorRestClient) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        X509Certificate certificate = validatorRestClient.authorize("med0", (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("spb0.pem", "EC"), format.parse("2037-7-7"));
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


        System.out.println(validatorRestClient.register(timestamp, ecPublicKey, patientInfo, signature));


    }

    private static void testRegister(ValidatorRestClient validatorRestClient, KeyPair testPatientKeyPair, SecretKey testPatientInfoEncryptionKey, String content) throws Exception {
        //just for saving key
        SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");
        SecurityHelper.writePrivateKeyToPEM(testPatientKeyPair.getPrivate(), "testPatientPrivateKey.pem", "EC");
        FileOutputStream fos = new FileOutputStream(new File("testPatientInfoEncryptionKey"));
        fos.write(testPatientInfoEncryptionKey.getEncoded());
        fos.close();


        FileInputStream fis = new FileInputStream(new File("testPatientInfoEncryptionKey"));
        byte[] encoded = fis.readAllBytes();
        fis.close();
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


            System.out.println(validatorRestClient.register(timestamp, (ECPublicKey) testPatientKeyPair.getPublic(), patientInfo, signature));

        } else {
            System.out.println("not correctly saved");
        }
    }
}
