import blockchain.BlockChainSecurityHelper;
import com.fasterxml.jackson.databind.ser.Serializers;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import pojo.LocationPojo;
import pojo.PatientInfoContentPojo;
import pojo.PatientShortInfoPojo;
import pojo.RecordShortInfoPojo;
import xyz.medirec.medirec.pojo.KeyTime;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import javax.swing.*;
import java.awt.event.*;




//for testing purpose
public class TestMain {
//ec2-user
    public static void main(String[] args) throws Exception {
        // Read AES key from File ***********
        FileReader fr = null;
        try
        { fr = new FileReader("testPatientInfoEncryptionKey");}
        catch (Exception e){}
        int j;
        String key_in_string= "";
        try
        {
            while ((j=fr.read()) != -1)
            {
                System.out.print((char) j);
                key_in_string = key_in_string + (char) j;
            }
        }
        catch (Exception e){System.out.println("OMG");}
        byte[] contentb = Files.readAllBytes(Paths.get("testPatientInfoEncryptionKey") );
        String content = new String (contentb);
        System.out.println(key_in_string.getBytes());
        SecretKey AKEY = null;
        try {
            AKEY =  new SecretKeySpec(key_in_string.getBytes(), "AES");
        }catch (Exception e){System.out.println("OMG");}
        System.out.println("AES : " + AKEY);
        System.out.println("AES : " + contentb);
        System.out.println("AES : " + content);
        // Read AES key from File *********** END ***************************


        // Read ECPublicKey key from File *******************************************
        ECPublicKey publicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem", "EC");
        ECPublicKey publicKey2 = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey2.pem", "EC");
        // Read ECPublicKey key from File *********** END ***************************


        InetAddress inetAddress = InetAddress.getByName("25.43.79.11");
        FullNodeRestClient fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("med0.cer")));

        fullNodeRestClient.login("user","1234".toCharArray());

        System.out.println(GeneralHelper.bytesToStringHex(fullNodeRestClient.getMedicalOrgIdentifier()));

        long timestamp = timestamp();

        PatientShortInfoPojo[] patient1  =  fullNodeRestClient.getPatientShortInfoList(publicKey);
        PatientShortInfoPojo[] patient2  =  fullNodeRestClient.getPatientShortInfoList(publicKey2);
        RecordShortInfoPojo[] record1 = fullNodeRestClient.getRecordShortInfoList(publicKey);
        RecordShortInfoPojo[] record2 = fullNodeRestClient.getRecordShortInfoList(publicKey2);

        ArrayList<LocationPojo> location_pojo = new ArrayList<LocationPojo>();
        for(int i = 0; i < patient1.length; i++)
            location_pojo.add(patient1[i].getLocation());

        System.out.println("Patient info Array List size : " + location_pojo.size());
        PatientInfoContentPojo[] patientinfo = fullNodeRestClient.getPatientInfoContentsList(location_pojo);

        System.out.println("Medical Org Identifier : " + fullNodeRestClient.getMedicalOrgIdentifier());
        System.out.println("**************************************");
        System.out.println("Patient : ");
        System.out.println("Patient length : " + patient1.length);
        System.out.println("Patient Timestamp : " + patient1[0].getTimestamp());
        System.out.println("*** Location *** ");
        System.out.println("Patient Block Hash : " + patient1[0].getLocation().getBlockHash().length);
        System.out.println("Patient Target Identifier : " + patient1[0].getLocation().getTargetIdentifier().length);
        System.out.println("Patient Information length : " + patientinfo.length);

        byte[] key = Files.readAllBytes(Paths.get("testPatientInfoEncryptionKey") );
        SecretKeySpec AES_KEY = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.DECRYPT_MODE, AES_KEY, iv);
        byte[] org = cipher.doFinal(patientinfo[0].getEncryptedInfo());
        String orgstr = new String(org);

        System.out.println("Patient Information contents : " + patientinfo[0].getEncryptedInfo());
        System.out.println("Patient Information contents : " + org);
        System.out.println("Patient Information contents : " + orgstr);

        System.out.println("**************************************");
        System.out.println("Record : ");
        System.out.println("Record length : " + record1.length);
        System.out.println("**************************************");
        System.out.println("Patient : ");
        System.out.println("Patient length : " + patient2.length);
        System.out.println("Patient Timestamp : " + patient2[0].getTimestamp());
        System.out.println("*** Location *** ");
        System.out.println("Patient Block Hash : " + patient2[0].getLocation().getBlockHash().length);
        System.out.println("Patient Target Identifier : " + patient2[0].getLocation().getTargetIdentifier().length);
        //System.out.println("Patient Information contents : " + patientinfo.length);
        System.out.println("**************************************");

        String patientSign_instring = "kd84t2FP2yCV9ZJv4AGIL952KVD+BXBXxCQdbJDzLzj7f12GVVv1bI3FoHggdhx0lbxX4bOL1AkWwo3iafjMlQ==";
        byte[] decoded_sign = Base64.getDecoder().decode(patientSign_instring);
        //byte[] transactionid = fullNodeRestClient.addTransaction(timestamp, BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(patientPublicKey));
        /*
        System.out.println(record1[0].getMedicalOrgName());
        System.out.println(record1[0].getTimestamp());
        System.out.println(record1[0].getLocationPojo().getBlockHash());
        System.out.println(record1[0].getLocationPojo().getTargetIdentifier());
        */
        //System.out.println("**************************************");

    }


    public static long timestamp() {
        long unixTime = System.currentTimeMillis() / 1000L;
        return unixTime;
    }


}
