package rest.server.manager;

import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import general.utility.GeneralHelper;
import general.security.SecurityHelper;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

public class CertificateManager {


    public static void remove(X509Certificate cert){
        byte[] identifier = BlockChainSecurityHelper.calculateIdentifierFromECPublicKey((ECPublicKey) cert.getPublicKey());
        String identifierString = GeneralHelper.bytesToStringHex(identifier);

        File certFile = new File(Configuration.ISSUED_CERT_FOLDER,identifierString.charAt(0)+"/"+identifierString.charAt(1)
                +"/"+identifierString.charAt(2)+"/"+identifierString);

        if(!certFile.exists())
            return;
        else
            certFile.delete();

    }

    public static boolean exist(byte[] identifier)
    {

        String identifierString = GeneralHelper.bytesToStringHex(identifier);

        return new File(Configuration.ISSUED_CERT_FOLDER,identifierString.charAt(0)+"/"+identifierString.charAt(1)
                +"/"+identifierString.charAt(2)+"/"+identifierString).exists();
    }

    public static X509Certificate get(byte[] identifier){

        String identifierString = GeneralHelper.bytesToStringHex(identifier);
        File certFile = new File(Configuration.ISSUED_CERT_FOLDER,identifierString.charAt(0)+"/"+identifierString.charAt(1)
            +"/"+identifierString.charAt(2)+"/"+identifierString);

        if(!certFile.exists())
            return null;

        X509Certificate result=null;
        try{
            result=SecurityHelper.getX509FromDER(certFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();// certificate file corruption
        }

        return result;
    }

    public static void store(X509Certificate cert) throws IOException, CertificateEncodingException {
        byte[] identifier = BlockChainSecurityHelper.calculateIdentifierFromECPublicKey((ECPublicKey) cert.getPublicKey());
        String identifierString = GeneralHelper.bytesToStringHex(identifier);

        File processingCertFolder = new File(Configuration.ISSUED_CERT_FOLDER,identifierString.charAt(0)+"/"+identifierString.charAt(1)
                +"/"+identifierString.charAt(2)+"/");

        if(!processingCertFolder.exists())
            processingCertFolder.mkdirs();

        SecurityHelper.writeX509ToDER(cert,new File(processingCertFolder,identifierString));

    }

}
