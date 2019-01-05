package blockchain;

import config.Configuration;
import general.security.SecurityHelper;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

public class BlockChainSecurityHelper {
    public static byte[] calculateIdentifierFromECPublicKey(ECPublicKey publicKey)
    {
        byte[] hash = new byte[0];
        try {
            hash = SecurityHelper.hash(SecurityHelper.getCompressedRawECPublicKey(
                    publicKey,Configuration.ELIPTIC_CURVE), Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return Arrays.copyOfRange(hash, hash.length-Configuration.IDENTIFIER_LENGTH, hash.length);
    }
}
