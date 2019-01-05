package general.security;


import config.Configuration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class SecurityHelper {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static SecureRandom random = new SecureRandom();

    //reference https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
    public static byte[] hash(byte[] data, String hashAlgo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(hashAlgo);

        return digest.digest(data);
    }

    // reference: https://www.baeldung.com/java-password-hashing
    public static byte[] passwordHash(char[] password, byte[] salt, String algo) throws NoSuchAlgorithmException {
        KeySpec spec = new PBEKeySpec(password, salt, 65536, Configuration.PASSWORD_HASH_LENGTH*8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algo);

        byte[] result= null;
        try {
            result= factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace(); // not expected
        }
        return result;
    }


    public static byte[] generateSalt(int saltSize) {
        byte[] salt = new byte[saltSize];
        random.nextBytes(salt);
        return salt;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println(passwordHash("c".toCharArray(),generateSalt(Configuration.PASSWORD_SALT_LENGTH),Configuration.PASSWORD_HASH_ALGORITHM).length);
    }
}
