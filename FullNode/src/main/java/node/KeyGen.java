package node;


import config.Configuration;
import general.security.SecurityHelper;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Scanner;

public class KeyGen {

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {


        Scanner sc = new Scanner(System.in);

        System.out.println("0. Signing");
        System.out.println("1. TLS");
        System.out.print("Choose the usage of the key: ");

        int usage = sc.nextInt();

        while(usage!=0&& usage!=1)
        {
            System.out.print("Invalid input, type again: ");
            usage = sc.nextInt();
        }

        System.out.print("Type your public key file name: ");
        String pubString =sc.next();
        while(new File(pubString).exists())
        {
            System.out.print("File already exists, type again: ");
            pubString =sc.next();
        }


        System.out.print("Type your private key file name: ");
        String privString =sc.next();
        while(new File(privString).exists())
        {
            System.out.print("File already exists, type again: ");
            privString =sc.next();
        }

        if(usage==0) {
            KeyPair keyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            SecurityHelper.writePublicKeyToPEM(keyPair.getPublic(), pubString);
            SecurityHelper.writePrivateKeyToPEM(keyPair.getPrivate(), privString,"EC");
        }
        else
        {
            KeyPair keyPair = SecurityHelper.generateRSAKeyPair();
            SecurityHelper.writePublicKeyToPEM(keyPair.getPublic(), pubString);
            SecurityHelper.writePrivateKeyToPEM(keyPair.getPrivate(), privString,"RSA");
        }

    }
}
