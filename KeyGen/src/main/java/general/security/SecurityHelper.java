package general.security;


import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import java.io.*;
import java.security.*;
import java.security.spec.*;



public class SecurityHelper {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    //----------------------------------------------for file handling start------------------------------------------------
    public static void writePublicKeyToPEM(PublicKey publicKey, String fileName) throws IOException {
        Writer writer = new FileWriter(fileName);
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.close();
    }

    public static void writePrivateKeyToPEM(PrivateKey privateKey, String fileName, String algo) throws IOException {
        Writer writer = new FileWriter(fileName);
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject(algo + " PRIVATE KEY", privateKey.getEncoded()));
        pemWriter.close();
    }


    //----------------------------------------------for file handling end------------------------------------------------

    //----------------------------------------------for key start------------------------------------------------

    public static KeyPair generateECKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec(curve), new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }

    //reference: https://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }

    //----------------------------------------------for key end------------------------------------------------

}
