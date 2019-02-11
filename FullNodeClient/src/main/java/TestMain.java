import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import pojo.LocationPojo;
import pojo.PatientShortInfoPojo;

import java.io.File;
import java.net.InetAddress;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

//for testing purpose
public class TestMain {

    public static void main(String[] args) throws Exception {
        InetAddress inetAddress = InetAddress.getByName("25.43.79.11");
        FullNodeRestClient fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("med0.cer")));


        ECPublicKey patientPublicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM("testPatientPublicKey.pem","EC");

        fullNodeRestClient.login("user","1234".toCharArray());


        PatientShortInfoPojo[] patientShortInfoPojos=fullNodeRestClient.getPatientShortInfoList(patientPublicKey);
        System.out.println(patientShortInfoPojos.length);

        ArrayList<LocationPojo> locationPojos = new ArrayList<>();
        for(PatientShortInfoPojo patientShortInfoPojo: patientShortInfoPojos)
        {
            locationPojos.add(patientShortInfoPojo.getLocation());
        }

        System.out.println(GeneralHelper.bytesToStringHex(fullNodeRestClient.getPatientInfoContentsList(locationPojos)[0].getEncryptedInfo()));

        System.out.println(GeneralHelper.bytesToStringHex(fullNodeRestClient.getMedicalOrgIdentifier()));


    }
}
