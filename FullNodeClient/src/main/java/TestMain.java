import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.File;
import java.net.InetAddress;

//for testing purpose
public class TestMain {

    public static void main(String[] args) throws Exception {
        InetAddress inetAddress = InetAddress.getByName("25.43.79.11");
        FullNodeRestClient fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("med0.cer")));


        fullNodeRestClient.login("user","1234".toCharArray());

        System.out.println(GeneralHelper.bytesToStringHex(fullNodeRestClient.getMedicalOrgIdentifier()));


    }
}
